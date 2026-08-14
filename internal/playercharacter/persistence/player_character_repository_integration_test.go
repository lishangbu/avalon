//go:build integration

package persistence_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/persistence"
	"github.com/lishangbu/avalon/internal/playercharacter"
	playerpersistence "github.com/lishangbu/avalon/internal/playercharacter/persistence"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

const playerCharacterPostgresImage = "postgres:18.4@sha256:311136771dca6826c3b6e691ebf8cb6e896e165074bc57a728f9619f25f0c4c7"

func TestStorePreventsAccountFromExceedingThreeActivePlayerCharacters(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	pool := startPlayerCharacterDatabase(t, ctx)
	accountID := snowflake.MustParse("1048576068")
	insertPlayerCharacterAccount(t, ctx, pool, accountID)
	ids := []snowflake.ID{
		snowflake.MustParse("1048576069"),
		snowflake.MustParse("1048576070"),
		snowflake.MustParse("1048576071"),
		snowflake.MustParse("1048576072"),
		snowflake.MustParse("1048576073"),
	}
	nextID := 0
	service := playercharacter.NewService(
		playerpersistence.NewRepository(pool, snowflake.NewTestID),
		snowflake.TestSource(func() snowflake.ID {
			id := ids[nextID]
			nextID++
			return id
		}),
		time.Now,
	)
	for index, displayName := range []string{"角色一", "角色二", "角色三"} {
		created, err := service.Create(ctx, playercharacter.CreateCommand{
			AccountID: accountID, DisplayName: displayName,
			IdempotencyKey: "create-character-" + displayName, RequestID: snowflake.NewTestID().String(),
		})
		if err != nil {
			t.Fatalf("Create(%d) error = %v", index, err)
		}
		if index == 2 {
			replayed, replayErr := service.Create(ctx, playercharacter.CreateCommand{
				AccountID: accountID, DisplayName: displayName,
				IdempotencyKey: "create-character-" + displayName, RequestID: snowflake.NewTestID().String(),
			})
			if replayErr != nil || replayed.ID != created.ID {
				t.Fatalf("retry third Create() = %+v, error = %v", replayed, replayErr)
			}
		}
	}
	_, err := service.Create(ctx, playercharacter.CreateCommand{
		AccountID: accountID, DisplayName: "角色四",
		IdempotencyKey: "create-character-four", RequestID: snowflake.NewTestID().String(),
	})
	if !errors.Is(err, playercharacter.ErrActiveLimitExceeded) {
		t.Fatalf("fourth Create() error = %v, want ErrActiveLimitExceeded", err)
	}
}

