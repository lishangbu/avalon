//go:build integration

package rpg_test

import (
	"context"
	"encoding/json"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/rpg"
	rpgpersistence "github.com/lishangbu/avalon/internal/rpg/persistence"
)

// TestEquipmentLifecycleIsAtomicAndIdempotent 验证资料保存、管理授予、整套换装和出售钱包入账共享稳定事务语义。
func TestEquipmentLifecycleIsAtomicAndIdempotent(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startEncounterDatabase(t, ctx)
	accountID, playerID, adminID, itemID, currencyID, now := seedEquipmentFixture(t, ctx, pool)
	world := rpgpersistence.NewAdapters(pool, snowflake.NewTestID)

	saved, err := world.SaveEquipment(ctx, rpg.SaveEquipmentCommand{
		Write: rpg.AdminWriteContext{ActorAccountID: adminID, IdempotencyKey: "equipment-save-integration", RequestID: "equipment-save-request"},
		Value: rpg.AdminEquipment{ItemID: itemID, SellCurrencyID: currencyID, SlotType: rpg.EquipmentSlotTypeHead, MinimumLevel: 1, SellPrice: 25, Enabled: true},
	})
	if err != nil {
		t.Fatalf("SaveEquipment() error = %v", err)
	}
	grantCommand := rpg.GrantEquipmentCommand{Write: rpg.AdminWriteContext{ActorAccountID: adminID, IdempotencyKey: "equipment-grant-integration", RequestID: "equipment-grant-request"}, PlayerCharacterID: playerID, EquipmentID: saved.ID, Quantity: 2, Reason: "集成测试授予", Now: now}
	granted, err := world.GrantEquipment(ctx, grantCommand)
	if err != nil {
		t.Fatalf("GrantEquipment() error = %v", err)
	}
	replayed, err := world.GrantEquipment(ctx, grantCommand)
	if err != nil || replayed.OperationID != granted.OperationID || len(replayed.InstanceIDs) != 2 || replayed.InstanceIDs[0] != granted.InstanceIDs[0] || replayed.InstanceIDs[1] != granted.InstanceIDs[1] {
		t.Fatalf("GrantEquipment(replay) = %+v, error = %v", replayed, err)
	}

	loadout, err := world.ReplaceEquipmentLoadout(ctx, rpg.ReplaceEquipmentLoadoutCommand{AccountID: accountID, Entries: []rpg.EquipmentLoadoutEntry{{Slot: rpg.EquipmentSlotHead, InstanceID: granted.InstanceIDs[0]}}, ExpectedVersion: 1, IdempotencyKey: "equipment-equip-integration", Now: now.Add(time.Second)})
	if err != nil || loadout.Version != 2 {
		t.Fatalf("ReplaceEquipmentLoadout() = %+v, error = %v", loadout, err)
	}
	var originalLoadoutEntryID snowflake.ID
	var originalEquippedAt time.Time
	if err = pool.QueryRow(ctx, `SELECT id, equipped_at FROM player_character_equipment_loadout_entry WHERE player_character_id = $1 AND slot = 'head'`, playerID).Scan(&originalLoadoutEntryID, &originalEquippedAt); err != nil {
		t.Fatalf("读取初始 Loadout Entry: %v", err)
	}
	unchangedLoadout, err := world.ReplaceEquipmentLoadout(ctx, rpg.ReplaceEquipmentLoadoutCommand{AccountID: accountID, Entries: []rpg.EquipmentLoadoutEntry{{Slot: rpg.EquipmentSlotHead, InstanceID: granted.InstanceIDs[0]}}, ExpectedVersion: 2, IdempotencyKey: "equipment-loadout-unchanged", Now: now.Add(2 * time.Second)})
	if err != nil || unchangedLoadout.Version != 3 {
		t.Fatalf("ReplaceEquipmentLoadout(unchanged) = %+v, error = %v", unchangedLoadout, err)
	}
	var unchangedLoadoutEntryID snowflake.ID
	var unchangedEquippedAt time.Time
	var equipTransactionCount int64
	if err = pool.QueryRow(ctx, `SELECT id, equipped_at FROM player_character_equipment_loadout_entry WHERE player_character_id = $1 AND slot = 'head'`, playerID).Scan(&unchangedLoadoutEntryID, &unchangedEquippedAt); err != nil {
		t.Fatalf("读取未变化 Loadout Entry: %v", err)
	}
	if err = pool.QueryRow(ctx, `SELECT count(*) FROM player_character_equipment_transaction WHERE player_character_id = $1 AND equipment_instance_id = $2 AND action = 'equip'`, playerID, granted.InstanceIDs[0]).Scan(&equipTransactionCount); err != nil {
		t.Fatalf("读取穿戴流水: %v", err)
	}
	if unchangedLoadoutEntryID != originalLoadoutEntryID || !unchangedEquippedAt.Equal(originalEquippedAt) || equipTransactionCount != 1 {
		t.Fatalf("未变化 Loadout 关系不稳定: id %s/%s, time %s/%s, equip transactions=%d", originalLoadoutEntryID, unchangedLoadoutEntryID, originalEquippedAt, unchangedEquippedAt, equipTransactionCount)
	}
	snapshotJSON, err := world.FreezePlayerCharacterEquipmentForBattle(ctx, playerID)
	if err != nil {
		t.Fatalf("FreezePlayerCharacterEquipmentForBattle() error = %v", err)
	}
	var snapshot rpg.EquipmentBattleSnapshotDocument
	if err = json.Unmarshal(snapshotJSON, &snapshot); err != nil || snapshot.SchemaVersion != 1 || snapshot.LoadoutVersion != 3 || len(snapshot.Entries) != 1 || snapshot.Entries[0].InstanceID != granted.InstanceIDs[0] || snapshot.Entries[0].EquipmentID != saved.ID {
		t.Fatalf("Equipment Battle Snapshot = %s, decoded=%+v, error=%v", snapshotJSON, snapshot, err)
	}
	if _, err := world.SellEquipmentInstance(ctx, rpg.SellEquipmentCommand{AccountID: accountID, InstanceID: granted.InstanceIDs[0], ExpectedVersion: 1, IdempotencyKey: "equipment-sell-equipped", Now: now.Add(2 * time.Second)}); !errors.Is(err, rpg.ErrEquipmentLoadoutConflict) {
		t.Fatalf("SellEquipmentInstance(equipped) error = %v", err)
	}
	if _, err := world.ReplaceEquipmentLoadout(ctx, rpg.ReplaceEquipmentLoadoutCommand{AccountID: accountID, Entries: []rpg.EquipmentLoadoutEntry{}, ExpectedVersion: 3, IdempotencyKey: "equipment-unequip-integration", Now: now.Add(3 * time.Second)}); err != nil {
		t.Fatalf("ReplaceEquipmentLoadout(empty) error = %v", err)
	}
	sold, err := world.SellEquipmentInstance(ctx, rpg.SellEquipmentCommand{AccountID: accountID, InstanceID: granted.InstanceIDs[0], ExpectedVersion: 1, IdempotencyKey: "equipment-sell-integration", Now: now.Add(4 * time.Second)})
	if err != nil || sold.CurrencyID != currencyID || sold.SellPrice != 25 || sold.BalanceAfter != 25 {
		t.Fatalf("SellEquipmentInstance() = %+v, error = %v", sold, err)
	}
	replayedSale, err := world.SellEquipmentInstance(ctx, rpg.SellEquipmentCommand{AccountID: accountID, InstanceID: granted.InstanceIDs[0], ExpectedVersion: 1, IdempotencyKey: "equipment-sell-integration", Now: now.Add(4 * time.Second)})
	if err != nil || replayedSale != sold {
		t.Fatalf("SellEquipmentInstance(replay) = %+v, error = %v", replayedSale, err)
	}

	var balance, currencyTransactions, soldInstances, acquireTransactions int64
	queries := []struct {
		query  string
		args   []any
		target *int64
	}{
		{`SELECT balance FROM player_character_wallet WHERE player_character_id = $1 AND currency_id = $2`, []any{playerID, currencyID}, &balance},
		{`SELECT count(*) FROM player_character_currency_transaction WHERE player_character_id = $1 AND reference_id = $2`, []any{playerID, sold.OperationID}, &currencyTransactions},
		{`SELECT count(*) FROM player_character_equipment_instance WHERE id = $1 AND sold_at IS NOT NULL`, []any{granted.InstanceIDs[0]}, &soldInstances},
		{`SELECT count(*) FROM player_character_equipment_transaction WHERE operation_id = $1 AND action = 'acquire'`, []any{granted.OperationID}, &acquireTransactions},
	}
	for _, query := range queries {
		if err := pool.QueryRow(ctx, query.query, query.args...).Scan(query.target); err != nil {
			t.Fatalf("读取 Equipment 事务事实: %v", err)
		}
	}
	if balance != 25 || currencyTransactions != 1 || soldInstances != 1 || acquireTransactions != 2 {
		t.Fatalf("Equipment 事务事实 = balance %d, currency %d, sold %d, acquire %d", balance, currencyTransactions, soldInstances, acquireTransactions)
	}
}

