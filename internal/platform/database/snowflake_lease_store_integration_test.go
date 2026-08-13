//go:build integration

package database_test

import (
	"context"
	"errors"
	"strings"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/persistence"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

func TestSnowflakeLeaseStoreAllocatesRenewsAndFencesNodes(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Minute)
	defer cancel()
	container, err := postgres.Run(
		ctx,
		"postgres:18.4@sha256:311136771dca6826c3b6e691ebf8cb6e896e165074bc57a728f9619f25f0c4c7",
		postgres.WithDatabase("avalon_snowflake_lease_test"),
		postgres.WithUsername("avalon"),
		postgres.WithPassword("avalon"),
		postgres.BasicWaitStrategies(),
	)
	if err != nil {
		t.Fatalf("启动 PostgreSQL: %v", err)
	}
	t.Cleanup(func() {
		if err := container.Terminate(context.Background()); err != nil {
			t.Errorf("停止 PostgreSQL: %v", err)
		}
	})
	url, err := container.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		t.Fatal(err)
	}
	pool, err := database.Open(database.Config{URL: url, MaxOpenConnections: 20, MaxIdleConnections: 10})
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(pool.Close)
	if err := pool.Persistence().ApplySchema(ctx, persistence.SchemaModeCreate); err != nil {
		t.Fatalf("创建 Ent Schema: %v", err)
	}
	store := database.NewSnowflakeLeaseStore(pool)
	ownerA, ownerB := strings.Repeat("a", 64), strings.Repeat("b", 64)
	first, err := store.Acquire(ctx, ownerA, 200*time.Millisecond)
	if err != nil {
		t.Fatal(err)
	}
	second, err := store.Acquire(ctx, ownerB, time.Second)
	if err != nil {
		t.Fatal(err)
	}
	if first.Node != 1 || second.Node != 2 || first.FencingToken != 2 {
		t.Fatalf("首次租约 = %+v，第二租约 = %+v", first, second)
	}
	if _, err := store.Renew(ctx, ownerB, first.Node, first.FencingToken, time.Second); !errors.Is(err, snowflake.ErrLeaseInvalid) {
		t.Fatalf("错误 owner 续租 error = %v", err)
	}
	time.Sleep(300 * time.Millisecond)
	reacquired, err := store.Acquire(ctx, ownerB, time.Second)
	if err != nil {
		t.Fatal(err)
	}
	if reacquired.Node != first.Node || reacquired.FencingToken != first.FencingToken+1 {
		t.Fatalf("重新领取租约 = %+v，首次租约 = %+v", reacquired, first)
	}
	if _, err := store.Renew(ctx, ownerA, first.Node, first.FencingToken, time.Second); !errors.Is(err, snowflake.ErrLeaseInvalid) {
		t.Fatalf("旧 fencing token 续租 error = %v", err)
	}
}
