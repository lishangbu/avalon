//go:build integration

package persistence_test

import (
	"context"
	"errors"
	"testing"
	"time"

	adminpersistence "github.com/lishangbu/avalon/internal/admin/persistence"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/persistence"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/security/authentication"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

const authenticationPostgresImage = "postgres:18.4@sha256:311136771dca6826c3b6e691ebf8cb6e896e165074bc57a728f9619f25f0c4c7"

func TestAdminAuthenticationRepositoryCommitsSuccessfulLoginFactsAtomically(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startAdminAuthenticationDatabase(t, ctx)
	repository := adminpersistence.NewAuthenticationAdapters(pool, nil)
	now := time.Date(2026, time.August, 14, 8, 0, 0, 0, time.UTC)
	accountID := snowflake.NewTestID()
	insertAdminLoginAccount(t, ctx, pool, accountID, "admin", "expected-hash", now)
	record := loginSuccessRecord(accountID, "expected-hash", "admin-login-success", now, 1)

	securityVersion, err := repository.CommitLoginSuccess(ctx, record)
	if err != nil {
		t.Fatalf("CommitLoginSuccess() error = %v", err)
	}
	if securityVersion != 1 {
		t.Fatalf("securityVersion = %d, want 1", securityVersion)
	}
	var status string
	var failedAttempts int32
	var locked bool
	if err := pool.QueryRow(ctx, `SELECT status, failed_login_attempts, locked_until IS NOT NULL FROM admin_account WHERE id = $1`, accountID).Scan(&status, &failedAttempts, &locked); err != nil {
		t.Fatalf("读取管理员登录保护状态: %v", err)
	}
	var attempts, audits int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM admin_login_attempt WHERE id = $1 AND succeeded`, record.LoginAttemptID).Scan(&attempts); err != nil {
		t.Fatalf("统计管理员成功尝试: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM admin_audit_log WHERE id = $1 AND action_code = 'admin.login.succeeded'`, record.AuditID).Scan(&audits); err != nil {
		t.Fatalf("统计管理员成功审计: %v", err)
	}
	if status != "active" || failedAttempts != 0 || locked || attempts != 1 || audits != 1 {
		t.Fatalf("提交结果 status=%s failed=%d locked=%t attempts=%d audits=%d", status, failedAttempts, locked, attempts, audits)
	}
}

func TestAdminAuthenticationRepositoryRollsBackConflictingLogin(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startAdminAuthenticationDatabase(t, ctx)
	repository := adminpersistence.NewAuthenticationAdapters(pool, nil)
	now := time.Date(2026, time.August, 14, 8, 0, 0, 0, time.UTC)
	accountID := snowflake.NewTestID()
	insertAdminLoginAccount(t, ctx, pool, accountID, "conflict-admin", "current-hash", now)
	record := loginSuccessRecord(accountID, "stale-hash", "admin-login-conflict", now, 1)

	_, err := repository.CommitLoginSuccess(ctx, record)
	if !errors.Is(err, authentication.ErrInvalidCredentials) {
		t.Fatalf("CommitLoginSuccess() error = %v, want ErrInvalidCredentials", err)
	}
	var status string
	var failedAttempts, attempts, audits int
	if err := pool.QueryRow(ctx, `SELECT status, failed_login_attempts FROM admin_account WHERE id = $1`, accountID).Scan(&status, &failedAttempts); err != nil {
		t.Fatalf("读取冲突后管理员账号: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM admin_login_attempt WHERE id = $1`, record.LoginAttemptID).Scan(&attempts); err != nil {
		t.Fatalf("统计冲突后登录尝试: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM admin_audit_log WHERE id = $1`, record.AuditID).Scan(&audits); err != nil {
		t.Fatalf("统计冲突后审计: %v", err)
	}
	if status != "locked" || failedAttempts != 4 || attempts != 0 || audits != 0 {
		t.Fatalf("回滚结果 status=%s failed=%d attempts=%d audits=%d", status, failedAttempts, attempts, audits)
	}
}

func startAdminAuthenticationDatabase(t *testing.T, ctx context.Context) *database.Pool {
	t.Helper()
	container, err := postgres.Run(ctx, authenticationPostgresImage,
		postgres.WithDatabase("avalon_admin_auth_test"), postgres.WithUsername("avalon"),
		postgres.WithPassword("avalon"), postgres.BasicWaitStrategies())
	if err != nil {
		t.Fatalf("启动 PostgreSQL: %v", err)
	}
	t.Cleanup(func() { _ = container.Terminate(context.Background()) })
	databaseURL, err := container.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		t.Fatalf("读取 PostgreSQL 地址: %v", err)
	}
	pool, err := database.Open(database.Config{URL: databaseURL, MaxOpenConnections: 10, MaxIdleConnections: 5})
	if err != nil {
		t.Fatalf("连接 PostgreSQL: %v", err)
	}
	t.Cleanup(pool.Close)
	if err := pool.Persistence().ApplySchema(ctx, persistence.SchemaModeCreate); err != nil {
		t.Fatalf("创建 Ent Schema: %v", err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO audit_hash_chain_state (id, ledger, latest_hash, updated_at) VALUES ($1, 'admin_audit_log', ''::bytea, now())`, snowflake.NewTestID()); err != nil {
		t.Fatalf("创建管理员审计链尾: %v", err)
	}
	return pool
}

func insertAdminLoginAccount(t *testing.T, ctx context.Context, pool *database.Pool, id snowflake.ID, username, passwordHash string, now time.Time) {
	t.Helper()
	if _, err := pool.Exec(ctx, `INSERT INTO admin_account (id, username, username_key, display_name, password_hash, password_algorithm, password_parameters, status, failed_login_attempts, locked_until, created_at, updated_at) VALUES ($1, $2, $2, $2, $3, 'argon2id', '{}'::jsonb, 'locked', 4, $4, $5, $5)`, id, username, passwordHash, now.Add(time.Hour), now.Add(-time.Hour)); err != nil {
		t.Fatalf("创建管理员登录夹具: %v", err)
	}
}

func loginSuccessRecord(accountID snowflake.ID, passwordHash, requestID string, now time.Time, securityVersion int64) authentication.LoginSuccessRecord {
	return authentication.LoginSuccessRecord{
		LoginAttemptID: snowflake.NewTestID(), AuditID: snowflake.NewTestID(), RequestID: requestID,
		UsernameDigest: make([]byte, 32), ExpectedPasswordHash: passwordHash,
		Session: authentication.SessionRecord{ID: snowflake.NewTestID(), FamilyID: snowflake.NewTestID(), AccountID: accountID, SecurityVersion: securityVersion, CreatedAt: now, ExpiresAt: now.Add(time.Hour), IdleExpiresAt: now.Add(time.Minute), LastActivityAt: now},
	}
}
