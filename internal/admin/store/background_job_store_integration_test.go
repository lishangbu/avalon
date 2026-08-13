//go:build integration

package store_test

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/admin"
	adminstore "github.com/lishangbu/avalon/internal/admin/store"
	platformaudit "github.com/lishangbu/avalon/internal/platform/audit"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/persistence"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

const backgroundJobPostgresImage = "postgres:18.4@sha256:311136771dca6826c3b6e691ebf8cb6e896e165074bc57a728f9619f25f0c4c7"

// TestBackgroundJobStoreKeepsJobOutboxAuditAndIdempotencyAtomic 验证人工校验任务的权威任务、
// Outbox、管理员审计和幂等响应只会共同提交一次，且审计哈希链可独立重算。
func TestBackgroundJobStoreKeepsJobOutboxAuditAndIdempotencyAtomic(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool, actorID := startBackgroundJobDatabase(t, ctx)
	store := adminstore.NewBackgroundJobStore(pool, snowflake.NewTestID)
	operation := admin.VerificationJobOperation{
		ActorAccountID: actorID, IdempotencyKey: "audit-verification-1", RequestID: "request-audit-verification-1",
		OccurredAt: time.Date(2026, time.August, 6, 1, 0, 0, 0, time.UTC),
	}
	created, err := store.EnqueueAuditHashVerification(ctx, operation)
	if err != nil {
		t.Fatalf("EnqueueAuditHashVerification() error = %v", err)
	}
	replayed, err := store.EnqueueAuditHashVerification(ctx, operation)
	if err != nil || replayed.ID != created.ID {
		t.Fatalf("幂等重放 = %+v, error = %v", replayed, err)
	}
	var jobs, outbox, audits, idempotencyRecords int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM background_job WHERE id = $1`, created.ID).Scan(&jobs); err != nil {
		t.Fatalf("统计后台任务: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM outbox_message WHERE aggregate_id = $1`, created.ID).Scan(&outbox); err != nil {
		t.Fatalf("统计 Outbox: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM admin_audit_log WHERE object_id = $1`, created.ID.String()).Scan(&audits); err != nil {
		t.Fatalf("统计管理员审计: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM admin_idempotency_record WHERE actor_account_id = $1`, actorID).Scan(&idempotencyRecords); err != nil {
		t.Fatalf("统计幂等记录: %v", err)
	}
	if jobs != 1 || outbox != 1 || audits != 1 || idempotencyRecords != 1 {
		t.Fatalf("原子写入计数 jobs=%d outbox=%d audits=%d idempotency=%d", jobs, outbox, audits, idempotencyRecords)
	}
	if _, err := platformaudit.NewVerifier(pool).Verify(ctx); err != nil {
		t.Fatalf("Verify() error = %v", err)
	}
}

// TestBackgroundJobStoreRetryRestoresFailedOutboxOnce 验证人工重试会恢复同一条 Outbox，
// 清零投递次数并通过幂等响应避免重复审计。
func TestBackgroundJobStoreRetryRestoresFailedOutboxOnce(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool, actorID := startBackgroundJobDatabase(t, ctx)
	store := adminstore.NewBackgroundJobStore(pool, snowflake.NewTestID)
	created, err := store.EnqueueAuditHashVerification(ctx, admin.VerificationJobOperation{
		ActorAccountID: actorID, IdempotencyKey: "enqueue-retry-target", RequestID: "request-enqueue-retry-target", OccurredAt: time.Now().UTC(),
	})
	if err != nil {
		t.Fatalf("EnqueueAuditHashVerification() error = %v", err)
	}
	if _, err := pool.Exec(ctx, `UPDATE background_job SET state = 'failed', completed_at = now() WHERE id = $1`, created.ID); err != nil {
		t.Fatalf("设置失败任务: %v", err)
	}
	if _, err := pool.Exec(ctx, `UPDATE outbox_message SET state = 'dead', attempt_count = 10 WHERE aggregate_id = $1`, created.ID); err != nil {
		t.Fatalf("设置死亡 Outbox: %v", err)
	}
	operation := admin.BackgroundJobOperation{
		JobID: created.ID, ActorAccountID: actorID, IdempotencyKey: "retry-job-1", RequestID: "request-retry-job-1",
		OccurredAt: time.Now().UTC().Add(time.Second),
	}
	retried, err := store.Retry(ctx, operation)
	if err != nil || retried.State != admin.BackgroundJobStateRetryRequested {
		t.Fatalf("Retry() = %+v, error = %v", retried, err)
	}
	replayed, err := store.Retry(ctx, operation)
	if err != nil || replayed.ID != retried.ID {
		t.Fatalf("Retry() replay = %+v, error = %v", replayed, err)
	}
	var state string
	var attempts int
	var audits int
	if err := pool.QueryRow(ctx, `SELECT state, attempt_count FROM outbox_message WHERE aggregate_id = $1`, created.ID).Scan(&state, &attempts); err != nil {
		t.Fatalf("读取恢复后的 Outbox: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM admin_audit_log WHERE action_code = 'admin.background_job.retry' AND object_id = $1`, created.ID.String()).Scan(&audits); err != nil {
		t.Fatalf("统计重试审计: %v", err)
	}
	if state != "pending" || attempts != 0 || audits != 1 {
		t.Fatalf("恢复结果 state=%s attempts=%d audits=%d", state, attempts, audits)
	}
}

func startBackgroundJobDatabase(t *testing.T, ctx context.Context) (*database.Pool, snowflake.ID) {
	t.Helper()
	container, err := postgres.Run(ctx, backgroundJobPostgresImage,
		postgres.WithDatabase("avalon_background_job_test"), postgres.WithUsername("avalon"),
		postgres.WithPassword("avalon"), postgres.BasicWaitStrategies())
	if err != nil {
		t.Fatalf("启动 PostgreSQL: %v", err)
	}
	t.Cleanup(func() { _ = container.Terminate(context.Background()) })
	databaseURL, err := container.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		t.Fatalf("读取 PostgreSQL 地址: %v", err)
	}
	pool, err := database.Open(database.Config{URL: databaseURL, MaxOpenConnections: 20, MaxIdleConnections: 10})
	if err != nil {
		t.Fatalf("连接 PostgreSQL: %v", err)
	}
	t.Cleanup(pool.Close)
	if err := pool.Persistence().ApplySchema(ctx, persistence.SchemaModeCreate); err != nil {
		t.Fatalf("创建 Ent Schema: %v", err)
	}
	actorID := snowflake.NewTestID()
	if _, err := pool.Exec(ctx, `
INSERT INTO admin_account (
    id, username, username_key, display_name, password_hash, password_algorithm,
    password_parameters, status, failed_login_attempts, created_at, updated_at
) VALUES ($1, 'job-admin', 'job-admin', '任务管理员', 'unused', 'argon2id', '{}'::jsonb, 'active', 0, now(), now())`, actorID); err != nil {
		t.Fatalf("创建管理员夹具: %v", err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO audit_hash_chain_state (id, ledger, latest_hash, updated_at) VALUES
(1048577, 'admin_audit_log', ''::bytea, now()), (1048578, 'administration_audit_log', ''::bytea, now())`); err != nil {
		t.Fatalf("创建审计链尾夹具: %v", err)
	}
	return pool, actorID
}
