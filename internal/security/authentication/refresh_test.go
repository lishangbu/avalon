package authentication_test

import (
	"context"
	"crypto/rand"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/security/authentication"
	"github.com/lishangbu/avalon/internal/security/session"
)

type refreshStoreStub struct {
	principal authentication.Principal
	expiresAt time.Time
	digest    []byte
}

func (s *refreshStoreStub) RotateRefreshSession(_ context.Context, digest, nextDigest []byte, nextID snowflake.ID, _ time.Time, _ time.Duration) (authentication.Principal, time.Time, error) {
	if string(digest) != string(s.digest) {
		return authentication.Principal{}, time.Time{}, authentication.ErrRefreshReplay
	}
	s.digest = nextDigest
	s.principal.SessionID = nextID
	return s.principal, s.expiresAt, nil
}

// TestRefreshServiceRotatesOnlyOnce 验证 refresh service 不复用旧摘要并把下一枚明文只返回一次。
func TestRefreshServiceRotatesOnlyOnce(t *testing.T) {
	tokens := session.NewTokenIssuer(session.TokenPurposeSession, rand.Reader)
	current, err := tokens.Issue()
	if err != nil {
		t.Fatal(err)
	}
	store := &refreshStoreStub{principal: authentication.Principal{AccountID: snowflake.NewTestID()}, expiresAt: time.Now().Add(time.Hour), digest: current.Digest}
	service := authentication.NewRefreshService(store, tokens, time.Minute, snowflake.NewTestID, time.Now)
	result, err := service.Refresh(context.Background(), current.Plaintext)
	if err != nil {
		t.Fatal(err)
	}
	if result.RefreshToken == current.Plaintext || result.Principal.SessionID == snowflake.ID(0) {
		t.Fatal("refresh token 未轮换")
	}
	if _, err := service.Refresh(context.Background(), current.Plaintext); err == nil {
		t.Fatal("旧 refresh token 重放应失败")
	}
}
