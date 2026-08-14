//go:build integration

package authentication_test

import (
	"context"
	"crypto/rand"
	"errors"
	"net"
	"testing"
	"time"

	adminpersistence "github.com/lishangbu/avalon/internal/admin/persistence"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/persistence"
	"github.com/lishangbu/avalon/internal/platform/sessionstore"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/security/account"
	"github.com/lishangbu/avalon/internal/security/authentication"
	securitypersistence "github.com/lishangbu/avalon/internal/security/persistence"
	"github.com/lishangbu/avalon/internal/security/session"
	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
	"github.com/testcontainers/testcontainers-go/wait"
)

const (
	lifecyclePostgresImage = "postgres:18.4@sha256:311136771dca6826c3b6e691ebf8cb6e896e165074bc57a728f9619f25f0c4c7"
	lifecycleValkeyImage   = "valkey/valkey@sha256:8e8d64b405ce18f41b8e5ee20aa4687a8ed0022d1298f2ce31cdcf3a76e09411"
)

type loginLifecycleAdapters interface {
	authentication.AuthenticationQuery
	authentication.AuthenticationRepository
	authentication.RefreshRotationStore
	authentication.LogoutStore
	AuthenticateSession(context.Context, []byte, time.Time) (authentication.Principal, error)
}

func TestAdminAndPlayerLoginLifecycleAgainstPostgreSQLAndValkey(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Minute)
	defer cancel()
	pool := startLifecyclePostgreSQL(t, ctx)
	valkeyAddress := startLifecycleValkey(t, ctx)
	passwords := account.NewPasswordHasher(rand.Reader)
	password := "integration login password"
	passwordHash, err := passwords.Hash(password)
	if err != nil {
		t.Fatalf("Hash() error = %v", err)
	}
	now := time.Now().UTC().Truncate(time.Millisecond)
	adminID, playerID := snowflake.NewTestID(), snowflake.NewTestID()
	insertLifecycleAccounts(t, ctx, pool, adminID, playerID, passwordHash, now)

	adminSessions := sessionstore.New(sessionstore.Config{Address: valkeyAddress, Prefix: "avalon:e2e:session", Domain: "admin"})
	t.Cleanup(func() { _ = adminSessions.Close() })
	adminAdapters := adminpersistence.NewAuthenticationAdapters(pool, adminSessions)
	t.Run("admin", func(t *testing.T) {
		runLoginLifecycle(t, ctx, adminAdapters, adminSessions, passwords, password, "AdminE2E")
	})

	playerSessions := sessionstore.New(sessionstore.Config{Address: valkeyAddress, Prefix: "avalon:e2e:session", Domain: "player"})
	t.Cleanup(func() { _ = playerSessions.Close() })
	playerAdapters := securitypersistence.NewAuthenticationAdapters(pool, playerSessions)
	t.Run("player", func(t *testing.T) {
		runLoginLifecycle(t, ctx, playerAdapters, playerSessions, passwords, password, "PlayerE2E")
	})
}

