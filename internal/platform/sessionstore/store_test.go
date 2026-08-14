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

func TestStoreRequiresActivationBeforeAuthentication(t *testing.T) {
	redisServer := miniredis.RunT(t)
	store := sessionstore.New(sessionstore.Config{Address: redisServer.Addr(), Domain: "admin"})
	t.Cleanup(func() { _ = store.Close() })
	ctx := context.Background()
	now := time.Now().UTC().Truncate(time.Microsecond)
	familyID, accountID, sessionID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	oldDigest := []byte("old-refresh-digest-32-bytes-000000")
	record := authentication.SessionRecord{ID: sessionID, FamilyID: familyID, AccountID: accountID, SessionTokenDigest: oldDigest, SecurityVersion: 1, ExpiresAt: now.Add(time.Hour), IdleExpiresAt: now.Add(time.Minute), LastActivityAt: now, CreatedAt: now, DeviceSummary: "web"}
	if err := store.StageSession(ctx, record); err != nil {
		t.Fatalf("StageSession() error = %v", err)
	}
	if _, err := store.AuthenticateSession(ctx, oldDigest, now); err != authentication.ErrSessionNotFound {
		t.Fatalf("pending AuthenticateSession() error = %v", err)
	}
	families, err := store.ListActiveSessionFamilies(ctx, accountID, now)
	if err != nil || len(families) != 0 {
		t.Fatalf("pending ListActiveSessionFamilies() = %+v, %v", families, err)
	}
	if err := store.ActivateSession(ctx, oldDigest, 7); err != nil {
		t.Fatalf("ActivateSession() error = %v", err)
	}
	if err := store.ActivateSession(ctx, oldDigest, 7); err != nil {
		t.Fatalf("ActivateSession() idempotent error = %v", err)
	}
	principal, err := store.AuthenticateSession(ctx, oldDigest, now)
	if err != nil || principal.SessionID != sessionID || principal.SecurityVersion != 7 {
		t.Fatalf("AuthenticateSession() = %+v, %v", principal, err)
	}
	families, err = store.ListActiveSessionFamilies(ctx, accountID, now)
	if err != nil || len(families) != 1 || families[0].FamilyID != familyID {
		t.Fatalf("active ListActiveSessionFamilies() = %+v, %v", families, err)
	}
}

func TestStoreRejectsDuplicateStagedSession(t *testing.T) {
	redisServer := miniredis.RunT(t)
	store := sessionstore.New(sessionstore.Config{Address: redisServer.Addr(), Domain: "admin"})
	t.Cleanup(func() { _ = store.Close() })
	ctx := context.Background()
	now := time.Now().UTC().Truncate(time.Microsecond)
	digest := []byte("duplicate-refresh-digest-32-bytes-00")
	record := authentication.SessionRecord{ID: snowflake.NewTestID(), FamilyID: snowflake.NewTestID(), AccountID: snowflake.NewTestID(), SessionTokenDigest: digest, SecurityVersion: 1, ExpiresAt: now.Add(time.Hour), IdleExpiresAt: now.Add(time.Minute), LastActivityAt: now, CreatedAt: now}
	if err := store.StageSession(ctx, record); err != nil {
		t.Fatalf("StageSession() error = %v", err)
	}
	if err := store.StageSession(ctx, record); err == nil {
		t.Fatal("duplicate StageSession() error = nil")
	}
}

func TestStoreAbortsStagedOrActivatedSession(t *testing.T) {
	redisServer := miniredis.RunT(t)
	store := sessionstore.New(sessionstore.Config{Address: redisServer.Addr(), Domain: "player"})
	t.Cleanup(func() { _ = store.Close() })
	ctx := context.Background()
	now := time.Now().UTC().Truncate(time.Microsecond)
	digest := []byte("abort-refresh-digest-32-bytes-00000")
	record := authentication.SessionRecord{ID: snowflake.NewTestID(), FamilyID: snowflake.NewTestID(), AccountID: snowflake.NewTestID(), SessionTokenDigest: digest, SecurityVersion: 1, ExpiresAt: now.Add(time.Hour), IdleExpiresAt: now.Add(time.Minute), LastActivityAt: now, CreatedAt: now}
	if err := store.StageSession(ctx, record); err != nil {
		t.Fatalf("StageSession() error = %v", err)
	}
	if err := store.ActivateSession(ctx, digest, 2); err != nil {
		t.Fatalf("ActivateSession() error = %v", err)
	}
	if err := store.AbortSession(ctx, digest); err != nil {
		t.Fatalf("AbortSession() error = %v", err)
	}
	if err := store.AbortSession(ctx, digest); err != nil {
		t.Fatalf("AbortSession() idempotent error = %v", err)
	}
	if _, err := store.AuthenticateSession(ctx, digest, now); err != authentication.ErrSessionNotFound {
		t.Fatalf("aborted AuthenticateSession() error = %v", err)
	}
}

func TestStoreExpiresUnactivatedSession(t *testing.T) {
	redisServer := miniredis.RunT(t)
	store := sessionstore.New(sessionstore.Config{Address: redisServer.Addr(), Domain: "admin"})
	t.Cleanup(func() { _ = store.Close() })
	ctx := context.Background()
	now := time.Now().UTC().Truncate(time.Microsecond)
	digest := []byte("expired-pending-digest-32-bytes-000")
	record := authentication.SessionRecord{ID: snowflake.NewTestID(), FamilyID: snowflake.NewTestID(), AccountID: snowflake.NewTestID(), SessionTokenDigest: digest, SecurityVersion: 1, ExpiresAt: now.Add(time.Hour), IdleExpiresAt: now.Add(time.Minute), LastActivityAt: now, CreatedAt: now}
	if err := store.StageSession(ctx, record); err != nil {
		t.Fatalf("StageSession() error = %v", err)
	}
	redisServer.FastForward(2 * time.Minute)
	if err := store.ActivateSession(ctx, digest, 1); err != authentication.ErrSessionNotFound {
		t.Fatalf("expired ActivateSession() error = %v", err)
	}
}

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
	if err := store.StageSession(ctx, record); err != nil {
		t.Fatalf("StageSession() error = %v", err)
	}
	if err := store.ActivateSession(ctx, oldDigest, record.SecurityVersion); err != nil {
		t.Fatalf("ActivateSession() error = %v", err)
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
