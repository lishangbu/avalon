//go:build integration

package worker

import (
	"context"
	"encoding/json"
	"encoding/json/jsontext"
	"errors"
	"fmt"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/ent/backgroundschedule"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/persistence"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

const asynqPostgresImage = "postgres:18.4@sha256:311136771dca6826c3b6e691ebf8cb6e896e165074bc57a728f9619f25f0c4c7"

// TestAsynqServerPersistsOneJobPerScheduleOccurrence 验证动态调度发生时间是数据库幂等键，
// 即使同一 occurrence 被再次扫描，也只产生一条权威任务和一条 Outbox。
func TestAsynqServerPersistsOneJobPerScheduleOccurrence(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startAsynqDatabase(t, ctx)
	registry, err := NewRegistry(TaskDefinition{
		Kind: "test.schedule.v1", Handler: func(context.Context, json.RawMessage) (TaskResult, error) { return TaskResult{}, nil },
	})
	if err != nil {
		t.Fatalf("NewRegistry() error = %v", err)
	}
	server := &AsynqServer{database: pool.Persistence(), registry: registry, identifiers: snowflake.TestSource(snowflake.NewTestID)}
	now := time.Date(2026, time.August, 6, 2, 0, 0, 0, time.UTC)
	scheduleID := snowflake.NewTestID()
	if _, err := pool.Exec(ctx, `
INSERT INTO background_schedule (
    id, code, name, task_kind, schedule_kind, interval_seconds, missed_run_policy,
    parameters, enabled, next_run_at, version, created_at, updated_at
) VALUES ($1, 'test-schedule', '测试调度', 'test.schedule.v1', 'interval', 60, 'coalesce',
    '{}'::jsonb, true, $2, 1, $2, $2)`, scheduleID, now); err != nil {
		t.Fatalf("创建调度夹具: %v", err)
	}
	if err := server.scheduleDue(ctx, now); err != nil {
		t.Fatalf("scheduleDue() error = %v", err)
	}
	if _, err := pool.Exec(ctx, `UPDATE background_schedule SET next_run_at = $2 WHERE id = $1`, scheduleID, now); err != nil {
		t.Fatalf("恢复相同 occurrence: %v", err)
	}
	if err := server.scheduleDue(ctx, now); err != nil {
		t.Fatalf("scheduleDue() replay error = %v", err)
	}
	var jobs, outbox int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM background_job WHERE schedule_id = $1 AND scheduled_for = $2`, scheduleID, now).Scan(&jobs); err != nil {
		t.Fatalf("统计调度任务: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM outbox_message WHERE aggregate_id IN (SELECT id FROM background_job WHERE schedule_id = $1)`, scheduleID).Scan(&outbox); err != nil {
		t.Fatalf("统计调度 Outbox: %v", err)
	}
	if jobs != 1 || outbox != 1 {
		t.Fatalf("调度 occurrence 幂等计数 jobs=%d outbox=%d", jobs, outbox)
	}
}

// TestAsynqServerConcurrentScheduleScansRemainIdempotent 验证两个并发调度扫描即使同时
// 看到同一个 occurrence，也只能提交一条后台任务和一条 Outbox 事实。
func TestAsynqServerConcurrentScheduleScansRemainIdempotent(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startAsynqDatabase(t, ctx)
	registry, err := NewRegistry(TaskDefinition{Kind: "test.concurrent-schedule.v1", Handler: func(context.Context, json.RawMessage) (TaskResult, error) { return TaskResult{}, nil }})
	if err != nil {
		t.Fatalf("NewRegistry() error = %v", err)
	}
	server := &AsynqServer{database: pool.Persistence(), registry: registry, identifiers: snowflake.TestSource(snowflake.NewTestID)}
	now := time.Date(2026, time.August, 6, 3, 0, 0, 0, time.UTC)
	scheduleID := snowflake.NewTestID()
	if _, err := pool.Exec(ctx, `
INSERT INTO background_schedule (
    id, code, name, task_kind, schedule_kind, interval_seconds, missed_run_policy,
    parameters, enabled, next_run_at, version, created_at, updated_at
) VALUES ($1, 'concurrent-schedule', '并发调度', 'test.concurrent-schedule.v1', 'interval', 60, 'coalesce',
    '{}'::jsonb, true, $2, 1, $2, $2)`, scheduleID, now); err != nil {
		t.Fatalf("创建调度夹具: %v", err)
	}
	errs := make(chan error, 2)
	for i := 0; i < 2; i++ {
		go func() { errs <- server.scheduleDue(ctx, now) }()
	}
	for i := 0; i < 2; i++ {
		if err := <-errs; err != nil {
			t.Fatalf("并发 scheduleDue() error = %v", err)
		}
	}
	var jobs, outbox int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM background_job WHERE schedule_id = $1 AND scheduled_for = $2`, scheduleID, now).Scan(&jobs); err != nil {
		t.Fatalf("统计并发调度任务: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM outbox_message WHERE aggregate_id IN (SELECT id FROM background_job WHERE schedule_id = $1)`, scheduleID).Scan(&outbox); err != nil {
		t.Fatalf("统计并发调度 Outbox: %v", err)
	}
	if jobs != 1 || outbox != 1 {
		t.Fatalf("并发 occurrence 幂等计数 jobs=%d outbox=%d", jobs, outbox)
	}
}

