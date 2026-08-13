package authentication_test

import (
	"bytes"
	"context"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/security/authentication"
	"github.com/lishangbu/avalon/internal/security/session"
)

func TestSessionAuthenticatorReturnsPrincipalWithoutPersistingPlaintextToken(t *testing.T) {
	t.Parallel()

	keys := session.NewTokenIssuer(session.TokenPurposeSession, bytes.NewReader(bytes.Repeat([]byte{0x22}, 32)))
	issued, err := keys.Issue()
	if err != nil {
		t.Fatalf("Issue() error = %v", err)
	}
	want := authentication.Principal{
		AccountID:       snowflake.MustParse("1048576204"),
		SessionID:       snowflake.MustParse("1048576205"),
		SessionFamilyID: snowflake.MustParse("1048576206"),
		SecurityVersion: 7,
	}
	store := &recordingSessionAuthenticationStore{principal: want}
	authenticator := authentication.NewSessionAuthenticator(
		store,
		keys,
		0,
		0,
		func() time.Time {
			return time.Unix(60, 0).UTC()
		},
	)

	got, err := authenticator.Authenticate(context.Background(), issued.Plaintext)
	if err != nil {
		t.Fatalf("Authenticate() error = %v", err)
	}
	if got != want {
		t.Errorf("Authenticate() = %+v, want %+v", got, want)
	}
	if len(store.digest) != 32 || bytes.Contains(store.digest, []byte(issued.Plaintext)) {
		t.Fatalf("会话查询摘要无效：%x", store.digest)
	}
}

func TestSessionAuthenticatorHonorsRequestDeadline(t *testing.T) {
	t.Parallel()

	authenticator := authentication.NewSessionAuthenticator(
		blockingSessionAuthenticationStore{},
		session.NewTokenIssuer(session.TokenPurposeSession, nil),
		0,
		0,
		time.Now,
	)
	ctx, cancel := context.WithTimeout(context.Background(), 20*time.Millisecond)
	defer cancel()

	_, err := authenticator.Authenticate(ctx, "access-token")

	if !errors.Is(err, context.DeadlineExceeded) {
		t.Fatalf("Authenticate() error = %v", err)
	}
}

type recordingSessionAuthenticationStore struct {
	principal authentication.Principal
	digest    []byte
}

func (s *recordingSessionAuthenticationStore) AuthenticateSession(
	_ context.Context,
	digest []byte,
	_ time.Time,
) (authentication.Principal, error) {
	s.digest = digest
	return s.principal, nil
}

type blockingSessionAuthenticationStore struct{}

func (blockingSessionAuthenticationStore) AuthenticateSession(
	ctx context.Context,
	_ []byte,
	_ time.Time,
) (authentication.Principal, error) {
	<-ctx.Done()
	return authentication.Principal{}, ctx.Err()
}
