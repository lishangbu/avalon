//go:build integration

package store_test

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
	gamedatastore "github.com/lishangbu/avalon/internal/gamedata/store"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/persistence"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

// TestStatCreateUsesAdminIdempotencyRecord 验证管理端创建数值项只依赖管理员账号域，
// 并把资料、管理员幂等记录和管理员审计事实提交到同一事务。
func TestStatCreateUsesAdminIdempotencyRecord(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	pool := startLiveGameDataPostgreSQL(t, ctx)
	now := time.Date(2026, time.August, 12, 10, 0, 0, 0, time.UTC)
	actorID := snowflake.NewTestID()
	if _, err := pool.Exec(ctx, `
		INSERT INTO admin_account (
			id, username, username_key, display_name, password_hash, password_algorithm,
			password_parameters, status, created_at, updated_at
		) VALUES ($1, 'stat-admin', 'stat-admin', '数值项资料管理员', 'unused', 'argon2id', '{}', 'active', $2, $2)
	`, actorID, now); err != nil {
		t.Fatalf("insert Stat admin fixture: %v", err)
	}

	store := gamedatastore.New(pool, snowflake.NewTestID)
	service := stat.NewService(store, snowflake.NewTestID, func() time.Time { return now })
	created, err := service.Create(ctx, stat.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "create-test-stat", "create-test-stat-request"),
		Code:                 "test-stat",
		Name:                 "测试数值项",
		SortOrder:            1,
		Enabled:              true,
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.Code != "test-stat" || created.Name != "测试数值项" || created.Version != 1 {
		t.Fatalf("Create() = %+v", created)
	}

	var adminRecords, playerRecords, audits int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM admin_idempotency_record WHERE actor_account_id = $1 AND operation_id = 'game-data.stat.create'`, actorID).Scan(&adminRecords); err != nil {
		t.Fatalf("query Stat admin idempotency record: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM administration_idempotency_record WHERE actor_account_id = $1 AND operation_id = 'game-data.stat.create'`, actorID).Scan(&playerRecords); err != nil {
		t.Fatalf("query Stat player idempotency record: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM admin_audit_log WHERE actor_account_id = $1 AND action_code = 'game-data.stat.created'`, actorID).Scan(&audits); err != nil {
		t.Fatalf("query Stat admin audit record: %v", err)
	}
	if adminRecords != 1 || playerRecords != 0 || audits != 1 {
		t.Fatalf("Stat persistence facts = admin idempotency %d, player idempotency %d, admin audit %d", adminRecords, playerRecords, audits)
	}
}

