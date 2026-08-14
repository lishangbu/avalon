package admin_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/admin"
)

// TestBackgroundJobServiceRejectsUnboundedQueries 验证管理员任务页面不会请求无界数据库遍历。
func TestBackgroundJobServiceRejectsUnboundedQueries(t *testing.T) {
	t.Parallel()

	query := &backgroundJobAdaptersStub{}
	service := admin.NewBackgroundJobService(query, nil, nil, nil, nil, time.Now)
	_, err := service.List(context.Background(), admin.BackgroundJobListQuery{
		PageNumber: admin.MaximumBackgroundJobPageNumber + 1,
		PageSize:   1,
	})
	if !errors.Is(err, admin.ErrInvalidBackgroundJobQuery) {
		t.Fatalf("List() error = %v，期望 ErrInvalidBackgroundJobQuery", err)
	}
}

// TestBackgroundJobServiceDelegatesOperationWithControlledTime 验证处置命令由应用层统一设置 UTC 审计时间。
func TestBackgroundJobServiceDelegatesOperationWithControlledTime(t *testing.T) {
	t.Parallel()

	observedAt := time.Date(2026, time.July, 31, 8, 0, 0, 0, time.FixedZone("UTC+8", 8*60*60))
	repository := &backgroundJobAdaptersStub{}
	service := admin.NewBackgroundJobService(nil, nil, repository, nil, nil, func() time.Time { return observedAt })
	operation := admin.BackgroundJobOperation{
		JobID: snowflake.MustParse("1048576207"), ActorAccountID: snowflake.MustParse("1048576204"),
		IdempotencyKey: "job-retry-001", RequestID: "request-001",
	}
	if _, err := service.Retry(context.Background(), operation); err != nil {
		t.Fatalf("Retry() error = %v", err)
	}
	if repository.retryOperation.OccurredAt.Location() != time.UTC || !repository.retryOperation.OccurredAt.Equal(observedAt) {
		t.Fatalf("Retry() 操作时间 = %s，期望 UTC %s", repository.retryOperation.OccurredAt, observedAt.UTC())
	}
}

// TestBackgroundJobServiceEnqueuesVerificationCommandsWithControlledTime 验证两类人工校验命令都绑定
// 已认证管理员、请求事实和统一 UTC 时间后才进入 PostgreSQL Outbox。
func TestBackgroundJobServiceEnqueuesVerificationCommandsWithControlledTime(t *testing.T) {
	t.Parallel()

	observedAt := time.Date(2026, time.August, 3, 9, 30, 0, 0, time.FixedZone("UTC+8", 8*60*60))
	repository := &backgroundJobAdaptersStub{}
	service := admin.NewBackgroundJobService(nil, nil, repository, nil, nil, func() time.Time { return observedAt })
	operation := admin.VerificationJobOperation{
		BattleID:       snowflake.MustParse("1048576211"),
		ActorAccountID: snowflake.MustParse("1048576212"),
		IdempotencyKey: "verification-301", RequestID: "request-301",
	}
	if _, err := service.EnqueueBattleReplayVerification(context.Background(), operation); err != nil {
		t.Fatalf("EnqueueBattleReplayVerification() error = %v", err)
	}
	if repository.replayOperation.OccurredAt.Location() != time.UTC ||
		!repository.replayOperation.OccurredAt.Equal(observedAt) {
		t.Fatalf("回放校验命令时间 = %s", repository.replayOperation.OccurredAt)
	}

	operation.BattleID = snowflake.ID(0)
	operation.IdempotencyKey = "verification-302"
	if _, err := service.EnqueueAuditHashVerification(context.Background(), operation); err != nil {
		t.Fatalf("EnqueueAuditHashVerification() error = %v", err)
	}
	if repository.auditOperation.OccurredAt.Location() != time.UTC ||
		!repository.auditOperation.OccurredAt.Equal(observedAt) {
		t.Fatalf("审计校验命令时间 = %s", repository.auditOperation.OccurredAt)
	}
}

// backgroundJobAdaptersStub 为后台任务应用服务提供无数据库测试替身。
type backgroundJobAdaptersStub struct {
	// retryOperation 保存服务转交的重试命令。
	retryOperation admin.BackgroundJobOperation
	// replayOperation 保存服务转交的持久 Battle 回放校验命令。
	replayOperation admin.VerificationJobOperation
	// auditOperation 保存服务转交的审计哈希链校验命令。
	auditOperation admin.VerificationJobOperation
}

// List 实现受限查询替身，不需要构造真实 Asynq 任务。
func (backgroundJobAdaptersStub) List(context.Context, admin.BackgroundJobListQuery) (admin.BackgroundJobPage, error) {
	return admin.BackgroundJobPage{}, nil
}

// Get 实现单任务读取替身。
func (backgroundJobAdaptersStub) Get(context.Context, snowflake.ID) (admin.BackgroundJob, error) {
	return admin.BackgroundJob{}, nil
}

// Retry 保存重试命令，以供测试断言应用服务赋予的时间。
func (stub *backgroundJobAdaptersStub) Retry(
	_ context.Context,
	operation admin.BackgroundJobOperation,
) (admin.BackgroundJob, error) {
	stub.retryOperation = operation
	return admin.BackgroundJob{}, nil
}

// Cancel 实现取消替身。
func (backgroundJobAdaptersStub) Cancel(context.Context, admin.BackgroundJobOperation) (admin.BackgroundJob, error) {
	return admin.BackgroundJob{}, nil
}

// EnqueueBattleReplayVerification 保存人工持久 Battle 回放校验命令。
func (stub *backgroundJobAdaptersStub) EnqueueBattleReplayVerification(
	_ context.Context,
	operation admin.VerificationJobOperation,
) (admin.BackgroundJob, error) {
	stub.replayOperation = operation
	return admin.BackgroundJob{}, nil
}

// EnqueueAuditHashVerification 保存人工审计哈希链校验命令。
func (stub *backgroundJobAdaptersStub) EnqueueAuditHashVerification(
	_ context.Context,
	operation admin.VerificationJobOperation,
) (admin.BackgroundJob, error) {
	stub.auditOperation = operation
	return admin.BackgroundJob{}, nil
}
