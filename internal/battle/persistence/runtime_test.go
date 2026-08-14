package persistence

import (
	"errors"
	"testing"
	"time"
)

func TestRecoveryBackoffUsesFixedBoundedSchedule(t *testing.T) {
	want := []time.Duration{5 * time.Second, 15 * time.Second, 45 * time.Second, 2 * time.Minute, 5 * time.Minute}
	for index, expected := range want {
		actual, err := RecoveryBackoff(int32(index + 1))
		if err != nil || actual != expected {
			t.Fatalf("RecoveryBackoff(%d) = %s, %v; want %s", index+1, actual, err, expected)
		}
	}
	if _, err := RecoveryBackoff(0); !errors.Is(err, ErrRecoveryExhausted) {
		t.Fatalf("RecoveryBackoff(0) error = %v, want %v", err, ErrRecoveryExhausted)
	}
	if _, err := RecoveryBackoff(6); !errors.Is(err, ErrRecoveryExhausted) {
		t.Fatalf("RecoveryBackoff(6) error = %v, want %v", err, ErrRecoveryExhausted)
	}
}
