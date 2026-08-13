package sessionstore_test

import (
	"context"
	"testing"
	"time"

	"github.com/alicebob/miniredis/v2"
	"github.com/lishangbu/avalon/internal/platform/sessionstore"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

func TestStoreRotatesAndRevokesRefreshFamily(t *testing.T) {
	redisServer := miniredis.RunT(t)
	store := sessionstore.New(sessionstore.Config{Address: redisServer.Addr(), Domain: "admin"})
	t.Cleanup(func() { _ = store.Close() })
	ctx := context.Background()
	now := time.Now().UTC().Truncate(time.Microsecond)
	familyID, accountID, sessionID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	oldDigest := []byte("old-refresh-digest-32-bytes-000000")
	nextDigest := []byte("next-refresh-digest-32-bytes-000000")
	record := authentication.SessionRecord{ID: sessionID, FamilyID: familyID, AccountID: accountID, SessionTokenDigest: oldDigest, SecurityVersion: 1, ExpiresAt: now.Add(time.Hour), IdleExpiresAt: now.Add(time.Minute), LastActivityAt: now, CreatedAt: now, DeviceSummary: "web"}
	if err := store.CreateSession(ctx, record); err != nil {
		t.Fatalf("CreateSession() error = %v", err)
	}
	principal, err := store.AuthenticateSession(ctx, oldDigest, now)
	if err != nil || principal.SessionID != sessionID {
		t.Fatalf("AuthenticateSession() = %+v, %v", principal, err)
	}
	nextID := snowflake.NewTestID()
	rotated, expires, err := store.RotateRefreshSession(ctx, oldDigest, nextDigest, nextID, now, time.Minute)
	if err != nil {
		t.Fatalf("RotateRefreshSession() error = %v", err)
	}
	if rotated.SessionID != nextID || expires.Sub(record.ExpiresAt).Abs() > time.Millisecond {
		t.Fatalf("rotated = %+v expires=%v", rotated, expires)
	}
	if _, _, err := store.RotateRefreshSession(ctx, oldDigest, []byte("replayed-refresh-digest-000000000"), snowflake.NewTestID(), now, time.Minute); err != authentication.ErrRefreshReplay {
		t.Fatalf("replay error = %v", err)
	}
	if _, err := store.AuthenticateSession(ctx, nextDigest, now); err != authentication.ErrSessionNotFound {
		t.Fatalf("family should be revoked, error = %v", err)
	}
}
