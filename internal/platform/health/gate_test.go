package health_test

import (
	"context"
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/health"
)

func TestGateBecomesUnavailableBeforeShutdownStarts(t *testing.T) {
	t.Parallel()

	gate := health.NewGate(readinessStub{})
	if err := gate.Ready(context.Background()); err != nil {
		t.Fatalf("Ready() before drain = %v, want nil", err)
	}

	gate.BeginDrain()
	if err := gate.Ready(context.Background()); !errors.Is(err, health.ErrDraining) {
		t.Fatalf("Ready() after drain = %v, want ErrDraining", err)
	}
}
