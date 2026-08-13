package health_test

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/health"
)

func TestLivenessStaysAvailableWithoutExternalDependencies(t *testing.T) {
	t.Parallel()

	recorder := httptest.NewRecorder()
	request := httptest.NewRequest(http.MethodGet, "/livez", nil)

	health.NewHandler(nil).Liveness(recorder, request)

	if recorder.Code != http.StatusNoContent {
		t.Fatalf("status = %d, want %d", recorder.Code, http.StatusNoContent)
	}
	if recorder.Body.Len() != 0 {
		t.Errorf("body = %q, want empty", recorder.Body.String())
	}
}

func TestReadinessReflectsDependencyAvailabilityWithoutLeakingErrors(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name       string
		checkError error
		wantStatus int
	}{
		{name: "ready", wantStatus: http.StatusNoContent},
		{name: "database unavailable", checkError: errors.New("postgres password=secret"), wantStatus: http.StatusServiceUnavailable},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()
			recorder := httptest.NewRecorder()
			request := httptest.NewRequest(http.MethodGet, "/readyz", nil)

			health.NewHandler(readinessStub{err: tt.checkError}).Readiness(recorder, request)

			if recorder.Code != tt.wantStatus {
				t.Fatalf("status = %d, want %d", recorder.Code, tt.wantStatus)
			}
			if recorder.Body.Len() != 0 {
				t.Errorf("body = %q, want empty", recorder.Body.String())
			}
		})
	}
}

func TestReadinessGivesDependencyChecksADeadline(t *testing.T) {
	t.Parallel()

	recorder := httptest.NewRecorder()
	request := httptest.NewRequest(http.MethodGet, "/readyz", nil)
	health.NewHandler(readinessFunc(func(ctx context.Context) error {
		if _, ok := ctx.Deadline(); !ok {
			return errors.New("missing deadline")
		}
		return nil
	})).Readiness(recorder, request)

	if recorder.Code != http.StatusNoContent {
		t.Fatalf("status = %d, want %d", recorder.Code, http.StatusNoContent)
	}
}

type readinessStub struct {
	err error
}

func (s readinessStub) Ready(context.Context) error {
	return s.err
}

type readinessFunc func(context.Context) error

func (f readinessFunc) Ready(ctx context.Context) error {
	return f(ctx)
}
