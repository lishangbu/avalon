package authentication_test

import (
	"bytes"
	"context"
	"errors"
	"strings"
	"testing"
	"time"

	"github.com/alicebob/miniredis/v2"
	"github.com/lishangbu/avalon/internal/platform/sessionstore"
	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/security/account"
	"github.com/lishangbu/avalon/internal/security/authentication"
	"github.com/lishangbu/avalon/internal/security/session"
)

func TestServiceCreatesRevocableSessionForValidCredentials(t *testing.T) {
	t.Parallel()

	passwords := account.NewPasswordHasher(bytes.NewReader(bytes.Repeat([]byte{0x11}, 16)))
	passwordHash, err := passwords.Hash("a sufficiently long login password")
	if err != nil {
		t.Fatalf("Hash() error = %v", err)
	}
	accountID := snowflake.NewTestID()
	adapters := &recordingAuthenticationAdapters{account: authentication.LoginAccount{
		ID:              accountID,
		PasswordHash:    passwordHash,
		Status:          account.StatusActive,
		SecurityVersion: 3,
	}, committedSecurityVersion: 4}
	redisServer := miniredis.RunT(t)
	sessions := sessionstore.New(sessionstore.Config{Address: redisServer.Addr(), Domain: "player"})
	t.Cleanup(func() { _ = sessions.Close() })
	nowValue := time.Now().UTC().Truncate(time.Millisecond)
	service := authentication.NewService(
		adapters, adapters, sessions, nil,
		passwords,
		session.NewTokenIssuer(session.TokenPurposeSession, bytes.NewReader(bytes.Repeat([]byte{0x55}, 32))),
		authentication.SessionPolicy{
			AbsoluteTTL: 30 * 24 * time.Hour,
			IdleTTL:     7 * 24 * time.Hour,
		},
		authentication.LoginProtectionPolicy{
			LockThreshold: 5,
			BaseLock:      time.Minute,
			MaximumLock:   15 * time.Minute,
		},
		snowflake.NewTestID,
		func() time.Time { return nowValue })

	result, err := service.Login(context.Background(), authentication.LoginCommand{
		Username: "Admin",
		Password: "a sufficiently long login password",
	})
	if err != nil {
		t.Fatalf("Login() error = %v", err)
	}
	if result.SessionToken == "" {
		t.Fatal("Login() returned an empty token")
	}
	if result.ExpiresAt != nowValue.Add(30*24*time.Hour) {
		t.Errorf("ExpiresAt = %v", result.ExpiresAt)
	}
	if adapters.committed.Session.AccountID != accountID || result.SecurityVersion != 4 {
		t.Errorf("committed login = %+v, result = %+v", adapters.committed, result)
	}
	createdIDs := []snowflake.ID{adapters.committed.Session.ID, adapters.committed.Session.FamilyID, adapters.committed.LoginAttemptID, adapters.committed.AuditID}
	seen := make(map[snowflake.ID]struct{}, len(createdIDs))
	for _, id := range createdIDs {
		if !id.IsValid() {
			t.Fatalf("成功登录生成了无效 Identifier: %+v", adapters.committed)
		}
		seen[id] = struct{}{}
	}
	if len(seen) != len(createdIDs) {
		t.Fatalf("成功登录复用了持久事实 Identifier: %+v", adapters.committed)
	}
	if bytes.Contains(adapters.committed.Session.SessionTokenDigest, []byte(result.SessionToken)) {
		t.Fatal("stored session digest contains plaintext token")
	}
	principal, err := sessions.AuthenticateSession(context.Background(), adapters.committed.Session.SessionTokenDigest, nowValue)
	if err != nil || principal.SecurityVersion != 4 {
		t.Fatalf("AuthenticateSession() = %+v, %v", principal, err)
	}
	_, err = service.Login(context.Background(), authentication.LoginCommand{
		Username:  "Admin",
		Password:  "wrong password",
		RequestID: "wrong-password-request",
	})
	if !errors.Is(err, authentication.ErrInvalidCredentials) {
		t.Fatalf("Login(wrong password) error = %v", err)
	}
	if adapters.commitCalls != 1 {
		t.Errorf("CommitLoginSuccess() calls = %d", adapters.commitCalls)
	}
	if len(adapters.failures) != 1 || adapters.failures[0].Reason != authentication.LoginFailureInvalidPassword ||
		adapters.failures[0].AccountID != accountID || adapters.failures[0].RequestID != "wrong-password-request" {
		t.Fatalf("login failures = %+v", adapters.failures)
	}
	if adapters.failures[0].LoginAttemptID == adapters.failures[0].AuditID ||
		!adapters.failures[0].LoginAttemptID.IsValid() || !adapters.failures[0].AuditID.IsValid() {
		t.Fatalf("失败登录复用或缺少持久事实 Identifier: %+v", adapters.failures[0])
	}
	_, err = service.Login(context.Background(), authentication.LoginCommand{
		Username:  "Admin",
		Password:  strings.Repeat("a", 1025),
		RequestID: "oversized-password-request",
	})
	if !errors.Is(err, authentication.ErrInvalidCredentials) {
		t.Fatalf("Login(oversized password) error = %v, want ErrInvalidCredentials", err)
	}
	if len(adapters.failures) != 2 || adapters.failures[1].Reason != authentication.LoginFailureInvalidPassword {
		t.Fatalf("login failures after oversized password = %+v", adapters.failures)
	}
}

