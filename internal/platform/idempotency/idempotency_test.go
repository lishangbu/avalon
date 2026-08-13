package idempotency_test

import (
	"context"
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

func TestPersistentWriterReplaysBattleingCommittedResponse(t *testing.T) {
	t.Parallel()

	store := &recordStoreStub{
		record: idempotency.StoredRecord{RequestDigest: []byte("same"), Response: []byte(`{"id":"result"}`)},
	}
	writer := idempotency.NewPersistentWriter(store)
	claim, err := writer.ClaimIdempotency(context.Background(), idempotency.Request{RequestDigest: []byte("same")})
	if err != nil {
		t.Fatalf("ClaimIdempotency() error = %v", err)
	}
	if !claim.Replay || string(claim.Response) != `{"id":"result"}` {
		t.Fatalf("ClaimIdempotency() = %+v", claim)
	}
}

func TestPersistentWriterRejectsSameKeyWithDifferentPayload(t *testing.T) {
	t.Parallel()

	store := &recordStoreStub{record: idempotency.StoredRecord{RequestDigest: []byte("original"), Response: []byte(`{}`)}}
	writer := idempotency.NewPersistentWriter(store)
	_, err := writer.ClaimIdempotency(context.Background(), idempotency.Request{RequestDigest: []byte("different")})
	if !errors.Is(err, idempotency.ErrConflict) {
		t.Fatalf("ClaimIdempotency() error = %v, want ErrConflict", err)
	}
}

func TestPersistentWriterRequiresExactlyOneCompletedRecord(t *testing.T) {
	t.Parallel()

	store := &recordStoreStub{claimed: true, completedRows: 0}
	writer := idempotency.NewPersistentWriter(store)
	err := writer.CompleteIdempotency(context.Background(), idempotency.Request{}, []byte(`{}`))
	if err == nil {
		t.Fatal("CompleteIdempotency() error = nil, want affected-row error")
	}
}

type recordStoreStub struct {
	claimed       bool
	record        idempotency.StoredRecord
	completedRows int64
}

func (s *recordStoreStub) TryClaim(context.Context, idempotency.Request) (bool, error) {
	return s.claimed, nil
}

func (s *recordStoreStub) FindForUpdate(context.Context, idempotency.Request) (idempotency.StoredRecord, error) {
	return s.record, nil
}

func (s *recordStoreStub) CompleteRecord(context.Context, idempotency.Request, []byte) (int64, error) {
	return s.completedRows, nil
}
