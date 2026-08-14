package battle

import (
	"context"
	"fmt"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// InitialStateFactsReader 读取并解析启动 Battle 所需的完整战斗事实。
//
// 实现只能读取实时资料和本场冻结快照，不能从客户端上传载荷取得任何战斗数值。
type InitialStateFactsReader interface {
	// ReadInitialStateFacts 读取并编译创建 Battle Engine 所需的全部事实。
	ReadInitialStateFacts(context.Context, Battle) (InitialStateFacts, error)
}

// BattleStartRepository 是启动事务、Runtime 回合提交器与故障中断所需的最小持久化边界。
type BattleStartRepository interface {
	// Start 原子写入初始状态并为等待承接的 running Battle 写入启动时间。
	Start(context.Context, RuntimeLease, battleengine.InitialState, battleengine.RandomSourceSnapshot, time.Time) (Battle, error)
	// InterruptRuntime 使用当前 Lease 将无法继续运行的 Battle 推进为 interrupted。
	InterruptRuntime(context.Context, RuntimeLease, TerminalReason, time.Time) (Battle, error)
	// TurnCommitter 返回指定活跃 Battle 的单回合持久化提交器。
	TurnCommitter(RuntimeLease) TurnCommitter
	// TurnTimeoutCompleter 返回绑定同一 Lease 的回合超时终局写入器。
	TurnTimeoutCompleter(RuntimeLease) TurnTimeoutCompleter
}

// RandomSourceFactory 为每场新启动的 Battle 创建独立的确定性随机源。
//
// 种子只在启动时生成；后续每回合实际消耗的随机轨迹由 Turn Record 保存，重放不依赖重新取得该种子。
type RandomSourceFactory func() (battleengine.RandomSource, error)

// StartService 为 Preview 已齐备的 running Battle 原子建立引擎状态并公开唯一 Runtime。
//
// 数据库状态先提交、Runtime 后公开：Registry 预留容量以避免提交后无槽位，同时不会在持久化前让 RPC
// 入口取得 Runtime。启动失败会保留或明确中断 Battle，绝不以未持久化内存状态继续对战。
type StartService struct {
	// repository 执行 Battle 状态转换、初始状态写入和 Runtime 回合提交。
	repository BattleStartRepository
	// registry 为当前进程的 running Battle 提供唯一串行 Runtime。
	registry *RuntimeRegistry
	// facts 从实时资料和冻结 Team 读取编译输入。
	facts InitialStateFactsReader
	// random 为每个新 Battle 生成独立的确定性随机源。
	random RandomSourceFactory
	// realtime 在启动成功后通知连接从持久化账本重同步。
	realtime *RealtimeHub
	// now 提供启动和中断时使用的唯一权威 UTC 时间来源。
	now func() time.Time
}

// NewStartService 使用显式基础设施依赖创建 Battle 启动应用服务。
func NewStartService(
	repository BattleStartRepository, registry *RuntimeRegistry, facts InitialStateFactsReader,
	random RandomSourceFactory,
	realtime *RealtimeHub,
	now func() time.Time,
) *StartService {
	if now == nil {
		now = time.Now
	}
	return &StartService{
		repository: repository, registry: registry, facts: facts, random: random,
		realtime: realtime, now: now,
	}
}

// Start 编译当前实时资料、写入初始状态并激活可提交回合的唯一 Runtime。
func (service *StartService) Start(ctx context.Context, session Battle) (Battle, error) {
	if service == nil || service.repository == nil || service.registry == nil || service.facts == nil || service.random == nil ||
		service.registry.leaseCoordinator == nil || session.ID == snowflake.ID(0) || session.Status != StatusRunning || !session.StartedAt.IsZero() {
		return Battle{}, ErrInitialStateCompilation
	}
	startedAt := service.now().UTC()
	if err := service.registry.Reserve(session.ID); err != nil {
		// 同一 Server 的同步 RPC 启动和到期补选协调循环可能短暂并发。已预留或已公开 Runtime 表明另一
		// 调用正在正确处理本场 Battle，落后调用不得触碰该 Runtime 持有的 Lease。
		return Battle{}, err
	}
	reserved := true
	releaseReservation := func() {
		if reserved {
			service.registry.ReleaseReservation(session.ID)
			reserved = false
		}
	}
	defer releaseReservation()
	if err := service.registry.AcquireRuntimeLease(ctx, session.ID); err != nil {
		releaseReservation()
		return Battle{}, err
	}
	lease, hasLease := service.registry.RuntimeLease(session.ID)
	if !hasLease {
		service.registry.ReleaseAcquiredRuntimeLease(ctx, session.ID)
		return Battle{}, ErrInvalidRuntimeRegistry
	}
	leaseAcquired := true
	fail := func(cause error) (Battle, error) {
		releaseReservation()
		_, interruptErr := service.repository.InterruptRuntime(ctx, lease, TerminalReasonStartupFailed, service.now().UTC())
		if leaseAcquired {
			service.registry.ReleaseAcquiredRuntimeLease(ctx, session.ID)
			leaseAcquired = false
		}
		if interruptErr != nil {
			return Battle{}, fmt.Errorf("%w；中断启动失败: %v", cause, interruptErr)
		}
		return Battle{}, cause
	}
	facts, err := service.facts.ReadInitialStateFacts(ctx, session)
	if err != nil {
		return fail(fmt.Errorf("读取 Battle 实时资料: %w", err))
	}
	initial, err := CompileInitialState(session, facts)
	if err != nil {
		return fail(err)
	}
	state, err := battleengine.NewState(initial)
	if err != nil {
		return fail(fmt.Errorf("构造 Battle 战斗状态: %w", err))
	}
	prepared, err := session.Start(startedAt)
	if err != nil {
		return fail(err)
	}
	random, err := service.random()
	if err != nil {
		return fail(fmt.Errorf("创建 Battle 随机源: %w", err))
	}
	randomSnapshot, err := random.Snapshot()
	if err != nil {
		return fail(fmt.Errorf("快照 Battle 随机源: %w", err))
	}
	committer := service.repository.TurnCommitter(lease)
	timeoutCompleter := service.repository.TurnTimeoutCompleter(lease)
	runtime, err := newBattleRuntime(prepared, state, random, committer, timeoutCompleter, service.now)
	if err != nil {
		return fail(err)
	}
	started, err := service.repository.Start(ctx, lease, initial, randomSnapshot, startedAt)
	if err != nil {
		return fail(err)
	}
	if err := service.registry.Activate(runtime); err != nil {
		// 数据库已进入 running 但 Runtime 未能公开时，必须立刻以明确原因中断并释放账号占用。
		return fail(fmt.Errorf("激活 Battle Runtime: %w", err))
	}
	reserved = false
	leaseAcquired = false
	if service.realtime != nil {
		service.realtime.Publish(ctx, session.ID)
	}
	return started, nil
}

func newBattleRuntime(
	session Battle,
	state battleengine.State,
	random battleengine.RandomSource,
	committer TurnCommitter,
	timeoutCompleter TurnTimeoutCompleter,
	now func() time.Time,
) (*Runtime, error) {
	for _, participant := range session.Participants {
		if participant.IsBot {
			strategies, err := botStrategiesForFrozenBattle(session)
			if err != nil {
				return nil, err
			}
			return NewRuntime(session, state, random, committer, timeoutCompleter, now, strategies)
		}
	}
	return NewRuntime(session, state, random, committer, timeoutCompleter, now, nil)
}