func TestLoginProtectionPolicyIncreasesLockDurationWithinMaximum(t *testing.T) {
	t.Parallel()

	policy := authentication.LoginProtectionPolicy{
		LockThreshold: 5,
		BaseLock:      time.Minute,
		MaximumLock:   15 * time.Minute,
	}
	tests := []struct {
		failedAttempts int32
		want           time.Duration
	}{
		{failedAttempts: 4, want: 0},
		{failedAttempts: 5, want: time.Minute},
		{failedAttempts: 6, want: 2 * time.Minute},
		{failedAttempts: 7, want: 4 * time.Minute},
		{failedAttempts: 8, want: 8 * time.Minute},
		{failedAttempts: 9, want: 15 * time.Minute},
		{failedAttempts: 30, want: 15 * time.Minute},
	}
	for _, test := range tests {
		if got := policy.LockDuration(test.failedAttempts); got != test.want {
			t.Errorf("LockDuration(%d) = %v, want %v", test.failedAttempts, got, test.want)
		}
	}
}

func TestServiceAbortsStagedSessionWhenLoginCommitFails(t *testing.T) {
	t.Parallel()

	redisServer := miniredis.RunT(t)
	sessions := sessionstore.New(sessionstore.Config{Address: redisServer.Addr(), Domain: "player"})
	t.Cleanup(func() { _ = sessions.Close() })
	commitErr := errors.New("database commit failed")
	service, adapters := newLoginTestService(t, sessions, commitErr)

	_, err := service.Login(context.Background(), authentication.LoginCommand{
		Username: "Admin", Password: "a sufficiently long login password", RequestID: "commit-failure",
	})
	if !errors.Is(err, commitErr) {
		t.Fatalf("Login() error = %v, want commit error", err)
	}
	if _, authErr := sessions.AuthenticateSession(context.Background(), adapters.committed.Session.SessionTokenDigest, time.Now().UTC()); authErr != authentication.ErrSessionNotFound {
		t.Fatalf("AuthenticateSession() error = %v, want ErrSessionNotFound", authErr)
	}
}

func TestServiceDoesNotCommitWhenSessionStageFails(t *testing.T) {
	t.Parallel()

	stageErr := errors.New("valkey unavailable")
	sessions := &failingLoginSessionStore{stageErr: stageErr}
	service, adapters := newLoginTestService(t, sessions, nil)

	_, err := service.Login(context.Background(), authentication.LoginCommand{
		Username: "Admin", Password: "a sufficiently long login password", RequestID: "stage-failure",
	})
	if !errors.Is(err, stageErr) {
		t.Fatalf("Login() error = %v, want stage error", err)
	}
	if adapters.commitCalls != 0 {
		t.Fatalf("CommitLoginSuccess() calls = %d, want 0", adapters.commitCalls)
	}
	if sessions.abortCalls != 1 {
		t.Fatalf("AbortSession() calls = %d, want 1", sessions.abortCalls)
	}
}

func TestServicePreservesStageAndCompensationErrors(t *testing.T) {
	t.Parallel()

	stageErr := errors.New("stage response lost")
	abortErr := errors.New("valkey still unavailable")
	sessions := &failingLoginSessionStore{stageErr: stageErr, abortErr: abortErr}
	telemetry := &recordingLoginSessionTelemetry{}
	service, _ := newLoginTestService(t, sessions, nil, telemetry)

	_, err := service.Login(context.Background(), authentication.LoginCommand{
		Username: "Admin", Password: "a sufficiently long login password", RequestID: "compensation-failure",
	})
	if !errors.Is(err, stageErr) || !errors.Is(err, abortErr) {
		t.Fatalf("Login() error = %v, want stage and abort errors", err)
	}
	if len(telemetry.failures) != 2 || telemetry.failures[0].stage != authentication.LoginSessionStageStage || telemetry.failures[1].stage != authentication.LoginSessionStageAbort {
		t.Fatalf("登录会话失败阶段 = %+v", telemetry.failures)
	}
	if telemetry.failures[0].requestID != "compensation-failure" || !errors.Is(telemetry.failures[0].cause, stageErr) || !errors.Is(telemetry.failures[1].cause, abortErr) {
		t.Fatalf("登录会话失败上下文 = %+v", telemetry.failures)
	}
}

