package worker_test

import (
	"context"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/verification"
	"github.com/lishangbu/avalon/internal/worker"
)

// TestBattleReplayVerificationWorkerUsesStableBattleIdentifier 验证任务参数中的强类型 Battle 标识会被
// 直接交给应用服务，并且只返回应用服务的安全摘要。
func TestBattleReplayVerificationWorkerUsesStableBattleIdentifier(t *testing.T) {
	t.Parallel()

	battleID := snowflake.MustParse("1048576210")
	runner := &matchReplayVerificationRunnerStub{
		result: verification.Result{Passed: true, Summary: "2 个回合的持久 Battle 回放校验通过"},
	}
	jobWorker := worker.NewBattleReplayVerificationWorker(runner)
	result, err := jobWorker.Run(context.Background(), worker.BattleReplayVerificationArgs{
		BattleID: battleID, ActorAccountID: snowflake.MustParse("1048576211"), RequestID: "request-201",
	})
	if err != nil {
		t.Fatalf("Work() error = %v", err)
	}
	if runner.battleID != battleID || result != runner.result {
		t.Fatalf("runner battleID = %s, result = %+v", runner.battleID, result)
	}
}

// TestBattleReplayVerificationWorkerRejectsMissingIdentifiers 验证回放校验不会把缺失的 Battle
// 或管理员 Identifier 交给应用服务。
func TestBattleReplayVerificationWorkerRejectsMissingIdentifiers(t *testing.T) {
	t.Parallel()

	for _, args := range []worker.BattleReplayVerificationArgs{
		{ActorAccountID: snowflake.MustParse("1048576211"), RequestID: "request-201"},
		{BattleID: snowflake.MustParse("1048576210"), RequestID: "request-201"},
	} {
		runner := &matchReplayVerificationRunnerStub{}
		if _, err := worker.NewBattleReplayVerificationWorker(runner).Run(context.Background(), args); err == nil {
			t.Fatalf("缺少 Identifier 的参数 %+v 不应通过", args)
		}
		if runner.battleID.IsValid() {
			t.Fatalf("无效参数仍调用了 runner，battleID = %s", runner.battleID)
		}
	}
}

// TestAuditHashVerificationWorkerRecordsSafeResult 验证人工和周期审计任务共用同一应用服务和安全输出模型。
func TestAuditHashVerificationWorkerRecordsSafeResult(t *testing.T) {
	t.Parallel()

	runner := &auditHashVerificationRunnerStub{
		result: verification.Result{Passed: false, Summary: "审计哈希链校验未通过"},
	}
	jobWorker := worker.NewAuditHashVerificationWorker(runner)
	actorID := snowflake.MustParse("1048576212")
	result, err := jobWorker.Run(context.Background(), worker.AuditHashVerificationArgs{
		Trigger: "manual", ActorAccountID: &actorID, RequestID: "request-203",
	})
	if err != nil {
		t.Fatalf("Run() error = %v", err)
	}
	if runner.calls != 1 || result != runner.result {
		t.Fatalf("runner calls = %d, result = %+v", runner.calls, result)
	}
}

// matchReplayVerificationRunnerStub 代表回放校验应用服务这一稳定接缝。
type matchReplayVerificationRunnerStub struct {
	// battleID 是 Worker 解析后交付的 Battle Identifier。
	battleID snowflake.ID
	// result 是允许写入 PostgreSQL 的安全摘要。
	result verification.Result
}

// Verify 保存目标 Battle 并返回安全摘要。
func (stub *matchReplayVerificationRunnerStub) Verify(_ context.Context, battleID snowflake.ID) (verification.Result, error) {
	stub.battleID = battleID
	return stub.result, nil
}

// auditHashVerificationRunnerStub 代表审计哈希链校验应用服务这一稳定接缝。
type auditHashVerificationRunnerStub struct {
	// calls 是任务实际触发校验的次数。
	calls int
	// result 是允许写入 PostgreSQL 的安全摘要。
	result verification.Result
}

// Verify 记录调用并返回安全摘要。
func (stub *auditHashVerificationRunnerStub) Verify(context.Context) (verification.Result, error) {
	stub.calls++
	return stub.result, nil
}
