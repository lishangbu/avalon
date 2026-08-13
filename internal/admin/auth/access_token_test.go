package auth_test

import (
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	adminauth "github.com/lishangbu/avalon/internal/admin/auth"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// TestAccessTokenRoundTrip 验证 Ed25519 token 只恢复最小管理员身份且过期后拒绝。
func TestAccessTokenRoundTrip(t *testing.T) {
	now := time.Date(2026, 8, 9, 10, 0, 0, 0, time.UTC)
	clock := now
	issuer, err := adminauth.NewEphemeralAccessTokenIssuer(10*time.Minute, func() time.Time { return clock })
	if err != nil {
		t.Fatal(err)
	}
	want := authentication.Principal{AccountID: snowflake.NewTestID(), SessionID: snowflake.NewTestID(), SessionFamilyID: snowflake.NewTestID()}
	token, expiresAt, err := issuer.Issue(want)
	if err != nil {
		t.Fatal(err)
	}
	got, err := issuer.Verify(token)
	if err != nil {
		t.Fatal(err)
	}
	if got.AccountID != want.AccountID || got.SessionID != want.SessionID || got.SessionFamilyID != want.SessionFamilyID {
		t.Fatalf("身份不一致：got=%+v want=%+v", got, want)
	}
	if !expiresAt.Equal(now.Add(10 * time.Minute)) {
		t.Fatalf("失效时间 = %v", expiresAt)
	}
	clock = expiresAt
	if _, err := issuer.Verify(token); err == nil {
		t.Fatal("过期 access token 不应通过验证")
	}
	key := issuer.JWK()
	if key.KeyType != "OKP" || key.Curve != "Ed25519" || key.Algorithm != "EdDSA" || key.X == "" {
		t.Fatalf("JWK 无效：%+v", key)
	}
}

// TestAccessTokenIssueRejectsMissingIdentifiers 验证签发器不会生成缺少管理员、会话或会话族
// Identifier 的 Bearer access token。
func TestAccessTokenIssueRejectsMissingIdentifiers(t *testing.T) {
	t.Parallel()

	issuer, err := adminauth.NewEphemeralAccessTokenIssuer(10*time.Minute, func() time.Time {
		return time.Date(2026, 8, 9, 10, 0, 0, 0, time.UTC)
	})
	if err != nil {
		t.Fatal(err)
	}
	validID := snowflake.MustParse("1048576214")
	for _, principal := range []authentication.Principal{
		{SessionID: validID, SessionFamilyID: validID},
		{AccountID: validID, SessionFamilyID: validID},
		{AccountID: validID, SessionID: validID},
	} {
		if _, _, err := issuer.Issue(principal); err == nil {
			t.Fatalf("缺少 Identifier 的身份 %+v 不应签发 access token", principal)
		}
	}
}