func TestServiceCompensatesAfterRequestContextIsCanceled(t *testing.T) {
	t.Parallel()

	stageErr := errors.New("stage response lost")
	sessions := &cancelingStageStore{stageErr: stageErr}
	service, _ := newLoginTestService(t, sessions, nil)
	ctx, cancel := context.WithCancel(context.Background())
	sessions.cancel = cancel

	_, err := service.Login(ctx, authentication.LoginCommand{
		Username: "Admin", Password: "a sufficiently long login password", RequestID: "canceled-request",
	})
	if !errors.Is(err, stageErr) {
		t.Fatalf("Login() error = %v, want stage error", err)
	}
	if sessions.abortContextErr != nil {
		t.Fatalf("AbortSession() context error = %v", sessions.abortContextErr)
	}
}

func TestPendingSessionExpiresWhenCompensationCannotReachValkey(t *testing.T) {
	t.Parallel()

	redisServer := miniredis.RunT(t)
	base := sessionstore.New(sessionstore.Config{Address: redisServer.Addr(), Domain: "player"})
	t.Cleanup(func() { _ = base.Close() })
	stageErr := errors.New("stage response lost")
	abortErr := errors.New("abort response lost")
	sessions := &stageAndAbortResponseLostStore{Store: base, stageErr: stageErr, abortErr: abortErr}
	service, _ := newLoginTestService(t, sessions, nil)

	_, err := service.Login(context.Background(), authentication.LoginCommand{
		Username: "Admin", Password: "a sufficiently long login password", RequestID: "ttl-cleanup",
	})
	if !errors.Is(err, stageErr) || !errors.Is(err, abortErr) {
		t.Fatalf("Login() error = %v, want stage and abort errors", err)
	}
	redisServer.FastForward(2 * time.Minute)
	if err := base.ActivateSession(context.Background(), sessions.digest, 4); err != authentication.ErrSessionNotFound {
		t.Fatalf("ActivateSession() after pending TTL error = %v", err)
	}
}

func TestServiceAbortsSessionWhenStageResponseIsLost(t *testing.T) {
	t.Parallel()

	redisServer := miniredis.RunT(t)
	base := sessionstore.New(sessionstore.Config{Address: redisServer.Addr(), Domain: "admin"})
	t.Cleanup(func() { _ = base.Close() })
	stageErr := errors.New("stage response lost")
	sessions := &stageResponseLostStore{Store: base, err: stageErr}
	service, adapters := newLoginTestService(t, sessions, nil)

	_, err := service.Login(context.Background(), authentication.LoginCommand{
		Username: "Admin", Password: "a sufficiently long login password", RequestID: "stage-response-lost",
	})
	if !errors.Is(err, stageErr) {
		t.Fatalf("Login() error = %v, want stage error", err)
	}
	if adapters.commitCalls != 0 {
		t.Fatalf("CommitLoginSuccess() calls = %d, want 0", adapters.commitCalls)
	}
	if _, authErr := base.AuthenticateSession(context.Background(), sessions.digest, time.Now().UTC()); authErr != authentication.ErrSessionNotFound {
		t.Fatalf("AuthenticateSession() error = %v, want ErrSessionNotFound", authErr)
	}
}

func TestServiceAbortsSessionWhenActivationResponseIsLost(t *testing.T) {
	t.Parallel()

	redisServer := miniredis.RunT(t)
	base := sessionstore.New(sessionstore.Config{Address: redisServer.Addr(), Domain: "player"})
	t.Cleanup(func() { _ = base.Close() })
	activationErr := errors.New("activation response lost")
	sessions := &activationResponseLostStore{Store: base, err: activationErr}
	service, adapters := newLoginTestService(t, sessions, nil)

	_, err := service.Login(context.Background(), authentication.LoginCommand{
		Username: "Admin", Password: "a sufficiently long login password", RequestID: "activation-response-lost",
	})
	if !errors.Is(err, activationErr) {
		t.Fatalf("Login() error = %v, want activation error", err)
	}
	if _, authErr := base.AuthenticateSession(context.Background(), adapters.committed.Session.SessionTokenDigest, time.Now().UTC()); authErr != authentication.ErrSessionNotFound {
		t.Fatalf("AuthenticateSession() error = %v, want ErrSessionNotFound", authErr)
	}
}