func runLoginLifecycle(t *testing.T, ctx context.Context, adapters loginLifecycleAdapters, sessions authentication.LoginSessionStore, passwords *account.PasswordHasher, password, username string) {
	t.Helper()
	tokens := session.NewTokenIssuer(session.TokenPurposeSession, rand.Reader)
	policy := authentication.SessionPolicy{AbsoluteTTL: time.Hour, IdleTTL: 10 * time.Minute}
	login := authentication.NewService(
		adapters, adapters, sessions, nil, passwords, tokens, policy,
		authentication.LoginProtectionPolicy{LockThreshold: 5, BaseLock: time.Minute, MaximumLock: 15 * time.Minute},
		snowflake.NewTestID, time.Now,
	)
	refresh := authentication.NewRefreshService(adapters, tokens, policy.IdleTTL, snowflake.NewTestID, time.Now)
	logout := authentication.NewLogoutService(adapters, time.Now)

	first, err := login.Login(ctx, authentication.LoginCommand{Username: username, Password: password, RequestID: username + "-login-1", DeviceSummary: "integration-web"})
	if err != nil {
		t.Fatalf("Login(first) error = %v", err)
	}
	rotated, err := refresh.Refresh(ctx, first.SessionToken)
	if err != nil {
		t.Fatalf("Refresh() error = %v", err)
	}
	if _, err := refresh.Refresh(ctx, first.SessionToken); err != authentication.ErrRefreshReplay {
		t.Fatalf("Refresh(replay) error = %v", err)
	}
	if _, err := adapters.AuthenticateSession(ctx, tokens.Digest(rotated.RefreshToken), time.Now().UTC()); err != authentication.ErrSessionNotFound {
		t.Fatalf("重放撤销后的 AuthenticateSession() error = %v", err)
	}

	second, err := login.Login(ctx, authentication.LoginCommand{Username: username, Password: password, RequestID: username + "-login-2", DeviceSummary: "integration-web"})
	if err != nil {
		t.Fatalf("Login(second) error = %v", err)
	}
	principal, err := adapters.AuthenticateSession(ctx, tokens.Digest(second.SessionToken), time.Now().UTC())
	if err != nil {
		t.Fatalf("AuthenticateSession() error = %v", err)
	}
	if err := logout.Logout(ctx, principal); err != nil {
		t.Fatalf("Logout() error = %v", err)
	}
	if _, err := adapters.AuthenticateSession(ctx, tokens.Digest(second.SessionToken), time.Now().UTC()); !errors.Is(err, authentication.ErrSessionNotFound) {
		t.Fatalf("退出后的 AuthenticateSession() error = %v", err)
	}
}

func startLifecyclePostgreSQL(t *testing.T, ctx context.Context) *database.Pool {
	t.Helper()
	container, err := postgres.Run(ctx, lifecyclePostgresImage,
		postgres.WithDatabase("avalon_login_lifecycle_test"), postgres.WithUsername("avalon"),
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
	if _, err := pool.Exec(ctx, `INSERT INTO audit_hash_chain_state (id, ledger, latest_hash, updated_at) VALUES ($1, 'admin_audit_log', ''::bytea, now()), ($2, 'administration_audit_log', ''::bytea, now())`, snowflake.NewTestID(), snowflake.NewTestID()); err != nil {
		t.Fatalf("创建审计链尾: %v", err)
	}
	return pool
}

func startLifecycleValkey(t *testing.T, ctx context.Context) string {
	t.Helper()
	container, err := testcontainers.GenericContainer(ctx, testcontainers.GenericContainerRequest{
		ContainerRequest: testcontainers.ContainerRequest{
			Image: lifecycleValkeyImage, ExposedPorts: []string{"6379/tcp"},
			WaitingFor: wait.ForLog("Ready to accept connections").WithStartupTimeout(time.Minute),
		}, Started: true,
	})
	if err != nil {
		t.Fatalf("启动 Valkey: %v", err)
	}
	t.Cleanup(func() { _ = container.Terminate(context.Background()) })
	host, err := container.Host(ctx)
	if err != nil {
		t.Fatalf("读取 Valkey Host: %v", err)
	}
	port, err := container.MappedPort(ctx, "6379/tcp")
	if err != nil {
		t.Fatalf("读取 Valkey Port: %v", err)
	}
	return net.JoinHostPort(host, port.Port())
}

func insertLifecycleAccounts(t *testing.T, ctx context.Context, pool *database.Pool, adminID, playerID snowflake.ID, passwordHash string, now time.Time) {
	t.Helper()
	if _, err := pool.Exec(ctx, `INSERT INTO admin_account (id, username, username_key, display_name, password_hash, password_algorithm, password_parameters, status, failed_login_attempts, created_at, updated_at) VALUES ($1, 'AdminE2E', 'admine2e', 'Admin E2E', $2, 'argon2id', '{}'::jsonb, 'active', 0, $3, $3)`, adminID, passwordHash, now); err != nil {
		t.Fatalf("创建管理员账号: %v", err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO account (id, username, username_key, display_name, password_hash, password_algorithm, password_parameters, status, security_version, failed_login_attempts, created_at, updated_at) VALUES ($1, 'PlayerE2E', 'playere2e', 'Player E2E', $2, 'argon2id', '{}'::jsonb, 'active', 1, 0, $3, $3)`, playerID, passwordHash, now); err != nil {
		t.Fatalf("创建玩家账号: %v", err)
	}
}