// TestEntTransactionClientSharesOuterTransaction 验证事务 Context 中的 Ent Client
// 与外层 SQL 事务共享提交、回滚边界，并拒绝再次开启嵌套事务。
func TestEntTransactionClientSharesOuterTransaction(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	p := startAsynqDatabase(t, ctx)
	code := "transaction-boundary"
	now := time.Now().UTC()
	wantErr := errors.New("主动回滚")
	err := p.Persistence().WithinTransaction(ctx, persistence.TransactionOptions{Isolation: persistence.IsolationReadCommitted}, func(txctx context.Context) error {
		if _, err := p.Client(txctx).BackgroundSchedule.Create().SetID(snowflake.NewTestID()).SetCode(code).SetName("事务测试").SetTaskKind("test.transaction.v1").SetScheduleKind("interval").SetIntervalSeconds(60).SetMissedRunPolicy("coalesce").SetParameters(jsontext.Value([]byte(`{}`))).SetEnabled(true).SetNextRunAt(now).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now).Save(txctx); err != nil {
			return err
		}
		nestedErr := p.Persistence().WithinTransaction(txctx, persistence.TransactionOptions{Isolation: persistence.IsolationReadCommitted}, func(context.Context) error { return nil })
		if !errors.Is(nestedErr, persistence.ErrNestedTransaction) {
			return fmt.Errorf("嵌套事务错误 = %v", nestedErr)
		}
		return wantErr
	})
	if !errors.Is(err, wantErr) {
		t.Fatalf("WithinTransaction() error = %v, want %v", err, wantErr)
	}
	if count, err := p.Client(ctx).BackgroundSchedule.Query().Where(backgroundschedule.CodeEQ(code)).Count(ctx); err != nil {
		t.Fatalf("读取回滚调度: %v", err)
	} else if count != 0 {
		t.Fatalf("回滚后调度数量 = %d, want 0", count)
	}
}

// TestAsynqServerMovesOutboxToDeadAfterBoundedFailures 验证 Outbox 失败次数、退避上限、
// 错误摘要清理和最终 dead 状态全部由 PostgreSQL 权威记录。
func TestAsynqServerMovesOutboxToDeadAfterBoundedFailures(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startAsynqDatabase(t, ctx)
	server := &AsynqServer{database: pool.Persistence()}
	jobID, outboxID := snowflake.NewTestID(), snowflake.NewTestID()
	if _, err := pool.Exec(ctx, `
INSERT INTO background_job (
    id, kind, queue, state, parameters, attempt_count, max_attempts,
    next_attempt_at, version, created_at, updated_at
) VALUES ($1, 'test.failure.v1', 'default', 'pending', '{}'::jsonb, 0, 10, now(), 1, now(), now())`, jobID); err != nil {
		t.Fatalf("创建失败任务夹具: %v", err)
	}
	if _, err := pool.Exec(ctx, `
INSERT INTO outbox_message (
    id, topic, aggregate_id, payload, state, attempt_count, available_at,
    lease_expires_at, created_at, updated_at
) VALUES ($1, $2, $3, '{}'::jsonb, 'processing', 18, now(), now() + interval '30 seconds', now(), now())`,
		outboxID, backgroundJobTaskType, jobID); err != nil {
		t.Fatalf("创建失败 Outbox 夹具: %v", err)
	}
	message := claimedOutbox{id: outboxID, jobID: jobID, attemptCount: 18}
	if err := server.markOutboxFailed(ctx, message, errors.New("连接失败\x00\x01")); err != nil {
		t.Fatalf("markOutboxFailed() error = %v", err)
	}
	var state string
	var attempts int
	var availableAt time.Time
	var lastError string
	if err := pool.QueryRow(ctx, `SELECT state, attempt_count, available_at, last_error FROM outbox_message WHERE id = $1`, outboxID).Scan(&state, &attempts, &availableAt, &lastError); err != nil {
		t.Fatalf("读取第十九次失败: %v", err)
	}
	if state != "pending" || attempts != 19 || availableAt.After(time.Now().UTC().Add(outboxMaximumBackoff+time.Minute)) || lastError != "连接失败" {
		t.Fatalf("第十九次失败 state=%s attempts=%d availableAt=%s lastError=%q", state, attempts, availableAt, lastError)
	}
	if _, err := pool.Exec(ctx, `UPDATE outbox_message SET state = 'processing' WHERE id = $1`, outboxID); err != nil {
		t.Fatalf("重新认领 Outbox: %v", err)
	}
	message.attemptCount = 19
	if err := server.markOutboxFailed(ctx, message, errors.New("仍然失败")); err != nil {
		t.Fatalf("markOutboxFailed() terminal error = %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT state, attempt_count FROM outbox_message WHERE id = $1`, outboxID).Scan(&state, &attempts); err != nil {
		t.Fatalf("读取最终失败: %v", err)
	}
	if state != "dead" || attempts != outboxMaximumAttempts {
		t.Fatalf("最终失败 state=%s attempts=%d", state, attempts)
	}
}

func startAsynqDatabase(t *testing.T, ctx context.Context) *database.Pool {
	t.Helper()
	container, err := postgres.Run(ctx, asynqPostgresImage,
		postgres.WithDatabase("avalon_asynq_test"), postgres.WithUsername("avalon"),
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
	return pool
}
