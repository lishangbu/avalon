package persistence

import (
	"context"

	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

func claimResponse[T any](
	ctx context.Context,
	store idempotency.RecordStore,
	request idempotency.Request,
	response *T,
) (bool, error) {
	return idempotency.ClaimResponse(ctx, idempotency.NewPersistentWriter(store), request, response)
}

func completeResponse[T any](
	ctx context.Context,
	store idempotency.RecordStore,
	request idempotency.Request,
	response T,
) error {
	return idempotency.Complete(ctx, idempotency.NewPersistentWriter(store), request, response)
}