// TestCreatureRelationsReplaceRoundTripsThroughPostgreSQL 验证 Creature 七类关系的完整替换、
// 缺失关系的软禁用、幂等上下文和同一事务内的审计写入。
func TestCreatureRelationsReplaceRoundTripsThroughPostgreSQL(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	pool := startLiveGameDataPostgreSQL(t, ctx)
	now := time.Date(2026, time.August, 4, 10, 0, 0, 0, time.UTC)
	actorID, speciesID, creatureID, otherCreatureID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	elementID, formID, alternativeFormID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	formElementID, alternativeFormElementID := snowflake.NewTestID(), snowflake.NewTestID()
	removedSkinID, otherSkinID, newSkinID, replayUnusedID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	if _, err := pool.Exec(ctx, `
		INSERT INTO admin_account (
			id, username, username_key, display_name, password_hash, password_algorithm,
			password_parameters, status, created_at, updated_at
		) VALUES ($1, 'creature-admin', 'creature-admin', 'Creature 资料管理员', 'unused', 'argon2id', '{}', 'active', $2, $2)
	`, actorID, now); err != nil {
		t.Fatalf("insert Creature relation admin fixture: %v", err)
	}
	if _, err := pool.Exec(ctx, `
		INSERT INTO game_species (
			id, national_dex_number, code, name, gender_differences, forms_switchable, enabled, version, created_at, updated_at
		) VALUES ($1, 999999, 'relation-species', '关系测试物种', false, false, true, 1, $2, $2)
	`, speciesID, now); err != nil {
		t.Fatalf("insert Creature relation Species fixture: %v", err)
	}
	if _, err := pool.Exec(ctx, `
		INSERT INTO game_creature (
			id, code, name, species_id, male_eighths, female_eighths, default_form, enabled, version, created_at, updated_at
		) VALUES
			($1, 'relation-creature', '关系测试 Creature', $3, 4, 4, true, true, 1, $4, $4),
			($2, 'isolated-creature', '隔离 Creature', $3, 4, 4, false, true, 1, $4, $4)
	`, creatureID, otherCreatureID, speciesID, now); err != nil {
		t.Fatalf("insert Creature relation Creature fixture: %v", err)
	}
	if _, err := pool.Exec(ctx, `
		INSERT INTO game_element (id, code, name, sort_order, enabled, version, created_at, updated_at)
		VALUES ($1, 'relation-element', '关系属性', 1, true, 1, $2, $2)
	`, elementID, now); err != nil {
		t.Fatalf("insert Creature relation Element fixture: %v", err)
	}
	if _, err := pool.Exec(ctx, `
		INSERT INTO game_creature_form (
			id, code, name, creature_id, sort_order, form_order, battle_only, default_form,
			enhanced_form, enabled, version, created_at, updated_at
		) VALUES
			($1, 'relation-form', '旧形态名称', $3, 1, 1, false, true, false, true, 1, $4, $4),
			($2, 'alternative-form', '候选形态', $3, 2, 2, false, false, false, true, 1, $4, $4)
	`, formID, alternativeFormID, creatureID, now); err != nil {
		t.Fatalf("insert Creature relation Form fixture: %v", err)
	}
	if _, err := pool.Exec(ctx, `
		INSERT INTO game_creature_form_element (id, form_id, element_id)
		VALUES ($1, $3, $5), ($2, $4, $5)
	`, formElementID, alternativeFormElementID, formID, alternativeFormID, elementID); err != nil {
		t.Fatalf("insert Creature relation Form Element fixture: %v", err)
	}
	if _, err := pool.Exec(ctx, `
		INSERT INTO game_creature_skin (id, creature_id, code, name, enabled, version, created_at, updated_at)
		VALUES
			($1, $3, 'removed-skin', '待禁用皮肤', true, 1, $5, $5),
			($2, $4, 'isolated-skin', '隔离皮肤', true, 1, $5, $5)
	`, removedSkinID, otherSkinID, creatureID, otherCreatureID, now); err != nil {
		t.Fatalf("insert Creature relation Skin fixture: %v", err)
	}

	store := gamedatastore.New(pool, snowflake.NewTestID)

	generatedIDs := []snowflake.ID{newSkinID, replayUnusedID}
	service := creaturemetadata.NewAdministrationService(store, snowflake.TestSource(func() snowflake.ID {
		value := generatedIDs[0]
		generatedIDs = generatedIDs[1:]
		return value
	}), func() time.Time { return now.Add(time.Minute) })
	newCommand := func() creaturemetadata.ReplaceRelationsCommand {
		return creaturemetadata.ReplaceRelationsCommand{
			GameDataWriteContext: liveWriteContext(actorID, "replace-creature-relations"),
			CreatureID:           creatureID,
			Relations: creaturemetadata.CreatureRelations{
				Forms: []creaturemetadata.Form{
					{
						ID: alternativeFormID, Code: "alternative-form", Name: "候选形态", CreatureID: creatureID,
						SortOrder: int32Pointer(2), FormOrder: int32Pointer(2), DefaultForm: true,
						Enabled: true, Version: 1, ElementIDs: []snowflake.ID{elementID},
					},
					{
						ID: formID, Code: "relation-form", Name: "新形态名称", CreatureID: creatureID,
						SortOrder: int32Pointer(1), FormOrder: int32Pointer(1), DefaultForm: false,
						Enabled: true, Version: 1, ElementIDs: []snowflake.ID{elementID},
					},
				},
				Stats: []creaturemetadata.StatBinding{},
				Skins: []creaturemetadata.Skin{{Code: "new-skin", Name: "新增皮肤", Enabled: true}},
			},
		}
	}
	updated, err := service.ReplaceRelations(ctx, newCommand())
	if err != nil {
		t.Fatalf("ReplaceRelations() error = %v", err)
	}
	var idempotencyRecordCount int
	if err := pool.QueryRow(ctx, `
		SELECT count(*)
		FROM admin_idempotency_record
		WHERE actor_account_id = $1 AND operation_id = 'game-data.creature-relations.replace'
	`, actorID).Scan(&idempotencyRecordCount); err != nil {
		t.Fatalf("query GameData admin idempotency record: %v", err)
	}
	if idempotencyRecordCount != 1 {
		t.Fatalf("GameData admin idempotency record count = %d, want 1", idempotencyRecordCount)
	}
	if len(updated.Forms) != 2 || updated.Forms[0].ID != alternativeFormID || !updated.Forms[0].DefaultForm || updated.Forms[0].Version != 2 ||
		updated.Forms[1].Name != "新形态名称" || updated.Forms[1].DefaultForm || updated.Forms[1].Version != 2 {
		t.Fatalf("updated Forms = %+v", updated.Forms)
	}
	if len(updated.Skins) != 2 || updated.Skins[0].ID != newSkinID || updated.Skins[0].Version != 1 || !updated.Skins[0].Enabled ||
		updated.Skins[1].ID != removedSkinID || updated.Skins[1].Version != 2 || updated.Skins[1].Enabled {
		t.Fatalf("updated Skins = %+v", updated.Skins)
	}
	otherRelations, err := service.GetCreatureRelations(ctx, otherCreatureID)
	if err != nil || len(otherRelations.Skins) != 1 || !otherRelations.Skins[0].Enabled || otherRelations.Skins[0].Version != 1 {
		t.Fatalf("other Creature relations = %+v, error = %v", otherRelations, err)
	}

	replayed, err := service.ReplaceRelations(ctx, newCommand())
	if err != nil || len(replayed.Skins) != 2 || replayed.Skins[0].ID != newSkinID {
		t.Fatalf("replayed ReplaceRelations() = %+v, error = %v", replayed, err)
	}
	loaded, err := service.GetCreatureRelations(ctx, creatureID)
	if err != nil || loaded.Forms[0].Version != 2 || loaded.Skins[0].Version != 1 {
		t.Fatalf("relations after rejected write = %+v, error = %v", loaded, err)
	}
}

