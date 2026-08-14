package worker_test

import (
	"context"
	"errors"
	"testing"

	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/worker"
)

// TestBattleLifecycleWorkerRunsSingleIdempotentExpirationSweep 验证后台任务只触发一次完整的生命周期扫描，
// 实际的重复投递安全性仍由 Repository 行锁和状态转换保证。
func TestBattleLifecycleWorkerRunsSingleIdempotentExpirationSweep(t *testing.T) {
	t.Parallel()

	runner := &battleLifecycleRunnerStub{result: battle.LifecycleRunResult{ExpiredChallenges: 1, TimedOutBattlees: 2}}
	job := worker.NewBattleLifecycleWorker(runner)

	if err := job.Run(context.Background()); err != nil {
		t.Fatalf("Run() error = %v", err)
	}
	if runner.calls != 1 {
		t.Fatalf("ExpireDue() 调用次数 = %d，期望 1", runner.calls)
	}
}

// TestBattleLifecycleWorkerReturnsApplicationFailureForAsynqRetry 验证生命周期扫描失败会原样返回 Asynq，
// 让任务框架按固定最大尝试次数和退避策略重试，而不是将错误伪装成成功。
func TestBattleLifecycleWorkerReturnsApplicationFailureForAsynqRetry(t *testing.T) {
	t.Parallel()

	want := errors.New("数据库暂时不可用")
	job := worker.NewBattleLifecycleWorker(&battleLifecycleRunnerStub{err: want})

	if err := job.Run(context.Background()); !errors.Is(err, want) {
		t.Fatalf("Run() error = %v，期望原始暂时错误", err)
	}
}

type battleLifecycleRunnerStub struct {
	result battle.LifecycleRunResult
	err    error
	calls  int
}

func (runner *battleLifecycleRunnerStub) ExpireDue(context.Context) (battle.LifecycleRunResult, error) {
	runner.calls++
	return runner.result, runner.err
}
