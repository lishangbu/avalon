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
	adapters := &recordingAuthenticationAdapters{account: authentication.LoginAccount{
		ID:              accountID,
		PasswordHash:    passwordHash,
		Status:          account.StatusActive,
		SecurityVersion: 3,
	}}
	nowValue := time.Unix(59, 0).UTC()
	service := authentication.NewService(
		adapters, adapters,
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
	if adapters.created.AccountID != accountID || adapters.created.SecurityVersion != 3 {
		t.Errorf("created session = %+v", adapters.created)
	}
	createdIDs := []snowflake.ID{adapters.created.ID, adapters.created.FamilyID, adapters.created.LoginAttemptID, adapters.created.AuditID}
	seen := make(map[snowflake.ID]struct{}, len(createdIDs))
	for _, id := range createdIDs {
		if !id.IsValid() {
			t.Fatalf("成功登录生成了无效 Identifier: %+v", adapters.created)
		}
		seen[id] = struct{}{}
	}
	if len(seen) != len(createdIDs) {
		t.Fatalf("成功登录复用了持久事实 Identifier: %+v", adapters.created)
	}
	if bytes.Contains(adapters.created.SessionTokenDigest, []byte(result.SessionToken)) {
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
	if adapters.createCalls != 1 {
		t.Errorf("CreateSession() calls = %d", adapters.createCalls)
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

type recordingAuthenticationAdapters struct {
	account     authentication.LoginAccount
	created     authentication.SessionRecord
	createCalls int
	failures    []authentication.LoginFailureRecord
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

func (s *recordingAuthenticationAdapters) CreateSession(
	_ context.Context,
	record authentication.SessionRecord,
) error {
	s.createCalls++
	s.created = record
	return nil
}
