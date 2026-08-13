package battle

import (
	"context"
	"errors"
	"fmt"
	"sort"
	"strings"
	"sync"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

var (
	// ErrInvalidRuntimeRegistry 表示 Registry 容量没有配置为正数，无法保证活跃对局的资源上限。
	ErrInvalidRuntimeRegistry = errors.New("对战 Runtime 注册表无效")
	// ErrRuntimeCapacityExceeded 表示所有 Runtime 槽位均被活跃 Battle 占用；Registry 不会淘汰已有对局。
	ErrRuntimeCapacityExceeded = errors.New("对战 Runtime 容量不足")
	// ErrRuntimeAlreadyRegistered 表示同一 Battle Identifier 已有唯一 Runtime，禁止重复创建并发执行者。
	ErrRuntimeAlreadyRegistered = errors.New("对战 Runtime 已经注册")
	// ErrRuntimeNotFound 表示请求的 Battle 当前没有可用 Runtime。
	ErrRuntimeNotFound = errors.New("对战 Runtime 不存在")
	// ErrRuntimePanicked 表示 Registry 已隔离单个 Runtime 的意外 panic，并已请求把该 Battle 中断。
	ErrRuntimePanicked = errors.New("对战 Runtime 发生 panic")
)

// RuntimePanic 记录已被 Registry 隔离、需要持久化为 interrupted 的单个 Runtime 故障。
type RuntimePanic struct {
	// BattleID 是发生 panic 的唯一 running Battle。
	BattleID snowflake.ID
	// Recovered 是 recover 捕获的原始值，仅供受控日志和诊断使用，不能返回给玩家客户端。
	Recovered any
	// Lease 是发生 panic 的 Runtime 创建时绑定的 PostgreSQL 承载权。
	Lease RuntimeLease
}

// RuntimePanicHandler 负责将被隔离的 Battle 标记为 runtime_panic 并释放账号占用。
//
// handler 应使用新的、有截止时间的数据库事务；它不得 panic，并且不能沿用可能已经取消的玩家 RPC 请求上下文。
type RuntimePanicHandler func(ctx context.Context, failure RuntimePanic)

// RuntimeRegistry 为单进程部署维护 Battle Identifier 到唯一串行 Runtime 的受限映射。
//
// Registry 只管理内存生命周期，不是跨进程分布式锁。生产入口在注册前已持有 server 租约，
// 服务重启或租约丢失会由应用层把未完成的 Battle 统一中断，而不是试图从内存恢复。
type RuntimeRegistry struct {
	// mutex 保护 actor 映射与容量判断，使并发启动不会超出固定上限。
	mutex sync.RWMutex
	// capacity 是允许同时执行的最大活跃 Battle 数量。
	capacity int
	// actors 按 Battle Identifier 保存唯一 Actor。
	actors map[snowflake.ID]*Runtime
	// reservations 先占用将要启动的 Actor 容量，但不会暴露给 Submit；它避免数据库启动成功后才发现
	// 本进程无可用 Actor 槽位的半启动 Battle。
	reservations map[snowflake.ID]struct{}
	// panicHandler 负责把隔离故障映射为持久化 Interrupted 生命周期转换。
	panicHandler     RuntimePanicHandler
	leaseCoordinator RuntimeLeaseCoordinator
	holderID         string
	leases           map[snowflake.ID]RuntimeLease
}

// newRuntimeRegistry 创建固定容量的 Runtime Registry。
//
// 容量必须为正；错误配置返回 nil，调用方必须在启动阶段失败，不能以无限容量悄悄继续运行。
func newRuntimeRegistry(capacity int, panicHandler RuntimePanicHandler) *RuntimeRegistry {
	if capacity <= 0 {
		return nil
	}
	return &RuntimeRegistry{
		capacity: capacity, actors: make(map[snowflake.ID]*Runtime, capacity),
		reservations: make(map[snowflake.ID]struct{}, capacity), panicHandler: panicHandler,
		leases: make(map[snowflake.ID]RuntimeLease, capacity),
	}
}

// NewRuntimeRegistryWithRuntimeLeases 创建由 PostgreSQL Lease 和 fencing token 保护的生产 Registry。
func NewRuntimeRegistryWithRuntimeLeases(capacity int, panicHandler RuntimePanicHandler, coordinator RuntimeLeaseCoordinator, holderID string) *RuntimeRegistry {
	registry := newRuntimeRegistry(capacity, panicHandler)
	if registry == nil || coordinator == nil || strings.TrimSpace(holderID) == "" {
		return nil
	}
	registry.leaseCoordinator = coordinator
	registry.holderID = strings.TrimSpace(holderID)
	return registry
}

// AcquireRuntimeLease 在公开 Runtime 前取得当前 Battle 的 fencing token。
func (registry *RuntimeRegistry) AcquireRuntimeLease(ctx context.Context, battleID snowflake.ID) error {
	if registry == nil || registry.leaseCoordinator == nil || registry.holderID == "" {
		return ErrInvalidRuntimeRegistry
	}
	lease, err := registry.leaseCoordinator.AcquireRuntimeLease(ctx, battleID, registry.holderID)
	if err != nil {
		return err
	}
	registry.mutex.Lock()
	registry.leases[battleID] = lease
	registry.mutex.Unlock()
	return nil
}

// RuntimeLease 返回指定 Battle 当前由本 Registry 持有的 Lease 快照。
//
// 返回值只用于绑定本次 Runtime 的持久化写入边界；续期不会改变 holder 或 fencing token。
func (registry *RuntimeRegistry) RuntimeLease(battleID snowflake.ID) (RuntimeLease, bool) {
	if registry == nil || battleID == snowflake.ID(0) {
		return RuntimeLease{}, false
	}
	registry.mutex.RLock()
	defer registry.mutex.RUnlock()
	lease, ok := registry.leases[battleID]
	return lease, ok
}

// ReleaseAcquiredRuntimeLease 释放尚未激活 Runtime 的 Battle Lease。
func (registry *RuntimeRegistry) ReleaseAcquiredRuntimeLease(ctx context.Context, battleID snowflake.ID) {
	if registry == nil || registry.leaseCoordinator == nil || battleID == snowflake.ID(0) {
		return
	}
	registry.mutex.Lock()
	lease, ok := registry.leases[battleID]
	delete(registry.leases, battleID)
	registry.mutex.Unlock()
	if ok {
		_ = registry.leaseCoordinator.ReleaseRuntimeLease(ctx, lease)
	}
}

// RenewRuntimeLeases 续期全部活跃 Runtime；任何丢失 token 的 Runtime 会立即从 Registry 移除。
func (registry *RuntimeRegistry) RenewRuntimeLeases(ctx context.Context) error {
	if registry == nil || registry.leaseCoordinator == nil {
		return ErrInvalidRuntimeRegistry
	}
	for _, battleID := range registry.IDs() {
		registry.mutex.RLock()
		lease, ok := registry.leases[battleID]
		registry.mutex.RUnlock()
		if !ok {
			registry.Remove(battleID)
			continue
		}
		renewed, err := registry.leaseCoordinator.RenewRuntimeLease(ctx, lease)
		if err != nil {
			registry.Remove(battleID)
			return err
		}
		registry.mutex.Lock()
		registry.leases[battleID] = renewed
		registry.mutex.Unlock()
	}
	return nil
}

// Reserve 预留一个尚未公开给 Submit 的 Battle Runtime 容量槽位。
//
// 调用方必须在数据库 Start 成功后调用 Activate，或在任一路径失败时调用 ReleaseReservation。预留
// 不会出现在 Get、Count 或 Submit 中，因此客户端不能在权威 Battle 尚未完成 Runtime 启动时提前提交回合。
func (registry *RuntimeRegistry) Reserve(battleID snowflake.ID) error {
	if registry == nil || battleID == snowflake.ID(0) {
		return ErrInvalidRuntimeRegistry
	}
	registry.mutex.Lock()
	defer registry.mutex.Unlock()
	if _, exists := registry.actors[battleID]; exists {
		return ErrRuntimeAlreadyRegistered
	}
	if _, exists := registry.reservations[battleID]; exists {
		return ErrRuntimeAlreadyRegistered
	}
	if len(registry.actors)+len(registry.reservations) >= registry.capacity {
		return ErrRuntimeCapacityExceeded
	}
	registry.reservations[battleID] = struct{}{}
	return nil
}

// Activate 将已经预留容量且数据库已进入 active 的 Actor 原子公开给回合提交入口。
func (registry *RuntimeRegistry) Activate(actor *Runtime) error {
	if registry == nil || actor == nil {
		return ErrInvalidRuntimeRegistry
	}
	battleID := actor.Battle().ID
	if battleID == snowflake.ID(0) {
		return ErrInvalidRuntime
	}
	registry.mutex.Lock()
	defer registry.mutex.Unlock()
	if _, exists := registry.actors[battleID]; exists {
		return ErrRuntimeAlreadyRegistered
	}
	if _, reserved := registry.reservations[battleID]; !reserved {
		return ErrInvalidRuntimeRegistry
	}
	delete(registry.reservations, battleID)
	registry.actors[battleID] = actor
	return nil
}

// ReleaseReservation 释放尚未激活的启动容量；重复调用和不存在的 Battle 都保持无副作用。
func (registry *RuntimeRegistry) ReleaseReservation(battleID snowflake.ID) {
	if registry == nil || battleID == snowflake.ID(0) {
		return
	}
	registry.mutex.Lock()
	defer registry.mutex.Unlock()
	delete(registry.reservations, battleID)
}

// Register 原子地注册一个尚未存在且未超出容量的 running Battle Runtime。
func (registry *RuntimeRegistry) Register(actor *Runtime) error {
	if registry == nil || actor == nil {
		return ErrInvalidRuntimeRegistry
	}
	battleID := actor.Battle().ID
	if battleID == snowflake.ID(0) {
		return ErrInvalidRuntime
	}
	registry.mutex.Lock()
	defer registry.mutex.Unlock()
	if _, exists := registry.actors[battleID]; exists {
		return ErrRuntimeAlreadyRegistered
	}
	if len(registry.actors)+len(registry.reservations) >= registry.capacity {
		return ErrRuntimeCapacityExceeded
	}
	registry.actors[battleID] = actor
	return nil
}

// Get 返回指定 Battle 的唯一 Actor；返回值仅供受控应用层使用，不得在外部长期缓存。
func (registry *RuntimeRegistry) Get(battleID snowflake.ID) (*Runtime, bool) {
	if registry == nil || battleID == snowflake.ID(0) {
		return nil, false
	}
	registry.mutex.RLock()
	defer registry.mutex.RUnlock()
	actor, found := registry.actors[battleID]
	return actor, found
}

// Remove 注销已终局或被中断 Battle 的 Actor，并返回是否实际移除了实例。
func (registry *RuntimeRegistry) Remove(battleID snowflake.ID) bool {
	if registry == nil || battleID == snowflake.ID(0) {
		return false
	}
	registry.mutex.Lock()
	if _, found := registry.actors[battleID]; !found {
		registry.mutex.Unlock()
		return false
	}
	delete(registry.actors, battleID)
	lease, hasLease := registry.leases[battleID]
	delete(registry.leases, battleID)
	registry.mutex.Unlock()
	if hasLease && registry.leaseCoordinator != nil {
		_ = registry.leaseCoordinator.ReleaseRuntimeLease(context.Background(), lease)
	}
	return true
}

// IDs 返回当前所有已激活 Actor 的 Battle Identifier 稳定快照。
//
// 返回值按 Identifier 字典序排序，调用方可以在不长期持有 Registry 锁的情况下进行受控状态同步。
func (registry *RuntimeRegistry) IDs() []snowflake.ID {
	if registry == nil {
		return nil
	}
	registry.mutex.RLock()
	defer registry.mutex.RUnlock()
	ids := make([]snowflake.ID, 0, len(registry.actors))
	for battleID := range registry.actors {
		ids = append(ids, battleID)
	}
	sort.Slice(ids, func(left, right int) bool {
		return ids[left].String() < ids[right].String()
	})
	return ids
}

// ExpireTurnDeadlines 以统一 UTC 观测时间结算当前进程内所有到期回合，并返回已移除的 Battle Identifier。
//
// Actor 在持有自身串行锁时调用持久化终局事务，因此提交命令与定时检查不会产生双重胜负。整场超时
// 由独立 Asynq Worker 处理，Actor 对到达整场截止时间的 Battle 保持无副作用，等待权威生命值裁定。
func (registry *RuntimeRegistry) ExpireTurnDeadlines(ctx context.Context, observedAt time.Time) ([]snowflake.ID, error) {
	if registry == nil || observedAt.IsZero() {
		return nil, ErrInvalidRuntimeRegistry
	}
	expired := make([]snowflake.ID, 0)
	for _, battleID := range registry.IDs() {
		actor, found := registry.Get(battleID)
		if !found {
			continue
		}
		completed, err := actor.ExpireTurn(ctx, observedAt)
		if err != nil {
			return expired, err
		}
		if completed && registry.Remove(battleID) {
			expired = append(expired, battleID)
		}
	}
	return expired, nil
}

// Count 返回当前受 registry 管理的活跃 Actor 数量。
func (registry *RuntimeRegistry) Count() int {
	if registry == nil {
		return 0
	}
	registry.mutex.RLock()
	defer registry.mutex.RUnlock()
	return len(registry.actors)
}

// Submit 在给定 Battle 的串行 Actor 内提交一份秘密回合选择，并隔离该 Actor 的意外 panic。
func (registry *RuntimeRegistry) Submit(
	ctx context.Context,
	battleID snowflake.ID,
	submission TurnSubmission,
) (result TurnSubmissionResult, err error) {
	if registry.leaseCoordinator != nil {
		registry.mutex.RLock()
		lease, ok := registry.leases[battleID]
		registry.mutex.RUnlock()
		if !ok {
			return TurnSubmissionResult{}, ErrRuntimeNotFound
		}
		renewed, renewErr := registry.leaseCoordinator.RenewRuntimeLease(ctx, lease)
		if renewErr != nil {
			registry.Remove(battleID)
			return TurnSubmissionResult{}, renewErr
		}
		registry.mutex.Lock()
		registry.leases[battleID] = renewed
		registry.mutex.Unlock()
	}
	actor, found := registry.Get(battleID)
	if !found {
		return TurnSubmissionResult{}, ErrRuntimeNotFound
	}
	defer func() {
		if recovered := recover(); recovered != nil {
			registry.mutex.RLock()
			lease := registry.leases[battleID]
			registry.mutex.RUnlock()
			if registry.panicHandler != nil {
				registry.panicHandler(ctx, RuntimePanic{BattleID: battleID, Recovered: recovered, Lease: lease})
			}
			registry.Remove(battleID)
			result = TurnSubmissionResult{}
			err = fmt.Errorf("%w: battle=%s", ErrRuntimePanicked, battleID)
		}
	}()
	result, err = actor.Submit(ctx, submission)
	if err != nil {
		return TurnSubmissionResult{}, err
	}
	// 引擎终局回合已由 TurnCommitter 在同一事务内完成 Battle、历史、Outbox、账号占用与活跃计数
	// 的转换。提交成功后立即移除本进程 Runtime，避免终局 Battle 继续占用有限执行容量。
	if result.Resolved && result.State.Result != nil {
		registry.Remove(battleID)
	}
	return result, nil
}