func int32Pointer(value int32) *int32 { return &value }

func startLiveGameDataPostgreSQL(t *testing.T, ctx context.Context) *database.Pool {
	t.Helper()
	container, err := postgres.Run(
		ctx,
		"postgres:18.4@sha256:311136771dca6826c3b6e691ebf8cb6e896e165074bc57a728f9619f25f0c4c7",
		postgres.WithDatabase("avalon_live_game_data_test"),
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
		t.Fatalf("open PostgreSQL: %v", err)
	}
	t.Cleanup(pool.Close)
	if err := pool.Persistence().ApplySchema(ctx, persistence.SchemaModeCreate); err != nil {
		t.Fatalf("创建 Ent Schema: %v", err)
	}
	resetLiveGameDataFixture(t, ctx, pool)
	if _, err := pool.Exec(ctx, `INSERT INTO audit_hash_chain_state (id, ledger, latest_hash, updated_at) VALUES (1048577, 'admin_audit_log', ''::bytea, now()), (1048578, 'administration_audit_log', ''::bytea, now())`); err != nil {
		t.Fatalf("初始化 GameData 审计哈希链: %v", err)
	}
	return pool
}

// resetLiveGameDataFixture 将受控 SQL seed 的首套资料清理为存储级测试需要的空资料状态。
// 这些测试分别构造精确的修订、锁和唯一键场景；它们不验证部署 seed，不能继承其维护窗口或资料行。
func resetLiveGameDataFixture(t *testing.T, ctx context.Context, pool *database.Pool) {
	t.Helper()
	if _, err := pool.Exec(ctx, `
		TRUNCATE TABLE
			game_ability, game_battle_format, game_battle_clause,
			game_battle_restriction, game_battle_mechanic,
			game_creature_skill_learn, game_creature_ability, game_creature_held_item,
			game_creature_stat, game_creature_form_element, game_creature_skin,
			game_creature_form, game_creature, game_species_egg_group, game_species,
			game_egg_group, game_gender, game_growth_rate, game_habitat,
			game_species_color, game_species_shape,
			game_element, game_item, game_item_category,
			game_skill_stat_change, game_skill, game_skill_ailment,
			game_skill_category, game_skill_damage_class, game_skill_learn_method,
			game_skill_target, game_stat, battle_bot_strategy
		CASCADE
	`); err != nil {
		t.Fatalf("clear seeded live Game Data fixture: %v", err)
	}
}

func liveWriteContext(actorID snowflake.ID, key string) administration.GameDataWriteContext {
	return administration.NewGameDataWriteContext(actorID, key, key+"-request")
}
