package battle

import (
	"context"
	"fmt"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// LifecycleRepository 隔离 Battle 生命周期后台扫描所需的最小持久化能力。
//
// 周期任务只传递稳定 Identifier 和一个统一的权威观测时间；实际状态转换仍由 Repository 在行锁事务内完成，
// 因此重复投递、服务重启与 RPC 入口并发不会造成重复终局。
type LifecycleRepository interface {
	// ListExpiredChallengeIDs 返回仍处于 pending 且已经到期的 Challenge 稳定 Identifier。
	ListExpiredChallengeIDs(context.Context, time.Time) ([]snowflake.ID, error)
	// ExpireChallenge 将一个仍有效的到期 Challenge 推进为 expired。
	ExpireChallenge(context.Context, snowflake.ID, time.Time) (Challenge, error)
	// ListExpiredPreviewBattleIDs 返回仍处于 preview 且 Preview 截止时间已经到达的 Battle Identifier。
	ListExpiredPreviewBattleIDs(context.Context, time.Time) ([]snowflake.ID, error)
	// CompleteExpiredPreview 为一个仍处于 preview 的到期 Battle 补齐可重放的自动随机选择。
	CompleteExpiredPreview(context.Context, snowflake.ID, time.Time) (Battle, error)
	// ListExpiredRunningBattleIDs 返回仍处于 active 且整场截止时间已经到达的 Battle Identifier。
	ListExpiredRunningBattleIDs(context.Context, time.Time) ([]snowflake.ID, error)
	// CompleteBattleTimeout 以最后一次持久化权威状态裁定一个仍活跃的整场超时 Battle。
	CompleteBattleTimeout(context.Context, snowflake.ID, time.Time) (Battle, error)
	// ScheduleMissingRuntimeRecoveries 为没有有效 Lease 的 Running Battle 创建唯一待处理恢复尝试。
	ScheduleMissingRuntimeRecoveries(context.Context, time.Time, int) (int, error)
}

// LifecycleRunResult 是一次到期扫描实际完成的各类生命周期转换数量。
type LifecycleRunResult struct {
	// ExpiredChallenges 是从 pending 转为 expired 的 Challenge 数量。
	ExpiredChallenges int `json:"expiredChallenges"`
	// AutoCompletedPreviews 是由到期扫描补齐自动随机选择并推进为 starting 的 Preview Battle 数量。
	AutoCompletedPreviews int `json:"autoCompletedPreviews"`
	// TimedOutBattlees 是以 battle_timeout 正常结算的 Active Battle 数量。
	TimedOutBattlees int `json:"timedOutBattlees"`
	// ScheduledRecoveries 是本轮为失去 Runtime 的 Battle 新建的恢复尝试数量。
	ScheduledRecoveries int `json:"scheduledRecoveries"`
}

// LifecycleService 编排 Worker 的周期到期与 Runtime 恢复扫描。
//
// 它不拥有 Asynq、RPC、Runtime 或 goroutine；Asynq Worker 调用本服务，Repository 负责事务和幂等，
// 而 Server 在收到终局通知后负责从内存 Registry 解除 Runtime。这样生命周期规则可以通过同步公开接口
// 单独测试，并避免把时钟和数据库细节散落到任务处理器中。
type LifecycleService struct {
	// repository 保存待处理项并在行锁内完成权威状态转换。
	repository LifecycleRepository
	// now 提供同一次扫描共享的权威 UTC 观测时间。
	now func() time.Time
}

// NewLifecycleService 使用显式 Repository 与时钟创建 Battle 生命周期应用服务。
func NewLifecycleService(repository LifecycleRepository, now func() time.Time) *LifecycleService {
	if now == nil {
		now = time.Now
	}
	return &LifecycleService{repository: repository, now: now}
}

// ExpireDue 以一个共享 UTC 观测时间扫描并结算到期 Challenge、Preview 和 Active Battle。
func (service *LifecycleService) ExpireDue(ctx context.Context) (LifecycleRunResult, error) {
	if service == nil || service.repository == nil {
		return LifecycleRunResult{}, ErrInvalidBattle
	}
	observedAt := service.now().UTC()
	result := LifecycleRunResult{}
	challenges, err := service.repository.ListExpiredChallengeIDs(ctx, observedAt)
	if err != nil {
		return result, fmt.Errorf("查询到期 Challenge: %w", err)
	}
	for _, id := range challenges {
		if _, expireErr := service.repository.ExpireChallenge(ctx, id, observedAt); expireErr != nil {
			return result, fmt.Errorf("到期 Challenge %s: %w", id, expireErr)
		}
		result.ExpiredChallenges++
	}

	previews, err := service.repository.ListExpiredPreviewBattleIDs(ctx, observedAt)
	if err != nil {
		return result, fmt.Errorf("查询到期 Preview Battle: %w", err)
	}
	for _, id := range previews {
		if _, completeErr := service.repository.CompleteExpiredPreview(ctx, id, observedAt); completeErr != nil {
			return result, fmt.Errorf("补齐到期 Preview Battle %s: %w", id, completeErr)
		}
		result.AutoCompletedPreviews++
	}

	active, err := service.repository.ListExpiredRunningBattleIDs(ctx, observedAt)
	if err != nil {
		return result, fmt.Errorf("查询到期 Active Battle: %w", err)
	}
	for _, id := range active {
		if _, completeErr := service.repository.CompleteBattleTimeout(ctx, id, observedAt); completeErr != nil {
			return result, fmt.Errorf("整场超时 Battle %s: %w", id, completeErr)
		}
		result.TimedOutBattlees++
	}
	result.ScheduledRecoveries, err = service.repository.ScheduleMissingRuntimeRecoveries(ctx, observedAt, 100)
	if err != nil {
		return result, fmt.Errorf("安排 Battle Runtime 恢复: %w", err)
	}
	return result, nil
}
