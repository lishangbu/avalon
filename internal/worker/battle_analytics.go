package worker

import (
	"context"
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/backgroundtask"
	battlepersistence "github.com/lishangbu/avalon/internal/battle/persistence"
)

const (
	battleAnalyticsScanInterval = 10 * time.Second
	battleAnalyticsBatchSize    = 100
)

// BattleAnalyticsTaskKind 是 Battle 终局投影消费任务的稳定类型。
const BattleAnalyticsTaskKind = backgroundtask.BattleAnalytics

// BattleAnalyticsRunner 是 Asynq 分析任务调用的最小 Battle Outbox 消费边界。
type BattleAnalyticsRunner interface {
	// DrainTerminalOutbox 有界消费已提交的 Battle 终局 Outbox 并更新可重建只读投影。
	DrainTerminalOutbox(context.Context, time.Time, int) (battlepersistence.AnalyticsDrainResult, error)
}

// DrainBattleAnalyticsArgs 是无状态 Battle 分析扫描任务的持久化参数。
//
// 任务不保存 Outbox Identifier 或可变游标；每次执行由 Repository 使用 PostgreSQL 行锁领取当前待发布记录，因此
// Asynq 重试、进程重启和周期任务重叠不会重复累计分析事实。
type DrainBattleAnalyticsArgs struct{}

// BattleAnalyticsWorker 消费终局 Battle Outbox 并写入总览投影。
type BattleAnalyticsWorker struct {
	// runner 领取 Outbox、写投影并标记发布；Worker 自身不直接操作数据库。
	runner BattleAnalyticsRunner
	// now 提供每次事务写入 refreshedAt 和 publishedAt 时使用的统一 UTC 观察时间。
	now func() time.Time
}

// NewBattleAnalyticsWorker 使用显式消费边界和时钟创建 Asynq 分析 Worker。
func NewBattleAnalyticsWorker(runner BattleAnalyticsRunner, now func() time.Time) *BattleAnalyticsWorker {
	if now == nil {
		now = time.Now
	}
	return &BattleAnalyticsWorker{runner: runner, now: now}
}

// Work 在单次任务中最多消费一百条终局 Outbox；剩余记录由紧随的周期任务继续处理。
func (worker *BattleAnalyticsWorker) Run(ctx context.Context) error {
	if worker == nil || worker.runner == nil || worker.now == nil {
		return errors.New("Battle 分析 Worker 未配置")
	}
	_, err := worker.runner.DrainTerminalOutbox(ctx, worker.now().UTC(), battleAnalyticsBatchSize)
	return err
}