func newLoginTestService(
	t *testing.T,
	sessions authentication.LoginSessionStore,
	commitErr error,
	telemetry ...authentication.LoginSessionTelemetry,
) (*authentication.Service, *recordingAuthenticationAdapters) {
	t.Helper()
	passwords := account.NewPasswordHasher(bytes.NewReader(bytes.Repeat([]byte{0x21}, 16)))
	passwordHash, err := passwords.Hash("a sufficiently long login password")
	if err != nil {
		t.Fatalf("Hash() error = %v", err)
	}
	adapters := &recordingAuthenticationAdapters{
		account: authentication.LoginAccount{
			ID: snowflake.NewTestID(), PasswordHash: passwordHash, Status: account.StatusActive, SecurityVersion: 3,
		},
		committedSecurityVersion: 4,
		commitErr:                commitErr,
	}
	service := authentication.NewService(
		adapters, adapters, sessions, firstLoginSessionTelemetry(telemetry), passwords,
		session.NewTokenIssuer(session.TokenPurposeSession, bytes.NewReader(bytes.Repeat([]byte{0x65}, 32))),
		authentication.SessionPolicy{AbsoluteTTL: time.Hour, IdleTTL: time.Minute},
		authentication.LoginProtectionPolicy{LockThreshold: 5, BaseLock: time.Minute, MaximumLock: 15 * time.Minute},
		snowflake.NewTestID, time.Now,
	)
	return service, adapters
}

func firstLoginSessionTelemetry(values []authentication.LoginSessionTelemetry) authentication.LoginSessionTelemetry {
	if len(values) == 0 {
		return nil
	}
	return values[0]
}

type recordingLoginSessionTelemetry struct {
	failures []loginSessionFailure
}

type loginSessionFailure struct {
	stage     authentication.LoginSessionStage
	requestID string
	cause     error
}

func (s *recordingLoginSessionTelemetry) RecordFailure(_ context.Context, stage authentication.LoginSessionStage, requestID string, cause error) {
	s.failures = append(s.failures, loginSessionFailure{stage: stage, requestID: requestID, cause: cause})
}

type stageResponseLostStore struct {
	*sessionstore.Store
	err    error
	digest []byte
}

func (s *stageResponseLostStore) StageSession(ctx context.Context, record authentication.SessionRecord) error {
	s.digest = bytes.Clone(record.SessionTokenDigest)
	if err := s.Store.StageSession(ctx, record); err != nil {
		return err
	}
	return s.err
}

type activationResponseLostStore struct {
	*sessionstore.Store
	err error
}

type failingLoginSessionStore struct {
	stageErr   error
	abortErr   error
	abortCalls int
}

type cancelingStageStore struct {
	cancel          context.CancelFunc
	stageErr        error
	abortContextErr error
}

func (s *cancelingStageStore) StageSession(context.Context, authentication.SessionRecord) error {
	s.cancel()
	return s.stageErr
}

func (s *cancelingStageStore) ActivateSession(context.Context, []byte, int64) error {
	return nil
}

func (s *cancelingStageStore) AbortSession(ctx context.Context, _ []byte) error {
	s.abortContextErr = ctx.Err()
	return nil
}

func (s *failingLoginSessionStore) StageSession(context.Context, authentication.SessionRecord) error {
	return s.stageErr
}

func (s *failingLoginSessionStore) ActivateSession(context.Context, []byte, int64) error {
	return nil
}

func (s *failingLoginSessionStore) AbortSession(context.Context, []byte) error {
	s.abortCalls++
	return s.abortErr
}

type stageAndAbortResponseLostStore struct {
	*sessionstore.Store
	stageErr error
	abortErr error
	digest   []byte
}

func (s *stageAndAbortResponseLostStore) StageSession(ctx context.Context, record authentication.SessionRecord) error {
	s.digest = bytes.Clone(record.SessionTokenDigest)
	if err := s.Store.StageSession(ctx, record); err != nil {
		return err
	}
	return s.stageErr
}

func (s *stageAndAbortResponseLostStore) AbortSession(context.Context, []byte) error {
	return s.abortErr
}

func (s *activationResponseLostStore) ActivateSession(ctx context.Context, digest []byte, securityVersion int64) error {
	if err := s.Store.ActivateSession(ctx, digest, securityVersion); err != nil {
		return err
	}
	return s.err
}

type recordingAuthenticationAdapters struct {
	account                  authentication.LoginAccount
	committed                authentication.LoginSuccessRecord
	commitCalls              int
	committedSecurityVersion int64
	commitErr                error
	failures                 []authentication.LoginFailureRecord
}

func (s *recordingAuthenticationAdapters) RecordLoginFailure(
	_ context.Context,
	record authentication.LoginFailureRecord,
) error {
	s.failures = append(s.failures, record)
	return nil
}

func (s *recordingAuthenticationAdapters) FindLoginAccount(
	context.Context,
	string,
) (authentication.LoginAccount, error) {
	return s.account, nil
}

func (s *recordingAuthenticationAdapters) CommitLoginSuccess(
	_ context.Context,
	record authentication.LoginSuccessRecord,
) (int64, error) {
	s.commitCalls++
	s.committed = record
	return s.committedSecurityVersion, s.commitErr
}
