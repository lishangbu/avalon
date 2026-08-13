package account_test

import (
	"bytes"
	"errors"
	"strings"
	"testing"

	"github.com/lishangbu/avalon/internal/security/account"
)

func TestPasswordHasherVerifiesOnlyTheOriginalPassword(t *testing.T) {
	t.Parallel()

	hasher := account.NewPasswordHasher(bytes.NewReader(bytes.Repeat([]byte{0x42}, 32)))
	encoded, err := hasher.Hash("correct horse battery staple")
	if err != nil {
		t.Fatalf("Hash() error = %v", err)
	}

	verified, err := hasher.Verify("correct horse battery staple", encoded)
	if err != nil {
		t.Fatalf("Verify(correct) error = %v", err)
	}
	if !verified {
		t.Error("Verify(correct) = false, want true")
	}

	verified, err = hasher.Verify("wrong password", encoded)
	if err != nil {
		t.Fatalf("Verify(wrong) error = %v", err)
	}
	if verified {
		t.Error("Verify(wrong) = true, want false")
	}
}

func TestPasswordHasherRejectsOversizedVerificationCandidate(t *testing.T) {
	t.Parallel()

	hasher := account.NewPasswordHasher(bytes.NewReader(bytes.Repeat([]byte{0x42}, 32)))
	encoded, err := hasher.Hash("correct horse battery staple")
	if err != nil {
		t.Fatalf("Hash() error = %v", err)
	}

	verified, err := hasher.Verify(strings.Repeat("a", 1025), encoded)
	if !errors.Is(err, account.ErrInvalidPassword) {
		t.Fatalf("Verify() error = %v, want ErrInvalidPassword", err)
	}
	if verified {
		t.Error("Verify() = true, want false")
	}
}
