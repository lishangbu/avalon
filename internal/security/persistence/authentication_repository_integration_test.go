//go:build integration

package persistence_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/persistence"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/security/authentication"
	securitypersistence "github.com/lishangbu/avalon/internal/security/persistence"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

const authenticationPostgresImage = "postgres:18.4@sha256:311136771dca6826c3b6e691ebf8cb6e896e165074bc57a728f9619f25f0c4c7"

func TestPlayerAuthenticationRepositoryCommitsSecurityVersionAndAuditAtomically(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startPlayerAuthenticationDatabase(t, ctx)
	repository := securitypersistence.NewAuthenticationAdapters(pool, nil)
	now := time.Date(2026, time.August, 14, 8, 0, 0, 0, time.UTC)
	accountID := snowflake.NewTestID()
	insertPlayerLoginAccount(t, ctx, pool, accountID, "player", "expected-hash", 3, now)
	record := playerLoginSuccessRecord(accountID, "expected-hash", "player-login-success", now, 3)

	securityVersion, err := repository.CommitLoginSuccess(ctx, record)
	if err != nil {
		t.Fatalf("CommitLoginSuccess() error = %v", err)
	}
	if securityVersion != 4 {
		t.Fatalf("securityVersion = %d, want 4", securityVersion)
	}
	var status string
	var version int64
	var failedAttempts int32
	var locked bool
	if err := pool.QueryRow(ctx, `SELECT status, security_version, failed_login_attempts, locked_until IS NOT NULL FROM account WHERE id = $1`, accountID).Scan(&status, &version, &failedAttempts, &locked); err != nil {
		t.Fatalf("读取玩家登录保护状态: %v", err)
	}
	var audits int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM administration_audit_log WHERE id = $1 AND action_code = 'security.login.succeeded'`, record.AuditID).Scan(&audits); err != nil {
		t.Fatalf("统计玩家成功审计: %v", err)
	}
	if status != "active" || version != 4 || failedAttempts != 0 || locked || audits != 1 {
		t.Fatalf("提交结果 status=%s version=%d failed=%d locked=%t audits=%d", status, version, failedAttempts, locked, audits)
	}
}

func TestPlayerAuthenticationRepositoryRollsBackConflictingLogin(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startPlayerAuthenticationDatabase(t, ctx)
	repository := securitypersistence.NewAuthenticationAdapters(pool, nil)
	now := time.Date(2026, time.August, 14, 8, 0, 0, 0, time.UTC)
	accountID := snowflake.NewTestID()
	insertPlayerLoginAccount(t, ctx, pool, accountID, "conflict-player", "current-hash", 5, now)
	record := playerLoginSuccessRecord(accountID, "current-hash", "player-login-conflict", now, 4)

	_, err := repository.CommitLoginSuccess(ctx, record)
	if !errors.Is(err, authentication.ErrInvalidCredentials) {
		t.Fatalf("CommitLoginSuccess() error = %v, want ErrInvalidCredentials", err)
	}
	var status string
	var version int64
	var failedAttempts, audits int
	if err := pool.QueryRow(ctx, `SELECT status, security_version, failed_login_attempts FROM account WHERE id = $1`, accountID).Scan(&status, &version, &failedAttempts); err != nil {
		t.Fatalf("读取冲突后玩家账号: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM administration_audit_log WHERE id = $1`, record.AuditID).Scan(&audits); err != nil {
		t.Fatalf("统计冲突后审计: %v", err)
	}
	if status != "locked" || version != 5 || failedAttempts != 4 || audits != 0 {
		t.Fatalf("回滚结果 status=%s version=%d failed=%d audits=%d", status, version, failedAttempts, audits)
	}
}

func startPlayerAuthenticationDatabase(t *testing.T, ctx context.Context) *database.Pool {
	t.Helper()
	container, err := postgres.Run(ctx, authenticationPostgresImage,
		postgres.WithDatabase("avalon_player_auth_test"), postgres.WithUsername("avalon"),
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
	if _, err := pool.Exec(ctx, `INSERT INTO audit_hash_chain_state (id, ledger, latest_hash, updated_at) VALUES ($1, 'administration_audit_log', ''::bytea, now())`, snowflake.NewTestID()); err != nil {
		t.Fatalf("创建玩家审计链尾: %v", err)
	}
	return pool
}

func insertPlayerLoginAccount(t *testing.T, ctx context.Context, pool *database.Pool, id snowflake.ID, username, passwordHash string, securityVersion int64, now time.Time) {
	t.Helper()
	if _, err := pool.Exec(ctx, `INSERT INTO account (id, username, username_key, display_name, password_hash, password_algorithm, password_parameters, status, security_version, failed_login_attempts, locked_until, created_at, updated_at) VALUES ($1, $2, $2, $2, $3, 'argon2id', '{}'::jsonb, 'locked', $4, 4, $5, $6, $6)`, id, username, passwordHash, securityVersion, now.Add(time.Hour), now.Add(-time.Hour)); err != nil {
		t.Fatalf("创建玩家登录夹具: %v", err)
	}
}

func playerLoginSuccessRecord(accountID snowflake.ID, passwordHash, requestID string, now time.Time, securityVersion int64) authentication.LoginSuccessRecord {
	return authentication.LoginSuccessRecord{
		LoginAttemptID: snowflake.NewTestID(), AuditID: snowflake.NewTestID(), RequestID: requestID,
		UsernameDigest: make([]byte, 32), ExpectedPasswordHash: passwordHash,
		Session: authentication.SessionRecord{ID: snowflake.NewTestID(), FamilyID: snowflake.NewTestID(), AccountID: accountID, SecurityVersion: securityVersion, CreatedAt: now, ExpiresAt: now.Add(time.Hour), IdleExpiresAt: now.Add(time.Minute), LastActivityAt: now},
	}
}
