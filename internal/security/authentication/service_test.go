package authentication_test

import (
	"bytes"
	"context"
	"errors"
	"strings"
	"testing"
	"time"

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
	repository := &recordingAuthenticationRepository{account: authentication.LoginAccount{
		ID:              accountID,
		PasswordHash:    passwordHash,
		Status:          account.StatusActive,
		SecurityVersion: 3,
	}}
	nowValue := time.Unix(59, 0).UTC()
	service := authentication.NewService(
		repository,
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
		func() time.Time { return nowValue },
	)

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
	if repository.created.AccountID != accountID || repository.created.SecurityVersion != 3 {
		t.Errorf("created session = %+v", repository.created)
	}
	createdIDs := []snowflake.ID{repository.created.ID, repository.created.FamilyID, repository.created.LoginAttemptID, repository.created.AuditID}
	seen := make(map[snowflake.ID]struct{}, len(createdIDs))
	for _, id := range createdIDs {
		if !id.IsValid() {
			t.Fatalf("成功登录生成了无效 Identifier: %+v", repository.created)
		}
		seen[id] = struct{}{}
	}
	if len(seen) != len(createdIDs) {
		t.Fatalf("成功登录复用了持久事实 Identifier: %+v", repository.created)
	}
	if bytes.Contains(repository.created.SessionTokenDigest, []byte(result.SessionToken)) {
		t.Fatal("stored session digest contains plaintext token")
	}
	_, err = service.Login(context.Background(), authentication.LoginCommand{
		Username:  "Admin",
		Password:  "wrong password",
		RequestID: "wrong-password-request",
	})
	if !errors.Is(err, authentication.ErrInvalidCredentials) {
		t.Fatalf("Login(wrong password) error = %v", err)
	}
	if repository.createCalls != 1 {
		t.Errorf("CreateSession() calls = %d", repository.createCalls)
	}
	if len(repository.failures) != 1 || repository.failures[0].Reason != authentication.LoginFailureInvalidPassword ||
		repository.failures[0].AccountID != accountID || repository.failures[0].RequestID != "wrong-password-request" {
		t.Fatalf("login failures = %+v", repository.failures)
	}
	if repository.failures[0].LoginAttemptID == repository.failures[0].AuditID ||
		!repository.failures[0].LoginAttemptID.IsValid() || !repository.failures[0].AuditID.IsValid() {
		t.Fatalf("失败登录复用或缺少持久事实 Identifier: %+v", repository.failures[0])
	}
	_, err = service.Login(context.Background(), authentication.LoginCommand{
		Username:  "Admin",
		Password:  strings.Repeat("a", 1025),
		RequestID: "oversized-password-request",
	})
	if !errors.Is(err, authentication.ErrInvalidCredentials) {
		t.Fatalf("Login(oversized password) error = %v, want ErrInvalidCredentials", err)
	}
	if len(repository.failures) != 2 || repository.failures[1].Reason != authentication.LoginFailureInvalidPassword {
		t.Fatalf("login failures after oversized password = %+v", repository.failures)
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

type recordingAuthenticationRepository struct {
	account     authentication.LoginAccount
	created     authentication.SessionRecord
	createCalls int
	failures    []authentication.LoginFailureRecord
}

func (s *recordingAuthenticationRepository) RecordLoginFailure(
	_ context.Context,
	record authentication.LoginFailureRecord,
) error {
	s.failures = append(s.failures, record)
	return nil
}

func (s *recordingAuthenticationRepository) FindLoginAccount(
	context.Context,
	string,
) (authentication.LoginAccount, error) {
	return s.account, nil
}

func (s *recordingAuthenticationRepository) CreateSession(
	_ context.Context,
	record authentication.SessionRecord,
) error {
	s.createCalls++
	s.created = record
	return nil
}