// TestEquipmentListsUseStableKeysetCursors 验证装备资料、玩家实例、管理实例与资产流水
// 均可在相同时间戳和过滤条件下连续翻页，不重复、不漏行且拒绝跨筛选复用游标。
func TestEquipmentListsUseStableKeysetCursors(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startEncounterDatabase(t, ctx)
	accountID, playerID, adminID, firstItemID, currencyID, now := seedEquipmentFixture(t, ctx, pool)
	world := rpgpersistence.NewAdapters(pool, snowflake.NewTestID)
	secondItemID := snowflake.NewTestID()
	if _, err := pool.Exec(ctx, `INSERT INTO game_item (id, code, name, usage_type, cost, enabled, version, created_at, updated_at) VALUES ($1, 'steel-boots', '钢靴', 'equipment', 0, true, 1, $2, $2)`, secondItemID, now); err != nil {
		t.Fatalf("写入第二件装备道具: %v", err)
	}
	save := func(key string, itemID snowflake.ID, slot rpg.EquipmentSlotType) rpg.AdminEquipment {
		t.Helper()
		value, err := world.SaveEquipment(ctx, rpg.SaveEquipmentCommand{Write: rpg.AdminWriteContext{ActorAccountID: adminID, IdempotencyKey: key, RequestID: key + "-request"}, Value: rpg.AdminEquipment{ItemID: itemID, SellCurrencyID: currencyID, SlotType: slot, MinimumLevel: 1, Enabled: true}})
		if err != nil {
			t.Fatalf("SaveEquipment(%s) error = %v", key, err)
		}
		return value
	}
	firstEquipment := save("equipment-page-first", firstItemID, rpg.EquipmentSlotTypeHead)
	secondEquipment := save("equipment-page-second", secondItemID, rpg.EquipmentSlotTypeFeet)
	firstCatalogPage, err := world.ListEquipments(ctx, 1, "")
	if err != nil || len(firstCatalogPage.Items) != 1 || firstCatalogPage.NextCursor == "" {
		t.Fatalf("ListEquipments(first) = %+v, error = %v", firstCatalogPage, err)
	}
	secondCatalogPage, err := world.ListEquipments(ctx, 1, firstCatalogPage.NextCursor)
	if err != nil || len(secondCatalogPage.Items) != 1 || secondCatalogPage.Items[0].ID == firstCatalogPage.Items[0].ID || secondCatalogPage.Items[0].ID != secondEquipment.ID {
		t.Fatalf("ListEquipments(second) = %+v, error = %v; first equipment=%s", secondCatalogPage, err, firstEquipment.ID)
	}
	granted, err := world.GrantEquipment(ctx, rpg.GrantEquipmentCommand{Write: rpg.AdminWriteContext{ActorAccountID: adminID, IdempotencyKey: "equipment-page-grant", RequestID: "equipment-page-grant-request"}, PlayerCharacterID: playerID, EquipmentID: firstEquipment.ID, Quantity: 3, Reason: "验证装备分页", Now: now})
	if err != nil || len(granted.InstanceIDs) != 3 {
		t.Fatalf("GrantEquipment() = %+v, error = %v", granted, err)
	}
	countPage, err := world.ListEquipments(ctx, 100, "")
	if err != nil {
		t.Fatalf("ListEquipments(count) error = %v", err)
	}
	var grantedCount int64
	for _, equipment := range countPage.Items {
		if equipment.ID == firstEquipment.ID {
			grantedCount = equipment.InstanceCount
		}
	}
	if grantedCount != 3 {
		t.Fatalf("ListEquipments() InstanceCount = %d, want 3", grantedCount)
	}
	if _, err = world.ListAdminEquipmentInstances(ctx, rpg.AdminEquipmentInstanceQuery{PageSize: 20, SourceType: "legacy"}); !errors.Is(err, rpg.ErrInvalidEquipmentFilter) {
		t.Fatalf("非法来源筛选 error = %v", err)
	}
	if _, err = world.ListEquipmentTransactions(ctx, rpg.EquipmentTransactionQuery{PageSize: 20, Action: "delete"}); !errors.Is(err, rpg.ErrInvalidEquipmentFilter) {
		t.Fatalf("非法动作筛选 error = %v", err)
	}
	playerSeen := map[snowflake.ID]struct{}{}
	playerCursor := ""
	for range 3 {
		page, pageErr := world.ListEquipmentInstances(ctx, accountID, 1, playerCursor)
		if pageErr != nil || len(page.Items) != 1 {
			t.Fatalf("ListEquipmentInstances(cursor=%q) = %+v, error = %v", playerCursor, page, pageErr)
		}
		if _, duplicate := playerSeen[page.Items[0].ID]; duplicate {
			t.Fatalf("玩家装备分页重复实例 %s", page.Items[0].ID)
		}
		playerSeen[page.Items[0].ID], playerCursor = struct{}{}, page.NextCursor
	}
	if len(playerSeen) != 3 || playerCursor != "" {
		t.Fatalf("玩家装备分页结果 = %d, final cursor=%q", len(playerSeen), playerCursor)
	}
	instanceSeen := map[snowflake.ID]struct{}{}
	instanceCursor := ""
	for range 3 {
		page, pageErr := world.ListAdminEquipmentInstances(ctx, rpg.AdminEquipmentInstanceQuery{PageSize: 1, Cursor: instanceCursor, PlayerCharacterID: playerID, SourceType: "admin"})
		if pageErr != nil || len(page.Items) != 1 {
			t.Fatalf("ListAdminEquipmentInstances(cursor=%q) = %+v, error = %v", instanceCursor, page, pageErr)
		}
		if _, duplicate := instanceSeen[page.Items[0].ID]; duplicate {
			t.Fatalf("管理装备分页重复实例 %s", page.Items[0].ID)
		}
		instanceSeen[page.Items[0].ID], instanceCursor = struct{}{}, page.NextCursor
	}
	if len(instanceSeen) != 3 || instanceCursor != "" {
		t.Fatalf("管理装备分页结果 = %d, final cursor=%q", len(instanceSeen), instanceCursor)
	}
	firstInstancePage, err := world.ListAdminEquipmentInstances(ctx, rpg.AdminEquipmentInstanceQuery{PageSize: 1, PlayerCharacterID: playerID, SourceType: "admin"})
	if err != nil || firstInstancePage.NextCursor == "" {
		t.Fatalf("ListAdminEquipmentInstances(first) = %+v, error = %v", firstInstancePage, err)
	}
	if _, err = world.ListAdminEquipmentInstances(ctx, rpg.AdminEquipmentInstanceQuery{PageSize: 1, Cursor: firstInstancePage.NextCursor, PlayerCharacterID: playerID, SourceType: "shop"}); !errors.Is(err, rpg.ErrInvalidEquipmentCursor) {
		t.Fatalf("跨筛选实例游标 error = %v", err)
	}
	transactionSeen := map[snowflake.ID]struct{}{}
	transactionCursor := ""
	for range 3 {
		page, pageErr := world.ListEquipmentTransactions(ctx, rpg.EquipmentTransactionQuery{PageSize: 1, Cursor: transactionCursor, PlayerCharacterID: playerID, Action: "acquire"})
		if pageErr != nil || len(page.Items) != 1 {
			t.Fatalf("ListEquipmentTransactions(cursor=%q) = %+v, error = %v", transactionCursor, page, pageErr)
		}
		if _, duplicate := transactionSeen[page.Items[0].ID]; duplicate {
			t.Fatalf("装备流水分页重复记录 %s", page.Items[0].ID)
		}
		transactionSeen[page.Items[0].ID], transactionCursor = struct{}{}, page.NextCursor
	}
	if len(transactionSeen) != 3 || transactionCursor != "" {
		t.Fatalf("装备流水分页结果 = %d, final cursor=%q", len(transactionSeen), transactionCursor)
	}
}

