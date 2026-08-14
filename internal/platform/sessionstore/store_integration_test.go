//go:build integration

package sessionstore_test

import (
	"context"
	"encoding/hex"
	"net"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/sessionstore"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/security/authentication"
	"github.com/redis/go-redis/v9"
	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/wait"
)

const sessionStoreValkeyImage = "valkey/valkey@sha256:8e8d64b405ce18f41b8e5ee20aa4687a8ed0022d1298f2ce31cdcf3a76e09411"

func TestSessionStoreRunsStateMachineAgainstValkey(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	container, address := startSessionStoreValkey(t, ctx)
	store := sessionstore.New(sessionstore.Config{Address: address, Prefix: "avalon:integration:session", Domain: "player"})
	t.Cleanup(func() { _ = store.Close() })
	client := redis.NewClient(&redis.Options{Addr: address})
	t.Cleanup(func() { _ = client.Close() })
	now := time.Now().UTC().Truncate(time.Millisecond)
	digest := []byte("real-valkey-refresh-digest-000000")
	record := authentication.SessionRecord{
		ID: snowflake.NewTestID(), FamilyID: snowflake.NewTestID(), AccountID: snowflake.NewTestID(),
		SessionTokenDigest: digest, SecurityVersion: 3, DeviceSummary: "integration-web",
		CreatedAt: now, LastActivityAt: now, IdleExpiresAt: now.Add(time.Minute), ExpiresAt: now.Add(time.Hour),
	}
	if err := store.StageSession(ctx, record); err != nil {
		t.Fatalf("StageSession() error = %v", err)
	}
	tokenKey := store.Key("player", "token", hex.EncodeToString(digest))
	pendingTTL, err := client.PTTL(ctx, tokenKey).Result()
	if err != nil || pendingTTL < 55*time.Second || pendingTTL > time.Minute {
		t.Fatalf("pending PTTL = %v, error = %v", pendingTTL, err)
	}
	if _, err := store.AuthenticateSession(ctx, digest, now); err != authentication.ErrSessionNotFound {
		t.Fatalf("pending AuthenticateSession() error = %v", err)
	}
	if err := store.ActivateSession(ctx, digest, 4); err != nil {
		t.Fatalf("ActivateSession() error = %v", err)
	}
	principal, err := store.AuthenticateSession(ctx, digest, now)
	if err != nil || principal.SecurityVersion != 4 || principal.SessionID != record.ID {
		t.Fatalf("AuthenticateSession() = %+v, %v", principal, err)
	}
	nextDigest := []byte("real-valkey-next-refresh-000000000")
	if _, _, err := store.RotateRefreshSession(ctx, digest, nextDigest, snowflake.NewTestID(), now, time.Minute); err != nil {
		t.Fatalf("RotateRefreshSession() error = %v", err)
	}
	if _, _, err := store.RotateRefreshSession(ctx, digest, []byte("real-valkey-replay-refresh-000000"), snowflake.NewTestID(), now, time.Minute); err != authentication.ErrRefreshReplay {
		t.Fatalf("refresh replay error = %v", err)
	}
	if _, err := store.AuthenticateSession(ctx, nextDigest, now); err != authentication.ErrSessionNotFound {
		t.Fatalf("重放后会话 error = %v", err)
	}

	expiringDigest := []byte("real-valkey-expiring-pending-0000")
	expiring := record
	expiring.ID, expiring.FamilyID = snowflake.NewTestID(), snowflake.NewTestID()
	expiring.SessionTokenDigest = expiringDigest
	if err := store.StageSession(ctx, expiring); err != nil {
		t.Fatalf("StageSession(expiring) error = %v", err)
	}
	expiringKey := store.Key("player", "token", hex.EncodeToString(expiringDigest))
	if err := client.PExpire(ctx, expiringKey, 20*time.Millisecond).Err(); err != nil {
		t.Fatalf("缩短 pending TTL: %v", err)
	}
	time.Sleep(30 * time.Millisecond)
	if err := store.ActivateSession(ctx, expiringDigest, 4); err != authentication.ErrSessionNotFound {
		t.Fatalf("过期 pending ActivateSession() error = %v", err)
	}

	stopTimeout := 5 * time.Second
	if err := container.Stop(ctx, &stopTimeout); err != nil {
		t.Fatalf("停止 Valkey: %v", err)
	}
	if err := store.Ready(ctx); err == nil {
		t.Fatal("Valkey 停止后 Ready() error = nil")
	}
	failed := record
	failed.ID, failed.FamilyID = snowflake.NewTestID(), snowflake.NewTestID()
	failed.SessionTokenDigest = []byte("real-valkey-unavailable-digest-0000")
	if err := store.StageSession(ctx, failed); err == nil {
		t.Fatal("Valkey 停止后 StageSession() error = nil")
	}
}

func startSessionStoreValkey(t *testing.T, ctx context.Context) (testcontainers.Container, string) {
	t.Helper()
	container, err := testcontainers.GenericContainer(ctx, testcontainers.GenericContainerRequest{
		ContainerRequest: testcontainers.ContainerRequest{
			Image: sessionStoreValkeyImage, ExposedPorts: []string{"6379/tcp"},
			WaitingFor: wait.ForLog("Ready to accept connections").WithStartupTimeout(time.Minute),
		},
		Started: true,
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
	return container, net.JoinHostPort(host, port.Port())
}
