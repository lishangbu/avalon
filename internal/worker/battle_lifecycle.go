// Package worker 组装仅由 avalon-worker 进程执行的 Asynq 任务。
package worker

import (
	"context"
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/backgroundtask"
	battle "github.com/lishangbu/avalon/internal/battle"
)

const battleLifecycleScanInterval = 15 * time.Second

// BattleLifecycleTaskKind 是 Battle 生命周期扫描在 PostgreSQL 与 Asynq 中共用的稳定类型。
const BattleLifecycleTaskKind = backgroundtask.BattleLifecycle

// BattleLifecycleRunner 是 Asynq Battle 生命周期任务调用的最小应用服务边界。
type BattleLifecycleRunner interface {
	// ExpireDue 扫描并结算到期 Challenge、Preview 与 Active Battle。
	ExpireDue(context.Context) (battle.LifecycleRunResult, error)
}

// ExpireBattleLifecycleArgs 是一次无状态 Battle 生命周期扫描的持久化任务参数。
//
// 任务不保存 Battle Identifier 或可变时间；执行时由应用服务读取数据库当前到期项并以同一次权威时间完成
// 行锁转换。这使重复投递、错过计划和故障重试都保持幂等。
type ExpireBattleLifecycleArgs struct{}

// BattleLifecycleWorker 执行一次 Battle 到期扫描。
type BattleLifecycleWorker struct {
	// runner 编排真正的生命周期扫描和持久化状态转换。
	runner BattleLifecycleRunner
}

// NewBattleLifecycleWorker 使用明确的 Battle 生命周期应用服务创建 Asynq Worker。
func NewBattleLifecycleWorker(runner BattleLifecycleRunner) *BattleLifecycleWorker {
	return &BattleLifecycleWorker{runner: runner}
}

// Run 执行一轮到期扫描，并把错误交给 PostgreSQL 任务与 Asynq 的有限重试策略处理。
func (worker *BattleLifecycleWorker) Run(ctx context.Context) error {
	if worker == nil || worker.runner == nil {
		return errors.New("Battle 生命周期 Worker 未配置")
	}
	_, err := worker.runner.ExpireDue(ctx)
	return err
}
