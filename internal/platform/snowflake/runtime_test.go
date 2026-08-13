package snowflake

import (
	"bytes"
	"context"
	"errors"
	"testing"
	"time"
)

type leaseStoreStub struct {
	grant    LeaseGrant
	renewErr error
}

func (store *leaseStoreStub) Acquire(context.Context, string, time.Duration) (LeaseGrant, error) {
	return store.grant, nil
}

func (store *leaseStoreStub) Renew(context.Context, string, uint8, int64, time.Duration) (LeaseGrant, error) {
	return store.grant, store.renewErr
}

func TestRuntimeAcquiresLeaseAndGeneratesID(t *testing.T) {
	now := time.Now().UTC()
	store := &leaseStoreStub{grant: LeaseGrant{Node: 12, FencingToken: 3, DatabaseTime: now, LeaseExpiresAt: now.Add(30 * time.Second)}}
	runtime, err := acquireRuntime(context.Background(), store, bytes.NewReader(make([]byte, ownerTokenLength)))
	if err != nil {
		t.Fatal(err)
	}
	defer runtime.Close()
	id, err := runtime.Next(context.Background())
	if err != nil || !id.IsValid() || runtime.Node() != 12 {
		t.Fatalf("Next() = %v, %v, node = %d", id, err, runtime.Node())
	}
}

func TestRuntimeRejectsInvalidGrant(t *testing.T) {
	now := time.Now().UTC()
	store := &leaseStoreStub{grant: LeaseGrant{Node: 0, FencingToken: 1, DatabaseTime: now, LeaseExpiresAt: now.Add(time.Second)}}
	_, err := acquireRuntime(context.Background(), store, bytes.NewReader(make([]byte, ownerTokenLength)))
	if err == nil || errors.Is(err, context.Canceled) {
		t.Fatalf("AcquireRuntime() error = %v, want invalid grant", err)
	}
}

func TestRuntimeRejectsGrantInsideSafetyWindow(t *testing.T) {
	t.Parallel()
	now := time.Now().UTC()
	grant := LeaseGrant{
		Node: 1, FencingToken: 1, DatabaseTime: now,
		LeaseExpiresAt: now.Add(leaseSafety),
	}
	if err := validateGrant(grant); err == nil {
		t.Fatal("安全窗口内的租约仍被接受")
	}
}

func TestRuntimeDeadlineIncludesStoreRoundTripInsideSafetyWindow(t *testing.T) {
	t.Parallel()
	requestStartedAt := time.Now()
	databaseTime := time.Now().UTC()
	grant := LeaseGrant{
		Node: 1, FencingToken: 1, DatabaseTime: databaseTime,
		LeaseExpiresAt: databaseTime.Add(leaseDuration),
	}
	runtime := &Runtime{}
	runtime.valid.Store(true)
	runtime.setDeadline(grant, requestStartedAt)
	deadline := runtime.deadline.Load()
	if deadline == nil {
		t.Fatal("Runtime 未保存租约截止时间")
	}
	if got, want := deadline.Sub(requestStartedAt), leaseDuration-leaseSafety; got != want {
		t.Fatalf("租约本地有效期 = %s，期望 %s", got, want)
	}

	// 数据库调用已经耗尽安全窗口时，即使 Grant 自身仍描述完整租期，也不得从响应时刻重新计时。
	delayedRuntime := &Runtime{}
	delayedRuntime.valid.Store(true)
	delayedRuntime.setDeadline(grant, time.Now().Add(-leaseDuration))
	if delayedRuntime.leaseValid() {
		t.Fatal("慢数据库响应错误延长了本地租约")
	}
}

func TestRuntimeRejectsDatabaseClockSkewInBothDirections(t *testing.T) {
	for _, databaseTime := range []time.Time{
		time.Now().Add(-maximumClockRollback - time.Second),
		time.Now().Add(maximumClockRollback + time.Second),
	} {
		grant := LeaseGrant{Node: 1, FencingToken: 1, DatabaseTime: databaseTime, LeaseExpiresAt: databaseTime.Add(leaseDuration)}
		if err := validateGrant(grant); !errors.Is(err, ErrClockRollback) {
			t.Fatalf("validateGrant(databaseTime=%s) error = %v", databaseTime, err)
		}
	}
}
