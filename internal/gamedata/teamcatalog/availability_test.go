package teamcatalog_test

import (
	"context"
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/gamedata/teamcatalog"
)

// TestAvailabilityGateRunsWorkInsideTheAvailableGameDataTransaction 验证 Team 写入通过单一事务 seam
// 取得可用实时资料状态后，才会执行资料校验与持久化工作。
func TestAvailabilityGateRunsWorkInsideTheAvailableGameDataTransaction(t *testing.T) {
	t.Parallel()

	runner := &availableTransactionStub{}
	gate := teamcatalog.NewAvailabilityGate(runner)
	called := false
	if err := gate.WithinAvailable(context.Background(), func(context.Context) error {
		called = true
		return nil
	}); err != nil {
		t.Fatalf("WithinAvailable() error = %v", err)
	}
	if !runner.called || !called {
		t.Fatalf("WithinAvailable() runner called = %t, work called = %t, want both true", runner.called, called)
	}
}

// TestAvailabilityGatePropagatesTransactionFailure 验证事务启动失败时不会执行 Team 写入工作。
func TestAvailabilityGatePropagatesTransactionFailure(t *testing.T) {
	t.Parallel()

	transactionErr := errors.New("事务不可用")
	runner := &availableTransactionStub{err: transactionErr}
	gate := teamcatalog.NewAvailabilityGate(runner)
	called := false
	err := gate.WithinAvailable(context.Background(), func(context.Context) error {
		called = true
		return nil
	})
	if !errors.Is(err, transactionErr) {
		t.Fatalf("WithinAvailable() error = %v, want transaction error", err)
	}
	if called {
		t.Fatal("WithinAvailable() called Team work while Current Game Data was unavailable")
	}
}

// availableTransactionStub 在 Team Catalog adapter 的公开 seam 上模拟已经锁定可用实时资料状态的事务。
type availableTransactionStub struct {
	// err 是开始数据库事务时返回的确定性错误。
	err error
	// called 记录调用方是否正确通过该事务 seam 请求执行工作。
	called bool
}

// WithinTransaction 模拟向回调传播同一数据库事务 Context。
func (stub *availableTransactionStub) WithinTransaction(ctx context.Context, work func(context.Context) error) error {
	stub.called = true
	if stub.err != nil {
		return stub.err
	}
	return work(ctx)
}
