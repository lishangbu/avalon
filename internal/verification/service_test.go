package verification_test

import (
	"context"
	"errors"
	"path/filepath"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/platform/audit"
	"github.com/lishangbu/avalon/internal/verification"
)

// TestBattleReplayServiceReturnsOnlySafeSuccessSummary 验证持久回放通过严格重放后只返回回合数摘要，
// 不把完整命令、事件、成员状态或随机轨迹带入 Asynq 输出。
func TestBattleReplayServiceReturnsOnlySafeSuccessSummary(t *testing.T) {
	t.Parallel()

	replay, err := battleengine.LoadGoldenReplay(filepath.Join("..", "battleengine", "testdata", "golden", "major-status-turn.v1.json"))
	if err != nil {
		t.Fatalf("LoadGoldenReplay() error = %v", err)
	}
	service := verification.NewBattleReplayService(replayArchiveLoaderStub{replay: replay})
	result, err := service.Verify(context.Background(), snowflake.MustParse("1048576208"))
	if err != nil {
		t.Fatalf("Verify() error = %v", err)
	}
	if !result.Passed || result.Summary != "1 个回合的持久 Battle 回放校验通过" {
		t.Fatalf("Verify() = %+v", result)
	}
}

// TestBattleReplayServiceReportsDivergenceAsCompletedVerification 验证规则偏离是一次已完成但未通过的
// 校验结果，而不是会被 Asynq 无限误判为基础设施故障的重试错误。
func TestBattleReplayServiceReportsDivergenceAsCompletedVerification(t *testing.T) {
	t.Parallel()

	replay, err := battleengine.LoadGoldenReplay(filepath.Join("..", "battleengine", "testdata", "golden", "major-status-turn.v1.json"))
	if err != nil {
		t.Fatalf("LoadGoldenReplay() error = %v", err)
	}
	replay.Turns[0].ExpectedState.Members[0].CurrentHP++
	result, err := verification.NewBattleReplayService(replayArchiveLoaderStub{replay: replay}).Verify(
		context.Background(),
		snowflake.MustParse("1048576209"),
	)
	if err != nil {
		t.Fatalf("Verify() error = %v", err)
	}
	if result.Passed || result.Summary != "持久 Battle 回放与当前 Battle Engine 输出不一致" {
		t.Fatalf("Verify() = %+v", result)
	}
}

// TestAuditHashServiceSeparatesIntegrityFailureFromInfrastructureFailure 验证链式摘要不一致会形成受控
// 未通过结果，而数据库读取故障仍返回错误交给 Asynq 重试。
func TestAuditHashServiceSeparatesIntegrityFailureFromInfrastructureFailure(t *testing.T) {
	t.Parallel()

	invalid := verification.NewAuditHashService(auditVerifierStub{err: audit.ErrHashChainInvalid})
	result, err := invalid.Verify(context.Background())
	if err != nil || result.Passed || result.Summary != "审计哈希链校验未通过" {
		t.Fatalf("Verify(invalid) = %+v, %v", result, err)
	}

	want := errors.New("database unavailable")
	broken := verification.NewAuditHashService(auditVerifierStub{err: want})
	if _, err := broken.Verify(context.Background()); !errors.Is(err, want) {
		t.Fatalf("Verify(broken) error = %v，期望 %v", err, want)
	}
}

// replayArchiveLoaderStub 代表 Battle 持久化回放档案这一数据库边界。
type replayArchiveLoaderStub struct {
	// replay 是数据库边界返回给纯引擎的冻结回放输入。
	replay battleengine.GoldenReplay
}

// LoadReplayArchive 返回测试指定的冻结回放输入。
func (stub replayArchiveLoaderStub) LoadReplayArchive(context.Context, snowflake.ID) (battleengine.GoldenReplay, error) {
	return stub.replay, nil
}

// auditVerifierStub 代表独立读取 PostgreSQL 审计账本的基础设施边界。
type auditVerifierStub struct {
	// report 是校验成功时返回的安全账本计数。
	report audit.VerificationReport
	// err 是链完整性失败或数据库故障。
	err error
}

// Verify 返回测试指定的审计校验结果。
func (stub auditVerifierStub) Verify(context.Context) (audit.VerificationReport, error) {
	return stub.report, stub.err
}