// TestEquipmentCatalogRelationsKeepStableIdentifiers 验证完整保存不会替换仍然存在的职业与属性修正关系身份。
func TestEquipmentCatalogRelationsKeepStableIdentifiers(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startEncounterDatabase(t, ctx)
	_, _, adminID, itemID, currencyID, now := seedEquipmentFixture(t, ctx, pool)
	world := rpgpersistence.NewAdapters(pool, snowflake.NewTestID)
	professionID, statID := snowflake.NewTestID(), snowflake.NewTestID()
	if _, err := pool.Exec(ctx, `INSERT INTO rpg_profession (id, code, name, maximum_level, enabled, version, created_at, updated_at) VALUES ($1, 'stable-relation-profession', '稳定关系职业', 100, true, 1, $2, $2)`, professionID, now); err != nil {
		t.Fatalf("写入装备职业资料: %v", err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO game_stat (id, code, name, sort_order, battle_only, enabled, version, created_at, updated_at) VALUES ($1, 'stable-relation-stat', '稳定关系属性', 1, false, true, 1, $2, $2)`, statID, now); err != nil {
		t.Fatalf("写入装备属性资料: %v", err)
	}
	created, err := world.SaveEquipment(ctx, rpg.SaveEquipmentCommand{
		Write: rpg.AdminWriteContext{ActorAccountID: adminID, IdempotencyKey: "equipment-stable-relations-create", RequestID: "equipment-stable-relations-create-request"},
		Value: rpg.AdminEquipment{ItemID: itemID, SellCurrencyID: currencyID, SlotType: rpg.EquipmentSlotTypeHead, MinimumLevel: 1, Enabled: true, ProfessionIDs: []snowflake.ID{professionID}, StatModifiers: []rpg.AdminEquipmentStatModifier{{StatID: statID, FlatValue: 5, PercentageBPS: 100}}},
	})
	if err != nil {
		t.Fatalf("SaveEquipment(create) error = %v", err)
	}
	if _, err = world.SaveEquipment(ctx, rpg.SaveEquipmentCommand{
		Write: rpg.AdminWriteContext{ActorAccountID: adminID, IdempotencyKey: "equipment-duplicate-item", RequestID: "equipment-duplicate-item-request"},
		Value: rpg.AdminEquipment{ItemID: itemID, SellCurrencyID: currencyID, SlotType: rpg.EquipmentSlotTypeBody, MinimumLevel: 1},
	}); !errors.Is(err, rpg.ErrAdminWorldConflict) {
		t.Fatalf("同一 Item 重复建立 Equipment error = %v", err)
	}
	var professionRelationID, modifierRelationID snowflake.ID
	if err = pool.QueryRow(ctx, `SELECT id FROM game_equipment_profession WHERE equipment_id = $1 AND profession_id = $2`, created.ID, professionID).Scan(&professionRelationID); err != nil {
		t.Fatalf("读取职业关系: %v", err)
	}
	if err = pool.QueryRow(ctx, `SELECT id FROM game_equipment_stat_modifier WHERE equipment_id = $1 AND stat_id = $2`, created.ID, statID).Scan(&modifierRelationID); err != nil {
		t.Fatalf("读取属性修正关系: %v", err)
	}
	updated, err := world.SaveEquipment(ctx, rpg.SaveEquipmentCommand{
		Write:           rpg.AdminWriteContext{ActorAccountID: adminID, IdempotencyKey: "equipment-stable-relations-update", RequestID: "equipment-stable-relations-update-request"},
		Value:           rpg.AdminEquipment{ID: created.ID, ItemID: itemID, SellCurrencyID: currencyID, SlotType: rpg.EquipmentSlotTypeHead, MinimumLevel: 2, Enabled: true, ProfessionIDs: []snowflake.ID{professionID}, StatModifiers: []rpg.AdminEquipmentStatModifier{{StatID: statID, FlatValue: 8, PercentageBPS: 200}}},
		ExpectedVersion: created.Version,
	})
	if err != nil || updated.Version != created.Version+1 {
		t.Fatalf("SaveEquipment(update) = %+v, error = %v", updated, err)
	}
	var updatedProfessionRelationID, updatedModifierRelationID snowflake.ID
	var flatValue int64
	var percentageBPS int32
	if err = pool.QueryRow(ctx, `SELECT id FROM game_equipment_profession WHERE equipment_id = $1 AND profession_id = $2`, created.ID, professionID).Scan(&updatedProfessionRelationID); err != nil {
		t.Fatalf("读取更新后职业关系: %v", err)
	}
	if err = pool.QueryRow(ctx, `SELECT id, flat_value, percentage_bps FROM game_equipment_stat_modifier WHERE equipment_id = $1 AND stat_id = $2`, created.ID, statID).Scan(&updatedModifierRelationID, &flatValue, &percentageBPS); err != nil {
		t.Fatalf("读取更新后属性修正关系: %v", err)
	}
	if updatedProfessionRelationID != professionRelationID || updatedModifierRelationID != modifierRelationID || flatValue != 8 || percentageBPS != 200 {
		t.Fatalf("装备关系更新不稳定: profession %s/%s, modifier %s/%s, values=%d/%d", professionRelationID, updatedProfessionRelationID, modifierRelationID, updatedModifierRelationID, flatValue, percentageBPS)
	}
}

// TestEquipmentOptionsReturnOnlyEnabledReferences 验证管理表单引用集合不会暴露停用装备或停用道具。
func TestEquipmentOptionsReturnOnlyEnabledReferences(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startEncounterDatabase(t, ctx)
	_, _, adminID, itemID, currencyID, now := seedEquipmentFixture(t, ctx, pool)
	world := rpgpersistence.NewAdapters(pool, snowflake.NewTestID)
	active, err := world.SaveEquipment(ctx, rpg.SaveEquipmentCommand{Write: rpg.AdminWriteContext{ActorAccountID: adminID, IdempotencyKey: "equipment-options-active", RequestID: "equipment-options-active-request"}, Value: rpg.AdminEquipment{ItemID: itemID, SellCurrencyID: currencyID, SlotType: rpg.EquipmentSlotTypeHead, MinimumLevel: 1, Enabled: true}})
	if err != nil {
		t.Fatalf("SaveEquipment(active) error = %v", err)
	}
	secondItemID := snowflake.NewTestID()
	if _, err = pool.Exec(ctx, `INSERT INTO game_item (id, code, name, usage_type, cost, enabled, version, created_at, updated_at) VALUES ($1, 'disabled-equipment-option', '停用道具', 'equipment', 0, false, 1, $2, $2)`, secondItemID, now); err != nil {
		t.Fatalf("写入停用道具: %v", err)
	}
	if _, err = pool.Exec(ctx, `INSERT INTO game_equipment (id, item_id, slot_type, minimum_level, sell_currency_id, sell_price, rules, enabled, version, created_at, updated_at) VALUES ($1, $2, 'body', 1, $3, 0, '{}'::jsonb, true, 1, $4, $4)`, snowflake.NewTestID(), secondItemID, currencyID, now); err != nil {
		t.Fatalf("写入停用道具装备: %v", err)
	}
	options, err := world.ListEquipmentOptions(ctx)
	if err != nil || len(options) != 1 || options[0].ID != active.ID || options[0].ItemName != "铁盔" {
		t.Fatalf("ListEquipmentOptions() = %+v, error = %v", options, err)
	}
}

// TestEquipmentShopPurchasePaysAndDeliversOnce 验证购买重试只扣款一次，并稳定返回首次建立的独立实例。
func TestEquipmentShopPurchasePaysAndDeliversOnce(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startEncounterDatabase(t, ctx)
	accountID, playerID, adminID, itemID, currencyID, now := seedEquipmentFixture(t, ctx, pool)
	world := rpgpersistence.NewAdapters(pool, snowflake.NewTestID)
	saved, err := world.SaveEquipment(ctx, rpg.SaveEquipmentCommand{Write: rpg.AdminWriteContext{ActorAccountID: adminID, IdempotencyKey: "equipment-shop-save", RequestID: "equipment-shop-save-request"}, Value: rpg.AdminEquipment{ItemID: itemID, SellCurrencyID: currencyID, SlotType: rpg.EquipmentSlotTypeHead, MinimumLevel: 1, SellPrice: 5, Enabled: true}})
	if err != nil || !saved.ID.IsValid() {
		t.Fatalf("SaveEquipment() = %+v, error = %v", saved, err)
	}
	regionID, locationID, shopID, shopItemID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	statements := []struct {
		query string
		args  []any
	}{
		{`INSERT INTO rpg_region (id, code, name, enabled, version, created_at, updated_at) VALUES ($1, 'equipment-shop-region', '装备商店区域', true, 1, $2, $2)`, []any{regionID, now}},
		{`INSERT INTO rpg_location (id, region_id, code, name, location_type, default_spawn, enabled, version, created_at, updated_at) VALUES ($1, $2, 'equipment-shop-location', '装备商店', 'settlement', true, true, 1, $3, $3)`, []any{locationID, regionID, now}},
		{`INSERT INTO player_character_position (id, player_character_id, location_id, move_sequence, version, updated_at) VALUES ($1, $2, $3, 0, 1, $4)`, []any{snowflake.NewTestID(), playerID, locationID, now}},
		{`INSERT INTO rpg_shop (id, location_id, code, name, enabled, version, created_at, updated_at) VALUES ($1, $2, 'equipment-shop', '装备商店', true, 1, $3, $3)`, []any{shopID, locationID, now}},
		{`INSERT INTO rpg_shop_item (id, shop_id, item_id, currency_id, buy_price, enabled) VALUES ($1, $2, $3, $4, 30, true)`, []any{shopItemID, shopID, itemID, currencyID}},
		{`INSERT INTO player_character_wallet (id, player_character_id, currency_id, balance, version, updated_at) VALUES ($1, $2, $3, 100, 1, $4)`, []any{snowflake.NewTestID(), playerID, currencyID, now}},
	}
	for _, statement := range statements {
		if _, err = pool.Exec(ctx, statement.query, statement.args...); err != nil {
			t.Fatalf("写入 Equipment Shop 夹具: %v\nSQL: %s", err, statement.query)
		}
	}
	command := rpg.PurchaseShopItemCommand{AccountID: accountID, ShopItemID: shopItemID, Quantity: 2, IdempotencyKey: "equipment-shop-purchase", Now: now.Add(time.Second)}
	first, err := world.PurchaseShopItem(ctx, command)
	if err != nil || first.BalanceAfter != 40 || len(first.EquipmentInstanceIDs) != 2 || first.InventoryStack != nil {
		t.Fatalf("PurchaseShopItem() = %+v, error = %v", first, err)
	}
	replayed, err := world.PurchaseShopItem(ctx, command)
	if err != nil || replayed.OperationID != first.OperationID || len(replayed.EquipmentInstanceIDs) != 2 || replayed.EquipmentInstanceIDs[0] != first.EquipmentInstanceIDs[0] {
		t.Fatalf("PurchaseShopItem(replay) = %+v, error = %v", replayed, err)
	}
	var balance, purchases, paymentTransactions, instances int64
	checks := []struct {
		query  string
		args   []any
		target *int64
	}{
		{`SELECT balance FROM player_character_wallet WHERE player_character_id = $1 AND currency_id = $2`, []any{playerID, currencyID}, &balance},
		{`SELECT count(*) FROM player_character_shop_purchase WHERE operation_id = $1`, []any{first.OperationID}, &purchases},
		{`SELECT count(*) FROM player_character_currency_transaction WHERE reference_id = $1 AND amount_delta = -60`, []any{first.OperationID}, &paymentTransactions},
		{`SELECT count(*) FROM player_character_equipment_instance WHERE source_type = 'shop' AND source_reference_id IN (SELECT id FROM player_character_shop_purchase WHERE operation_id = $1)`, []any{first.OperationID}, &instances},
	}
	for _, check := range checks {
		if err = pool.QueryRow(ctx, check.query, check.args...).Scan(check.target); err != nil {
			t.Fatalf("读取 Equipment Shop 事实: %v", err)
		}
	}
	if balance != 40 || purchases != 1 || paymentTransactions != 1 || instances != 2 {
		t.Fatalf("购买事实 = balance %d, purchases %d, payments %d, instances %d", balance, purchases, paymentTransactions, instances)
	}
}

// TestShopMaintenancePreservesPurchasedItemIdentity 验证商品产生购买历史后仍可编辑，移除时保留稳定身份并禁用。
func TestShopMaintenancePreservesPurchasedItemIdentity(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startEncounterDatabase(t, ctx)
	_, playerID, adminID, itemID, currencyID, now := seedEquipmentFixture(t, ctx, pool)
	world := rpgpersistence.NewAdapters(pool, snowflake.NewTestID)
	regionID, locationID := snowflake.NewTestID(), snowflake.NewTestID()
	if _, err := pool.Exec(ctx, `INSERT INTO rpg_region (id, code, name, enabled, version, created_at, updated_at) VALUES ($1, 'shop-maintenance-region', '商店维护区域', true, 1, $2, $2)`, regionID, now); err != nil {
		t.Fatalf("写入商店维护夹具: %v", err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO rpg_location (id, region_id, code, name, location_type, default_spawn, enabled, version, created_at, updated_at) VALUES ($1, $2, 'shop-maintenance-location', '商店维护地点', 'settlement', false, true, 1, $3, $3)`, locationID, regionID, now); err != nil {
		t.Fatalf("写入商店维护地点夹具: %v", err)
	}
	created, err := world.SaveShop(ctx, rpg.SaveShopCommand{
		Write: rpg.AdminWriteContext{ActorAccountID: adminID, IdempotencyKey: "shop-maintenance-create", RequestID: "shop-maintenance-create-request"},
		Value: rpg.AdminShop{LocationID: locationID, Code: "shop-maintenance", Name: "商店维护", Enabled: true, Items: []rpg.AdminShopItem{{ItemID: itemID, CurrencyID: currencyID, BuyPrice: 10, Enabled: true}}},
	})
	if err != nil || len(created.Items) != 1 {
		t.Fatalf("SaveShop(create) = %+v, error = %v", created, err)
	}
	shopItemID := created.Items[0].ID
	if _, err = pool.Exec(ctx, `INSERT INTO player_character_shop_purchase (id, operation_id, player_character_id, shop_item_id, item_id, currency_id, quantity, unit_price, total_price, balance_after, created_at) VALUES ($1, $2, $3, $4, $5, $6, 1, 10, 10, 90, $7)`, snowflake.NewTestID(), snowflake.NewTestID(), playerID, shopItemID, itemID, currencyID, now); err != nil {
		t.Fatalf("写入购买事实: %v", err)
	}
	updated, err := world.SaveShop(ctx, rpg.SaveShopCommand{
		Write:           rpg.AdminWriteContext{ActorAccountID: adminID, IdempotencyKey: "shop-maintenance-update", RequestID: "shop-maintenance-update-request"},
		Value:           rpg.AdminShop{ID: created.ID, LocationID: locationID, Code: created.Code, Name: created.Name, Enabled: true, Items: []rpg.AdminShopItem{{ID: shopItemID, ItemID: itemID, CurrencyID: currencyID, BuyPrice: 20, Enabled: true}}},
		ExpectedVersion: created.Version,
	})
	if err != nil || len(updated.Items) != 1 || updated.Items[0].ID != shopItemID || updated.Items[0].BuyPrice != 20 {
		t.Fatalf("SaveShop(update) = %+v, error = %v", updated, err)
	}
	removed, err := world.SaveShop(ctx, rpg.SaveShopCommand{
		Write:           rpg.AdminWriteContext{ActorAccountID: adminID, IdempotencyKey: "shop-maintenance-remove", RequestID: "shop-maintenance-remove-request"},
		Value:           rpg.AdminShop{ID: created.ID, LocationID: locationID, Code: created.Code, Name: created.Name, Enabled: true},
		ExpectedVersion: updated.Version,
	})
	if err != nil || len(removed.Items) != 1 || removed.Items[0].ID != shopItemID || removed.Items[0].Enabled {
		t.Fatalf("SaveShop(remove referenced) = %+v, error = %v", removed, err)
	}
	var enabled bool
	if err = pool.QueryRow(ctx, `SELECT enabled FROM rpg_shop_item WHERE id = $1`, shopItemID).Scan(&enabled); err != nil || enabled {
		t.Fatalf("购买历史商品应保留并禁用: enabled=%t, error=%v", enabled, err)
	}
}

// TestReferencedRPGRelationsPreserveIdentityOnMaintenance 验证任务目标、遭遇候选和掉落候选
// 已被玩家历史事实引用后仍可原位编辑，并在移除时保留稳定身份、仅退出当前启用投影。
func TestReferencedRPGRelationsPreserveIdentityOnMaintenance(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startEncounterDatabase(t, ctx)
	encounter := seedEncounterBattleFixture(t, ctx, pool)
	_, playerID, adminID, _, _, now := seedEquipmentFixture(t, ctx, pool)
	world := rpgpersistence.NewAdapters(pool, snowflake.NewTestID)
	write := func(key string) rpg.AdminWriteContext {
		return rpg.AdminWriteContext{ActorAccountID: adminID, IdempotencyKey: key, RequestID: key + "-request"}
	}

	t.Run("Quest Objective", func(t *testing.T) {
		created, err := world.SaveQuest(ctx, rpg.SaveQuestCommand{
			Write: write("referenced-quest-create"),
			Value: rpg.AdminQuest{Code: "referenced-objective-quest", Name: "历史目标任务", QuestType: "side", Description: "验证历史任务目标可维护", Enabled: true, Objectives: []rpg.AdminQuestObjective{{Code: "referenced-objective", Position: 1, ObjectiveType: "battle", RequiredCount: 1, Description: "完成一次战斗"}}},
		})
		if err != nil || len(created.Objectives) != 1 {
			t.Fatalf("SaveQuest(create) = %+v, error = %v", created, err)
		}
		objectiveID := created.Objectives[0].ID
		questProgressID := snowflake.NewTestID()
		if _, err = pool.Exec(ctx, `INSERT INTO player_character_quest (id, player_character_id, quest_id, status, started_at, completion_count, version) VALUES ($1, $2, $3, 'active', $4, 0, 1)`, questProgressID, playerID, created.ID, now); err != nil {
			t.Fatalf("写入 Quest Progress: %v", err)
		}
		if _, err = pool.Exec(ctx, `INSERT INTO player_character_quest_objective (id, player_character_id, quest_id, objective_id, current_count) VALUES ($1, $2, $3, $4, 0)`, snowflake.NewTestID(), playerID, created.ID, objectiveID); err != nil {
			t.Fatalf("写入 Quest Objective Progress: %v", err)
		}
		updated, err := world.SaveQuest(ctx, rpg.SaveQuestCommand{Write: write("referenced-quest-update"), Value: rpg.AdminQuest{ID: created.ID, Code: created.Code, Name: created.Name, QuestType: created.QuestType, Description: created.Description, Enabled: true, Objectives: []rpg.AdminQuestObjective{{ID: objectiveID, Code: "referenced-objective", Position: 1, ObjectiveType: "battle", RequiredCount: 2, Description: "完成两次战斗"}}}, ExpectedVersion: created.Version})
		if err != nil || len(updated.Objectives) != 1 || updated.Objectives[0].ID != objectiveID || updated.Objectives[0].RequiredCount != 2 {
			t.Fatalf("SaveQuest(update) = %+v, error = %v", updated, err)
		}
		removed, err := world.SaveQuest(ctx, rpg.SaveQuestCommand{Write: write("referenced-quest-remove"), Value: rpg.AdminQuest{ID: created.ID, Code: created.Code, Name: created.Name, QuestType: created.QuestType, Description: created.Description, Enabled: true}, ExpectedVersion: updated.Version})
		if err != nil || len(removed.Objectives) != 0 {
			t.Fatalf("SaveQuest(remove) = %+v, error = %v", removed, err)
		}
		assertDisabledRelation(t, ctx, pool, "rpg_quest_objective", objectiveID)
		quests, err := world.ListQuests(ctx, 200)
		if err != nil || relationCount(quests, created.ID, func(value rpg.AdminQuest) int { return len(value.Objectives) }) != 0 {
			t.Fatalf("ListQuests() 仍回显已移除目标: %+v, error = %v", quests, err)
		}
	})

	t.Run("Encounter Entry", func(t *testing.T) {
		tables, err := world.ListEncounterTables(ctx, 200)
		if err != nil {
			t.Fatalf("ListEncounterTables() error = %v", err)
		}
		var table rpg.AdminEncounterTable
		for _, candidate := range tables {
			if candidate.Code == "encounter-table" {
				table = candidate
				break
			}
		}
		if !table.ID.IsValid() || len(table.Entries) != 1 {
			t.Fatalf("Encounter Table 夹具 = %+v", table)
		}
		entryID := table.Entries[0].ID
		table.Entries[0].Weight = 7
		updated, err := world.UpdateEncounterTable(ctx, rpg.SaveEncounterTableCommand{Write: write("referenced-encounter-update"), Table: table, ExpectedVersion: table.Version})
		if err != nil || len(updated.Entries) != 1 || updated.Entries[0].ID != entryID || updated.Entries[0].Weight != 7 {
			t.Fatalf("UpdateEncounterTable(update) = %+v, error = %v", updated, err)
		}
		updated.Entries = nil
		removed, err := world.UpdateEncounterTable(ctx, rpg.SaveEncounterTableCommand{Write: write("referenced-encounter-remove"), Table: updated, ExpectedVersion: updated.Version})
		if err != nil || len(removed.Entries) != 0 {
			t.Fatalf("UpdateEncounterTable(remove) = %+v, error = %v", removed, err)
		}
		assertDisabledRelation(t, ctx, pool, "rpg_encounter_entry", entryID)
		tables, err = world.ListEncounterTables(ctx, 200)
		if err != nil || relationCount(tables, table.ID, func(value rpg.AdminEncounterTable) int { return len(value.Entries) }) != 0 {
			t.Fatalf("ListEncounterTables() 仍回显已移除候选: %+v, error = %v", tables, err)
		}
	})

	t.Run("Loot Entry", func(t *testing.T) {
		settlementID := snowflake.NewTestID()
		if _, err := pool.Exec(ctx, `INSERT INTO player_character_loot_settlement (id, player_character_id, loot_table_id, source_type, source_reference_id, state, random_algorithm, random_trace, created_at) VALUES ($1, $2, $3, 'world', $4, 'pending', 'hmac-sha256-v1', '{}'::jsonb, $5)`, settlementID, playerID, encounter.lootTableID, snowflake.NewTestID(), now); err != nil {
			t.Fatalf("写入 Loot Settlement: %v", err)
		}
		if _, err := pool.Exec(ctx, `INSERT INTO player_character_loot_settlement_entry (id, loot_settlement_id, loot_entry_id, item_id, quantity) VALUES ($1, $2, $3, $4, 1)`, snowflake.NewTestID(), settlementID, encounter.lootEntryID, encounter.lootItemID); err != nil {
			t.Fatalf("写入 Loot Settlement Entry: %v", err)
		}
		tables, err := world.ListLootTables(ctx, 200)
		if err != nil {
			t.Fatalf("ListLootTables() error = %v", err)
		}
		var table rpg.AdminLootTable
		for _, candidate := range tables {
			if candidate.ID == encounter.lootTableID {
				table = candidate
				break
			}
		}
		if len(table.Entries) != 1 || table.Entries[0].ID != encounter.lootEntryID {
			t.Fatalf("Loot Table 夹具 = %+v", table)
		}
		table.Entries[0].MaximumQuantity = 5
		updated, err := world.SaveLootTable(ctx, rpg.SaveLootTableCommand{Write: write("referenced-loot-update"), Value: table, ExpectedVersion: table.Version})
		if err != nil || len(updated.Entries) != 1 || updated.Entries[0].ID != encounter.lootEntryID || updated.Entries[0].MaximumQuantity != 5 {
			t.Fatalf("SaveLootTable(update) = %+v, error = %v", updated, err)
		}
		updated.Entries = nil
		removed, err := world.SaveLootTable(ctx, rpg.SaveLootTableCommand{Write: write("referenced-loot-remove"), Value: updated, ExpectedVersion: updated.Version})
		if err != nil || len(removed.Entries) != 0 {
			t.Fatalf("SaveLootTable(remove) = %+v, error = %v", removed, err)
		}
		assertDisabledRelation(t, ctx, pool, "rpg_loot_entry", encounter.lootEntryID)
		tables, err = world.ListLootTables(ctx, 200)
		if err != nil || relationCount(tables, table.ID, func(value rpg.AdminLootTable) int { return len(value.Entries) }) != 0 {
			t.Fatalf("ListLootTables() 仍回显已移除候选: %+v, error = %v", tables, err)
		}
	})
}

func assertDisabledRelation(t *testing.T, ctx context.Context, pool *database.Pool, table string, id snowflake.ID) {
	t.Helper()
	var enabled bool
	if err := pool.QueryRow(ctx, `SELECT enabled FROM `+table+` WHERE id = $1`, id).Scan(&enabled); err != nil || enabled {
		t.Fatalf("%s 历史关系应保留并禁用: enabled=%t, error=%v", table, enabled, err)
	}
}

func relationCount[T any](values []T, id snowflake.ID, count func(T) int) int {
	for _, value := range values {
		var candidate snowflake.ID
		switch typed := any(value).(type) {
		case rpg.AdminQuest:
			candidate = typed.ID
		case rpg.AdminEncounterTable:
			candidate = typed.ID
		case rpg.AdminLootTable:
			candidate = typed.ID
		}
		if candidate == id {
			return count(value)
		}
	}
	return -1
}

// TestQuestLifecycleStartsCompletesClaimsAndRepeats 验证任务从开始、目标达成、交付、领奖到重复开始的完整事务闭环。
func TestQuestLifecycleStartsCompletesClaimsAndRepeats(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startEncounterDatabase(t, ctx)
	accountID, playerID, _, _, currencyID, now := seedEquipmentFixture(t, ctx, pool)
	world := rpgpersistence.NewAdapters(pool, snowflake.NewTestID)
	regionID, startLocationID, wrongLocationID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	npcID, questID, objectiveID, rewardID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	statements := []struct {
		query string
		args  []any
	}{
		{`INSERT INTO rpg_region (id, code, name, enabled, version, created_at, updated_at) VALUES ($1, 'quest-region', '任务区域', true, 1, $2, $2)`, []any{regionID, now}},
		{`INSERT INTO rpg_location (id, region_id, code, name, location_type, default_spawn, enabled, version, created_at, updated_at) VALUES ($1, $3, 'quest-start', '任务地点', 'settlement', false, true, 1, $4, $4), ($2, $3, 'quest-wrong', '错误地点', 'settlement', false, true, 1, $4, $4)`, []any{startLocationID, wrongLocationID, regionID, now}},
		{`INSERT INTO rpg_npc (id, location_id, code, name, npc_type, description, enabled, version, created_at, updated_at) VALUES ($1, $2, 'quest-giver', '任务 NPC', 'story', '任务生命周期测试 NPC', true, 1, $3, $3)`, []any{npcID, startLocationID, now}},
		{`INSERT INTO rpg_quest (id, code, name, quest_type, start_npc_id, turn_in_npc_id, description, repeatable, enabled, version, created_at, updated_at) VALUES ($1, 'repeatable-quest', '可重复任务', 'side', $2, $2, '验证任务完整生命周期', true, true, 1, $3, $3)`, []any{questID, npcID, now}},
		{`INSERT INTO rpg_quest_objective (id, quest_id, code, position, objective_type, required_count, description) VALUES ($1, $2, 'quest-arrive', 1, 'explore', 1, '完成一次探索')`, []any{objectiveID, questID}},
		{`INSERT INTO rpg_quest_reward (id, quest_id, currency_id, quantity) VALUES ($1, $2, $3, 15)`, []any{rewardID, questID, currencyID}},
		{`INSERT INTO player_character_position (id, player_character_id, location_id, move_sequence, version, updated_at) VALUES ($1, $2, $3, 0, 1, $4)`, []any{snowflake.NewTestID(), playerID, startLocationID, now}},
	}
	for _, statement := range statements {
		if _, err := pool.Exec(ctx, statement.query, statement.args...); err != nil {
			t.Fatalf("写入 Quest 生命周期夹具: %v\nSQL: %s", err, statement.query)
		}
	}
	available, err := world.ListAvailableQuests(ctx, accountID)
	if err != nil || len(available) != 1 || available[0].QuestID != questID {
		t.Fatalf("ListAvailableQuests() = %+v, error = %v", available, err)
	}
	started, err := world.StartQuest(ctx, rpg.StartQuestCommand{AccountID: accountID, QuestID: questID, IdempotencyKey: "quest-start-first", Now: now.Add(time.Second)})
	if err != nil || started.Status != "active" || started.Version != 1 || len(started.Objectives) != 1 || started.Objectives[0].CurrentCount != 0 {
		t.Fatalf("StartQuest() = %+v, error = %v", started, err)
	}
	if _, err = world.CompleteQuest(ctx, rpg.CompleteQuestCommand{AccountID: accountID, QuestID: questID, ExpectedVersion: started.Version, IdempotencyKey: "quest-complete-incomplete", Now: now.Add(2 * time.Second)}); !errors.Is(err, rpg.ErrQuestObjectivesIncomplete) {
		t.Fatalf("CompleteQuest(incomplete) error = %v", err)
	}
	if _, err = pool.Exec(ctx, `UPDATE player_character_quest_objective SET current_count = 1, completed_at = $1 WHERE player_character_id = $2 AND quest_id = $3`, now.Add(3*time.Second), playerID, questID); err != nil {
		t.Fatalf("更新 Quest Objective: %v", err)
	}
	if _, err = pool.Exec(ctx, `UPDATE player_character_position SET location_id = $1 WHERE player_character_id = $2`, wrongLocationID, playerID); err != nil {
		t.Fatalf("更新 Quest 交付位置: %v", err)
	}
	if _, err = world.CompleteQuest(ctx, rpg.CompleteQuestCommand{AccountID: accountID, QuestID: questID, ExpectedVersion: started.Version, IdempotencyKey: "quest-complete-wrong-location", Now: now.Add(4 * time.Second)}); !errors.Is(err, rpg.ErrQuestUnavailable) {
		t.Fatalf("CompleteQuest(wrong location) error = %v", err)
	}
	if _, err = pool.Exec(ctx, `UPDATE player_character_position SET location_id = $1 WHERE player_character_id = $2`, startLocationID, playerID); err != nil {
		t.Fatal(err)
	}
	completed, err := world.CompleteQuest(ctx, rpg.CompleteQuestCommand{AccountID: accountID, QuestID: questID, ExpectedVersion: started.Version, IdempotencyKey: "quest-complete-first", Now: now.Add(5 * time.Second)})
	if err != nil || completed.Status != "completed" || completed.CompletionCount != 1 || completed.Version != 2 {
		t.Fatalf("CompleteQuest() = %+v, error = %v", completed, err)
	}
	claimed, err := world.ClaimQuestRewards(ctx, rpg.ClaimQuestRewardsCommand{AccountID: accountID, QuestID: questID, ExpectedProgressVersion: completed.Version, IdempotencyKey: "quest-claim-first", Now: now.Add(6 * time.Second)})
	if err != nil || len(claimed.CurrencyBalances) != 1 || claimed.CurrencyBalances[0].BalanceAfter != 15 {
		t.Fatalf("ClaimQuestRewards() = %+v, error = %v", claimed, err)
	}
	restarted, err := world.StartQuest(ctx, rpg.StartQuestCommand{AccountID: accountID, QuestID: questID, IdempotencyKey: "quest-start-second", Now: now.Add(7 * time.Second)})
	if err != nil || restarted.Status != "active" || restarted.CompletionCount != 1 || restarted.Version != 3 || restarted.Objectives[0].CurrentCount != 0 || restarted.Objectives[0].CompletedAt != nil {
		t.Fatalf("StartQuest(repeat) = %+v, error = %v", restarted, err)
	}
}

// TestProfessionChangeRejectsInvalidEquippedLoadout 验证职业切换不会留下与新职业集合不兼容的已穿装备。
func TestProfessionChangeRejectsInvalidEquippedLoadout(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startEncounterDatabase(t, ctx)
	accountID, playerID, adminID, itemID, currencyID, now := seedEquipmentFixture(t, ctx, pool)
	world := rpgpersistence.NewAdapters(pool, snowflake.NewTestID)
	warriorID, mageID := snowflake.NewTestID(), snowflake.NewTestID()
	if _, err := pool.Exec(ctx, `INSERT INTO rpg_profession (id, code, name, maximum_level, enabled, version, created_at, updated_at) VALUES ($1, 'equipment-warrior', '战士', 100, true, 1, $3, $3), ($2, 'equipment-mage', '法师', 100, true, 1, $3, $3)`, warriorID, mageID, now); err != nil {
		t.Fatalf("写入职业资料: %v", err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO player_character_profession (id, player_character_id, profession_id, level, experience, active, version, updated_at) VALUES ($1, $3, $4, 10, 0, true, 1, $5), ($2, $3, $6, 10, 0, false, 1, $5)`, snowflake.NewTestID(), snowflake.NewTestID(), playerID, warriorID, now, mageID); err != nil {
		t.Fatalf("写入角色职业: %v", err)
	}
	saved, err := world.SaveEquipment(ctx, rpg.SaveEquipmentCommand{Write: rpg.AdminWriteContext{ActorAccountID: adminID, IdempotencyKey: "profession-equipment-save", RequestID: "profession-equipment-request"}, Value: rpg.AdminEquipment{ItemID: itemID, SellCurrencyID: currencyID, SlotType: rpg.EquipmentSlotTypeHead, MinimumLevel: 1, SellPrice: 0, Enabled: true, ProfessionIDs: []snowflake.ID{warriorID}}})
	if err != nil {
		t.Fatalf("SaveEquipment() error = %v", err)
	}
	granted, err := world.GrantEquipment(ctx, rpg.GrantEquipmentCommand{Write: rpg.AdminWriteContext{ActorAccountID: adminID, IdempotencyKey: "profession-equipment-grant", RequestID: "profession-equipment-grant-request"}, PlayerCharacterID: playerID, EquipmentID: saved.ID, Quantity: 1, Reason: "职业集成测试", Now: now})
	if err != nil {
		t.Fatalf("GrantEquipment() error = %v", err)
	}
	if _, err = world.ReplaceEquipmentLoadout(ctx, rpg.ReplaceEquipmentLoadoutCommand{AccountID: accountID, Entries: []rpg.EquipmentLoadoutEntry{{Slot: rpg.EquipmentSlotHead, InstanceID: granted.InstanceIDs[0]}}, ExpectedVersion: 1, IdempotencyKey: "profession-equipment-equip", Now: now.Add(time.Second)}); err != nil {
		t.Fatalf("ReplaceEquipmentLoadout() error = %v", err)
	}
	_, err = world.ReplaceActiveProfessions(ctx, rpg.ReplaceActiveProfessionsCommand{AccountID: accountID, ProfessionIDs: []snowflake.ID{mageID}, IdempotencyKey: "replace-profession-invalid", Now: now.Add(2 * time.Second)})
	if !errors.Is(err, rpg.ErrEquipmentRequirementNotMet) {
		t.Fatalf("ReplaceActiveProfessions() error = %v", err)
	}
	var warriorActive, mageActive bool
	if err = pool.QueryRow(ctx, `SELECT active FROM player_character_profession WHERE player_character_id = $1 AND profession_id = $2`, playerID, warriorID).Scan(&warriorActive); err != nil {
		t.Fatal(err)
	}
	if err = pool.QueryRow(ctx, `SELECT active FROM player_character_profession WHERE player_character_id = $1 AND profession_id = $2`, playerID, mageID).Scan(&mageActive); err != nil {
		t.Fatal(err)
	}
	if !warriorActive || mageActive {
		t.Fatalf("职业切换回滚失败: warrior=%t mage=%t", warriorActive, mageActive)
	}
}

func seedEquipmentFixture(t *testing.T, ctx context.Context, pool *database.Pool) (snowflake.ID, snowflake.ID, snowflake.ID, snowflake.ID, snowflake.ID, time.Time) {
	t.Helper()
	accountID, playerID, adminID, itemID, currencyID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	now := time.Date(2026, time.August, 13, 12, 0, 0, 0, time.UTC)
	statements := []struct {
		query string
		args  []any
	}{
		{`INSERT INTO audit_hash_chain_state (id, ledger, latest_hash, updated_at) VALUES ($1, 'admin_audit_log', ''::bytea, $2)`, []any{snowflake.NewTestID(), now}},
		{`INSERT INTO account (id, username, username_key, display_name, password_hash, password_algorithm, password_parameters, status, security_version, created_at, updated_at) VALUES ($1, 'equipment-owner', 'equipment-owner', '装备玩家', 'test', 'argon2id', '{}', 'active', 1, $2, $2)`, []any{accountID, now}},
		{`INSERT INTO player_character (id, account_id, display_name, display_name_key, level, version, created_at, updated_at) VALUES ($1, $2, '装备角色', '装备角色', 10, 1, $3, $3)`, []any{playerID, accountID, now}},
		{`INSERT INTO active_player_character (account_id, player_character_id, version, updated_at) VALUES ($1, $2, 1, $3)`, []any{accountID, playerID, now}},
		{`INSERT INTO admin_account (id, username, username_key, display_name, password_hash, password_algorithm, password_parameters, status, version, created_at, updated_at) VALUES ($1, 'equipment-admin', 'equipment-admin', '装备管理员', 'test', 'argon2id', '{}', 'active', 1, $2, $2)`, []any{adminID, now}},
		{`INSERT INTO game_currency (id, code, name, enabled, version, created_at, updated_at) VALUES ($1, 'gold', '金币', true, 1, $2, $2)`, []any{currencyID, now}},
		{`INSERT INTO game_item (id, code, name, usage_type, cost, enabled, version, created_at, updated_at) VALUES ($1, 'iron-helmet', '铁盔', 'equipment', 0, true, 1, $2, $2)`, []any{itemID, now}},
	}
	for _, statement := range statements {
		if _, err := pool.Exec(ctx, statement.query, statement.args...); err != nil {
			t.Fatalf("写入 Equipment 集成夹具: %v", err)
		}
	}
	return accountID, playerID, adminID, itemID, currencyID, now
}