func TestStoreReservesHistoricalDisplayNameAgainstOtherPlayerCharacters(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	pool := startPlayerCharacterDatabase(t, ctx)
	accountID := snowflake.MustParse("1048576074")
	insertPlayerCharacterAccount(t, ctx, pool, accountID)
	ids := []snowflake.ID{
		snowflake.MustParse("1048576075"),
		snowflake.MustParse("1048576076"),
	}
	nextID := 0
	service := playercharacter.NewService(
		playerpersistence.NewRepository(pool, snowflake.NewTestID),
		snowflake.TestSource(func() snowflake.ID {
			id := ids[nextID]
			nextID++
			return id
		}),
		time.Now,
	)
	first, err := service.Create(ctx, playercharacter.CreateCommand{
		AccountID: accountID, DisplayName: "星界一号",
		IdempotencyKey: "create-star-one", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil {
		t.Fatalf("first Create() error = %v", err)
	}
	renamed, err := service.Rename(ctx, playercharacter.RenameCommand{
		AccountID: accountID, PlayerCharacterID: first.ID, ExpectedVersion: first.Version,
		DisplayName: "星界二号", IdempotencyKey: "rename-star-two", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil {
		t.Fatalf("Rename() error = %v", err)
	}
	_, err = service.Create(ctx, playercharacter.CreateCommand{
		AccountID: accountID, DisplayName: "星界一号",
		IdempotencyKey: "reuse-star-one", RequestID: snowflake.NewTestID().String(),
	})
	if !errors.Is(err, playercharacter.ErrDisplayNameUnavailable) {
		t.Fatalf("other character Create() error = %v, want ErrDisplayNameUnavailable", err)
	}
	reclaimed, err := service.Rename(ctx, playercharacter.RenameCommand{
		AccountID: accountID, PlayerCharacterID: first.ID, ExpectedVersion: renamed.Version,
		DisplayName: "星界一号", IdempotencyKey: "reclaim-star-one", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil {
		t.Fatalf("owner Rename() error = %v", err)
	}
	if reclaimed.DisplayName != "星界一号" || reclaimed.Version != 3 {
		t.Fatalf("reclaimed = %+v", reclaimed)
	}
}

func TestStorePersistsOneOptimisticallyVersionedActiveBindingAcrossDevices(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	pool := startPlayerCharacterDatabase(t, ctx)
	accountID := snowflake.MustParse("1048576092")
	insertPlayerCharacterAccount(t, ctx, pool, accountID)
	characterIDs := []snowflake.ID{
		snowflake.MustParse("1048576093"),
		snowflake.MustParse("1048576094"),
	}
	nextID := 0
	repository := playerpersistence.NewRepository(pool, snowflake.NewTestID)
	lifecycle := playercharacter.NewService(store, snowflake.TestSource(func() snowflake.ID {
		id := characterIDs[nextID]
		nextID++
		return id
	}), time.Now)
	for index, displayName := range []string{"并发角色一", "并发角色二"} {
		if _, err := lifecycle.Create(ctx, playercharacter.CreateCommand{
			AccountID: accountID, DisplayName: displayName,
			IdempotencyKey: "create-concurrent-character-" + displayName, RequestID: snowflake.NewTestID().String(),
		}); err != nil {
			t.Fatalf("Create(%d) error = %v", index, err)
		}
	}
	active := playercharacter.NewActiveService(repository, playercharacter.NewPresenceRegistry(time.Minute), nil, time.Now)
	_, err := active.Switch(ctx, playercharacter.SwitchActiveCommand{
		AccountID: accountID, PlayerCharacterID: characterIDs[0], ExpectedVersion: 1,
		IdempotencyKey: "activate-with-stale-initial-version", RequestID: snowflake.NewTestID().String(),
	})
	if !errors.Is(err, playercharacter.ErrActiveBindingConflict) {
		t.Fatalf("initial stale Switch() error = %v, want ErrActiveBindingConflict", err)
	}
	first, err := active.Switch(ctx, playercharacter.SwitchActiveCommand{
		AccountID: accountID, PlayerCharacterID: characterIDs[0], ExpectedVersion: 0,
		IdempotencyKey: "activate-first", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil || first.Version != 1 {
		t.Fatalf("first Switch() = %+v, error = %v", first, err)
	}
	second, err := active.Switch(ctx, playercharacter.SwitchActiveCommand{
		AccountID: accountID, PlayerCharacterID: characterIDs[1], ExpectedVersion: first.Version,
		IdempotencyKey: "activate-second", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil || second.PlayerCharacterID != characterIDs[1] || second.Version != 2 {
		t.Fatalf("second Switch() = %+v, error = %v", second, err)
	}
	replayedFirst, err := active.Switch(ctx, playercharacter.SwitchActiveCommand{
		AccountID: accountID, PlayerCharacterID: characterIDs[0], ExpectedVersion: 0,
		IdempotencyKey: "activate-first", RequestID: snowflake.NewTestID().String(),
	})
	if err != nil || replayedFirst.PlayerCharacterID != characterIDs[0] || replayedFirst.Version != 1 {
		t.Fatalf("replayed first Switch() = %+v, error = %v", replayedFirst, err)
	}
	current, err := store.GetActive(ctx, accountID)
	if err != nil || current != second {
		t.Fatalf("GetActive() after delayed replay = %+v, error = %v", current, err)
	}
	_, err = active.Switch(ctx, playercharacter.SwitchActiveCommand{
		AccountID: accountID, PlayerCharacterID: characterIDs[0], ExpectedVersion: first.Version,
		IdempotencyKey: "stale-device-switch", RequestID: snowflake.NewTestID().String(),
	})
	if !errors.Is(err, playercharacter.ErrActiveBindingConflict) {
		t.Fatalf("stale Switch() error = %v, want ErrActiveBindingConflict", err)
	}
	query := playercharacter.NewQueryService(store, playercharacter.NewPresenceRegistry(time.Minute), time.Now)
	owned, err := query.ListOwned(ctx, accountID, false)
	if err != nil || len(owned) != 2 {
		t.Fatalf("ListOwned() = %+v, error = %v", owned, err)
	}
}

func startPlayerCharacterDatabase(t *testing.T, ctx context.Context) *database.Pool {
	t.Helper()
	container, err := postgres.Run(
		ctx,
		playerCharacterPostgresImage,
		postgres.WithDatabase("avalon_player_character_test"),
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
	databaseURL, err := container.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		t.Fatalf("读取 PostgreSQL 地址: %v", err)
	}
	pool, err := database.Open(database.Config{URL: databaseURL, MaxOpenConnections: 20, MaxIdleConnections: 10})
	if err != nil {
		t.Fatalf("打开数据库: %v", err)
	}
	t.Cleanup(pool.Close)
	if err := pool.Persistence().ApplySchema(ctx, persistence.SchemaModeCreate); err != nil {
		t.Fatalf("创建 Ent Schema: %v", err)
	}
	if _, err := pool.Exec(ctx, `
INSERT INTO rpg_region (id, code, name, enabled, version, created_at, updated_at)
VALUES (1048576201, 'test-region', '测试区域', true, 1, now(), now());
INSERT INTO rpg_location (id, region_id, code, name, location_type, default_spawn, enabled, version, created_at, updated_at)
VALUES (1048576202, 1048576201, 'test-spawn', '测试出生点', 'settlement', true, true, 1, now(), now())`); err != nil {
		t.Fatalf("初始化 PlayerCharacter RPG 出生点: %v", err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO audit_hash_chain_state (id, ledger, latest_hash, updated_at) VALUES (1048578, 'administration_audit_log', ''::bytea, now())`); err != nil {
		t.Fatalf("初始化 PlayerCharacter 审计哈希链: %v", err)
	}
	return pool
}

func insertPlayerCharacterAccount(t *testing.T, ctx context.Context, pool *database.Pool, accountID snowflake.ID) {
	t.Helper()
	_, err := pool.Exec(ctx, `
        INSERT INTO account (
            id, username, username_key, display_name, password_hash, password_algorithm,
            password_parameters, status, security_version, created_at, updated_at
        ) VALUES ($1, 'character-owner', 'character-owner', '角色账号', 'test', 'argon2id', '{}',
                  'active', 1, now(), now())
    `, accountID)
	if err != nil {
		t.Fatalf("创建测试账号: %v", err)
	}
}
