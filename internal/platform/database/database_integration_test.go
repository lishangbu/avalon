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

func TestOpenReportsReadyForAvailablePostgreSQL(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	container, err := postgres.Run(
		ctx,
		"postgres:18.4@sha256:311136771dca6826c3b6e691ebf8cb6e896e165074bc57a728f9619f25f0c4c7",
		postgres.WithDatabase("avalon_test"),
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

	url, err := container.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		t.Fatalf("PostgreSQL connection string: %v", err)
	}
	pool, err := database.Open(database.Config{URL: url, MaxOpenConnections: 20, MaxIdleConnections: 10})
	if err != nil {
		t.Fatalf("Open() error = %v", err)
	}
	t.Cleanup(pool.Close)

	if err := pool.Ready(ctx); err != nil {
		t.Fatalf("Ready() error = %v, want nil", err)
	}
}

func TestWithTxRollsBackWhenCallbackFails(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	container, err := postgres.Run(
		ctx,
		"postgres:18.4@sha256:311136771dca6826c3b6e691ebf8cb6e896e165074bc57a728f9619f25f0c4c7",
		postgres.WithDatabase("avalon_test"),
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

	url, err := container.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		t.Fatalf("PostgreSQL connection string: %v", err)
	}
	pool, err := database.Open(database.Config{URL: url, MaxOpenConnections: 20, MaxIdleConnections: 10})
	if err != nil {
		t.Fatalf("Open() error = %v", err)
	}
	t.Cleanup(pool.Close)

	if err := pool.WithTx(ctx, func(tx database.Transaction) error {
		_, err := tx.Exec(ctx, "CREATE TABLE rollback_probe (value integer NOT NULL)")
		return err
	}); err != nil {
		t.Fatalf("create probe table: %v", err)
	}

	wantErr := errors.New("reject transaction")
	err = pool.WithTx(ctx, func(tx database.Transaction) error {
		if _, err := tx.Exec(ctx, "INSERT INTO rollback_probe (value) VALUES (1)"); err != nil {
			return err
		}
		return wantErr
	})
	if !errors.Is(err, wantErr) {
		t.Fatalf("WithTx() error = %v, want %v", err, wantErr)
	}

	if err := pool.WithTx(ctx, func(tx database.Transaction) error {
		var count int
		if err := tx.QueryRow(ctx, "SELECT count(*) FROM rollback_probe").Scan(&count); err != nil {
			return err
		}
		if count != 0 {
			t.Errorf("row count after rollback = %d", count)
		}
		return nil
	}); err != nil {
		t.Fatalf("query probe table: %v", err)
	}
}
