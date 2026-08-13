package worker_test

import (
	"context"
	"errors"
	"testing"
	"time"

	battlestore "github.com/lishangbu/avalon/internal/battle/store"
	"github.com/lishangbu/avalon/internal/worker"
)

// TestBattleAnalyticsWorkerDrainsBoundedTerminalOutbox 验证 Asynq Worker 只传递固定批量和单一权威时间，
// 并将具体的行锁、投影与已发布标记交给持久层原子处理。
func TestBattleAnalyticsWorkerDrainsBoundedTerminalOutbox(t *testing.T) {
	t.Parallel()

	observedAt := time.Date(2026, time.July, 31, 13, 0, 0, 0, time.UTC)
	runner := &battleAnalyticsRunnerStub{result: battlestore.AnalyticsDrainResult{Published: 3}}
	job := worker.NewBattleAnalyticsWorker(runner, func() time.Time { return observedAt })

	if err := job.Run(context.Background()); err != nil {
		t.Fatalf("Run() error = %v", err)
	}
	if runner.calls != 1 || runner.maximum != 100 || !runner.observedAt.Equal(observedAt) {
		t.Fatalf("DrainTerminalOutbox() calls=%d maximum=%d observedAt=%s", runner.calls, runner.maximum, runner.observedAt)
	}
}

// TestBattleAnalyticsWorkerReturnsFailureForAsynqRetry 验证 Outbox 或投影事务失败时会由 Asynq 使用有限重试，
// 而不是把尚未 published 的记录伪装为成功。
func TestBattleAnalyticsWorkerReturnsFailureForAsynqRetry(t *testing.T) {
	t.Parallel()

	want := errors.New("投影数据库暂时不可用")
	job := worker.NewBattleAnalyticsWorker(&battleAnalyticsRunnerStub{err: want}, time.Now)
	if err := job.Run(context.Background()); !errors.Is(err, want) {
		t.Fatalf("Run() error = %v，期望原始暂时错误", err)
	}
}

// battleAnalyticsRunnerStub 捕获 Worker 交给 Battle 持久化边界的扫描参数。
type battleAnalyticsRunnerStub struct {
	// result 是成功消费时返回的最小结果。
	result battlestore.AnalyticsDrainResult
	// err 是需要交给 Asynq 重试的可控错误。
	err error
	// calls 是 DrainTerminalOutbox 被调用的次数。
	calls int
	// observedAt 是 Worker 传入 Store 的统一 UTC 观察时间。
	observedAt time.Time
	// maximum 是 Worker 请求的单次最大消费数量。
	maximum int
}

// DrainTerminalOutbox 记录扫描参数并返回可控结果。
func (stub *battleAnalyticsRunnerStub) DrainTerminalOutbox(
	_ context.Context,
	observedAt time.Time,
	maximum int,
) (battlestore.AnalyticsDrainResult, error) {
	stub.calls++
	stub.observedAt = observedAt
	stub.maximum = maximum
	return stub.result, stub.err
}
