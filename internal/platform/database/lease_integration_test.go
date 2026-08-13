//go:build integration

package database_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

func TestLeaseAllowsOnlyOneHolderAndCanBeReleased(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	container, err := postgres.Run(
		ctx,
		"postgres:18.4@sha256:311136771dca6826c3b6e691ebf8cb6e896e165074bc57a728f9619f25f0c4c7",
		postgres.WithDatabase("avalon_lease_test"),
		postgres.WithUsername("avalon"),
		postgres.WithPassword("avalon"),
		postgres.BasicWaitStrategies(),
	)
	if err != nil {
		t.Fatalf("start PostgreSQL: %v", err)
	}
	t.Cleanup(func() {
		if err := container.Terminate(context.Background()); err != nil {
			t.Errorf("terminate PostgreSQL: %v", err)
		}
	})
	databaseURL, err := container.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		t.Fatalf("PostgreSQL connection string: %v", err)
	}
	pool, err := database.Open(database.Config{URL: databaseURL, MaxOpenConnections: 20, MaxIdleConnections: 10})
	if err != nil {
		t.Fatalf("Open() error = %v", err)
	}
	t.Cleanup(pool.Close)

	lease, err := pool.AcquireLease(ctx, database.ServerLeaseKey)
	if err != nil {
		t.Fatalf("AcquireLease() error = %v", err)
	}
	if err := lease.Ready(ctx); err != nil {
		t.Fatalf("lease Ready() error = %v", err)
	}
	if _, err := pool.AcquireLease(ctx, database.ServerLeaseKey); !errors.Is(err, database.ErrLeaseHeld) {
		t.Fatalf("second AcquireLease() error = %v", err)
	}
	if err := lease.Close(ctx); err != nil {
		t.Fatalf("lease Close() error = %v", err)
	}

	reacquired, err := pool.AcquireLease(ctx, database.ServerLeaseKey)
	if err != nil {
		t.Fatalf("AcquireLease(after release) error = %v", err)
	}
	if err := reacquired.Close(ctx); err != nil {
		t.Fatalf("reacquired Close() error = %v", err)
	}
}
