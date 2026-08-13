package snowflake

import (
	"context"
	"errors"
	"sync"
	"testing"
	"time"
)

type monitorReadinessStub struct {
	mu  sync.RWMutex
	err error
}

func (stub *monitorReadinessStub) Ready(context.Context) error {
	stub.mu.RLock()
	defer stub.mu.RUnlock()
	return stub.err
}

func (stub *monitorReadinessStub) fail(err error) {
	stub.mu.Lock()
	stub.err = err
	stub.mu.Unlock()
}

func TestLeaseMonitorReturnsReadinessFailure(t *testing.T) {
	t.Parallel()
	stub := &monitorReadinessStub{}
	monitor, err := newLeaseMonitor(stub, time.Millisecond)
	if err != nil {
		t.Fatal(err)
	}
	done := make(chan error, 1)
	go func() { done <- monitor.Start(context.Background()) }()
	want := errors.New("租约已失效")
	stub.fail(want)
	select {
	case err := <-done:
		if !errors.Is(err, want) {
			t.Fatalf("Start() error = %v, want %v", err, want)
		}
	case <-time.After(time.Second):
		t.Fatal("LeaseMonitor 未传播租约失效")
	}
}

func TestLeaseMonitorStopEndsHealthyMonitor(t *testing.T) {
	t.Parallel()
	monitor, err := newLeaseMonitor(&monitorReadinessStub{}, time.Millisecond)
	if err != nil {
		t.Fatal(err)
	}
	done := make(chan error, 1)
	go func() { done <- monitor.Start(context.Background()) }()
	if err := monitor.Stop(context.Background()); err != nil {
		t.Fatal(err)
	}
	select {
	case err := <-done:
		if err != nil {
			t.Fatalf("Start() error = %v", err)
		}
	case <-time.After(time.Second):
		t.Fatal("Stop() 未结束 LeaseMonitor")
	}
}
