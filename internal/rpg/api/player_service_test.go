package api

import (
	"context"
	"testing"
	"time"

	rpgv1 "github.com/lishangbu/avalon/api/gen/go/avalon/rpg/v1"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/rpg"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

type worldStub struct {
	mapValue               rpg.WorldMap
	traverseValue          rpg.TraversalResult
	pendingValue           *rpg.PendingEncounter
	resolveValue           rpg.PendingEncounter
	inventoryValue         rpg.Inventory
	heldItemValue          rpg.OwnedCreatureHeldItem
	replaceHeldItemCommand rpg.ReplaceHeldItemCommand
	purchaseValue          rpg.ItemAcquisitionResult
	purchaseCommand        rpg.PurchaseShopItemCommand
	questRewardValue       rpg.RewardAcquisitionResult
	questRewardCommand     rpg.ClaimQuestRewardsCommand
	questProgress          []rpg.QuestProgress
	startQuestCommand      rpg.StartQuestCommand
	completeQuestCommand   rpg.CompleteQuestCommand
	lootRewardValue        rpg.RewardAcquisitionResult
	lootRewardCommand      rpg.ClaimLootSettlementCommand
	activeProfessions      []rpg.ActiveProfession
	replaceProfessionCmd   rpg.ReplaceActiveProfessionsCommand
	equipmentPage          rpg.EquipmentInstancePage
	equipmentPageSize      int
	equipmentCursor        string
}

func (stub *worldStub) GetMap(context.Context, snowflake.ID) (rpg.WorldMap, error) {
	return stub.mapValue, nil
}
func (stub *worldStub) Traverse(context.Context, rpg.TraversalCommand) (rpg.TraversalResult, error) {
	return stub.traverseValue, nil
}
func (stub *worldStub) GetPendingEncounter(context.Context, snowflake.ID, time.Time) (*rpg.PendingEncounter, error) {
	return stub.pendingValue, nil
}
func (stub *worldStub) ResolvePendingEncounter(context.Context, rpg.ResolveEncounterCommand) (rpg.PendingEncounter, error) {
	return stub.resolveValue, nil
}
func (*worldStub) GetCheckpoint(context.Context, snowflake.ID) (*rpg.Checkpoint, error) {
	return nil, nil
}
func (*worldStub) SetCheckpoint(context.Context, rpg.SetCheckpointCommand) (rpg.Checkpoint, error) {
	return rpg.Checkpoint{}, nil
}
func (*worldStub) GetParty(context.Context, snowflake.ID) (rpg.Party, error) { return rpg.Party{}, nil }
func (*worldStub) ReplaceParty(context.Context, rpg.ReplacePartyCommand) (rpg.Party, error) {
	return rpg.Party{}, nil
}
func (stub *worldStub) GetInventory(context.Context, snowflake.ID) (rpg.Inventory, error) {
	return stub.inventoryValue, nil
}
func (stub *worldStub) ReplaceHeldItem(_ context.Context, command rpg.ReplaceHeldItemCommand) (rpg.OwnedCreatureHeldItem, error) {
	stub.replaceHeldItemCommand = command
	return stub.heldItemValue, nil
}
func (stub *worldStub) ListEquipmentInstances(_ context.Context, _ snowflake.ID, pageSize int, cursor string) (rpg.EquipmentInstancePage, error) {
	stub.equipmentPageSize, stub.equipmentCursor = pageSize, cursor
	return stub.equipmentPage, nil
}
func (*worldStub) GetEquipmentInstance(context.Context, snowflake.ID, snowflake.ID) (rpg.EquipmentInstance, error) {
	return rpg.EquipmentInstance{}, nil
}
func (*worldStub) GetEquipmentLoadout(context.Context, snowflake.ID) (rpg.EquipmentLoadout, error) {
	return rpg.EquipmentLoadout{Version: 1, UpdatedAt: time.Now()}, nil
}
func (*worldStub) ReplaceEquipmentLoadout(context.Context, rpg.ReplaceEquipmentLoadoutCommand) (rpg.EquipmentLoadout, error) {
	return rpg.EquipmentLoadout{Version: 1, UpdatedAt: time.Now()}, nil
}
func (*worldStub) SellEquipmentInstance(context.Context, rpg.SellEquipmentCommand) (rpg.SellEquipmentResult, error) {
	return rpg.SellEquipmentResult{}, nil
}
func (stub *worldStub) PurchaseShopItem(_ context.Context, command rpg.PurchaseShopItemCommand) (rpg.ItemAcquisitionResult, error) {
	stub.purchaseCommand = command
	return stub.purchaseValue, nil
}
func (*worldStub) ListAvailableQuests(context.Context, snowflake.ID) ([]rpg.AvailableQuest, error) {
	return nil, nil
}
func (stub *worldStub) ListQuestProgress(context.Context, snowflake.ID) ([]rpg.QuestProgress, error) {
	return stub.questProgress, nil
}
func (stub *worldStub) StartQuest(_ context.Context, command rpg.StartQuestCommand) (rpg.QuestProgress, error) {
	stub.startQuestCommand = command
	return stub.questProgress[0], nil
}
func (stub *worldStub) CompleteQuest(_ context.Context, command rpg.CompleteQuestCommand) (rpg.QuestProgress, error) {
	stub.completeQuestCommand = command
	return stub.questProgress[0], nil
}
func (stub *worldStub) ClaimQuestRewards(_ context.Context, command rpg.ClaimQuestRewardsCommand) (rpg.RewardAcquisitionResult, error) {
	stub.questRewardCommand = command
	return stub.questRewardValue, nil
}
func (stub *worldStub) ClaimLootSettlement(_ context.Context, command rpg.ClaimLootSettlementCommand) (rpg.RewardAcquisitionResult, error) {
	stub.lootRewardCommand = command
	return stub.lootRewardValue, nil
}
func (stub *worldStub) GetActiveProfessions(context.Context, snowflake.ID) ([]rpg.ActiveProfession, error) {
	return stub.activeProfessions, nil
}
func (stub *worldStub) ReplaceActiveProfessions(_ context.Context, command rpg.ReplaceActiveProfessionsCommand) ([]rpg.ActiveProfession, error) {
	stub.replaceProfessionCmd = command
	return stub.activeProfessions, nil
}

// TestListEquipmentInstancesMapsCursorPage 验证玩家装备实例 RPC 透传游标并返回下一页锚点。
func TestListEquipmentInstancesMapsCursorPage(t *testing.T) {
	accountID, instanceID, equipmentID, itemID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	stub := &worldStub{equipmentPage: rpg.EquipmentInstancePage{Items: []rpg.EquipmentInstance{{ID: instanceID, EquipmentID: equipmentID, ItemID: itemID, Name: "铁剑", SlotType: rpg.EquipmentSlotTypeMainHand, SourceType: "shop", Version: 1, MinimumLevel: 1, AcquiredAt: time.Now().UTC()}}, NextCursor: "next-equipment-page"}}
	service := NewPlayerService(stub, time.Now)
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})
	response, err := service.ListEquipmentInstances(ctx, &rpgv1.ListEquipmentInstancesRequest{PageSize: 25, Cursor: "current-equipment-page"})
	if err != nil {
		t.Fatalf("ListEquipmentInstances() error = %v", err)
	}
	if stub.equipmentPageSize != 25 || stub.equipmentCursor != "current-equipment-page" || response.GetNextCursor() != "next-equipment-page" || len(response.GetInstances()) != 1 || response.GetInstances()[0].GetId() != instanceID.String() {
		t.Fatalf("ListEquipmentInstances() request=%d/%q response=%+v", stub.equipmentPageSize, stub.equipmentCursor, response)
	}
}

// TestCompleteQuestMapsVersionAndProgress 验证任务完成 RPC 保留并发版本、幂等键和完整目标投影。
func TestCompleteQuestMapsVersionAndProgress(t *testing.T) {
	accountID, questID, objectiveID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	now := time.Date(2026, time.August, 14, 1, 0, 0, 0, time.UTC)
	stub := &worldStub{questProgress: []rpg.QuestProgress{{QuestID: questID, Code: "first-quest", Name: "第一个任务", Status: "completed", CompletionCount: 1, Version: 2, StartedAt: now.Add(-time.Hour), CompletedAt: &now, Objectives: []rpg.QuestObjectiveProgress{{ObjectiveID: objectiveID, Code: "arrive", ObjectiveType: "explore", CurrentCount: 1, RequiredCount: 1, CompletedAt: &now}}}}}
	service := NewPlayerService(stub, func() time.Time { return now })
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})
	response, err := service.CompleteQuest(ctx, &rpgv1.CompleteQuestRequest{QuestId: questID.String(), ExpectedVersion: 1, IdempotencyKey: "complete-first-quest"})
	if err != nil {
		t.Fatal(err)
	}
	if stub.completeQuestCommand.AccountID != accountID || stub.completeQuestCommand.QuestID != questID || stub.completeQuestCommand.ExpectedVersion != 1 || stub.completeQuestCommand.IdempotencyKey != "complete-first-quest" {
		t.Fatalf("CompleteQuest command = %+v", stub.completeQuestCommand)
	}
	if response.GetQuest().GetQuestId() != questID.String() || response.GetQuest().GetCompletedAt() == nil || len(response.GetQuest().GetObjectives()) != 1 {
		t.Fatalf("CompleteQuest response = %+v", response)
	}
}

func TestGetMapReturnsPositionAndDiscoverySubgraph(t *testing.T) {
	accountID := snowflake.MustParse("1048576199")
	locationID := snowflake.MustParse("1048576200")
	stub := &worldStub{mapValue: rpg.WorldMap{Position: rpg.Position{LocationID: locationID, Version: 1}, Locations: []rpg.WorldLocation{{ID: locationID, RegionID: accountID, Code: "spawn", Name: "出生点"}}}}
	service := NewPlayerService(stub, time.Now)
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})
	response, err := service.GetMap(ctx, &rpgv1.GetMapRequest{PageSize: 20})
	if err != nil {
		t.Fatal(err)
	}
	if response.GetPosition().GetLocationId() != locationID.String() || len(response.GetLocations()) != 1 {
		t.Fatalf("unexpected map response: %v", response)
	}
}

// TestGetInventoryPreservesHeldItemView 验证聚合背包与 Owned Creature 携带事实不会在 RPC 映射时丢失。
func TestGetInventoryPreservesHeldItemView(t *testing.T) {
	accountID := snowflake.MustParse("1048576199")
	itemID := snowflake.MustParse("1048576200")
	ownedID := snowflake.MustParse("1048576201")
	creatureID := snowflake.MustParse("1048576202")
	name := "剩饭"
	stub := &worldStub{inventoryValue: rpg.Inventory{
		Items:          []rpg.InventoryItem{{ItemID: itemID, ItemName: name, UsageType: "held", Quantity: 2, Version: 3}},
		OwnedCreatures: []rpg.OwnedCreatureHeldItem{{PlayerCharacterCreatureID: ownedID, CreatureID: creatureID, HeldItemID: itemID, HeldItemName: name, Version: 7}},
	}}
	service := NewPlayerService(stub, time.Now)
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})
	response, err := service.GetInventory(ctx, &rpgv1.GetInventoryRequest{})
	if err != nil {
		t.Fatal(err)
	}
	if response.GetItems()[0].GetQuantity() != 2 || response.GetOwnedCreatures()[0].GetHeldItemId() != itemID.String() {
		t.Fatalf("unexpected inventory response: %v", response)
	}
}

// TestReplaceHeldItemMapsOptionalItem 验证缺省 item_id 明确映射为卸下命令，而不是零值道具身份。
func TestReplaceHeldItemMapsOptionalItem(t *testing.T) {
	accountID := snowflake.MustParse("1048576199")
	ownedID := snowflake.MustParse("1048576201")
	stub := &worldStub{heldItemValue: rpg.OwnedCreatureHeldItem{PlayerCharacterCreatureID: ownedID, CreatureID: snowflake.MustParse("1048576202"), Version: 8}}
	service := NewPlayerService(stub, time.Now)
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})
	_, err := service.ReplaceHeldItem(ctx, &rpgv1.ReplaceHeldItemRequest{PlayerCharacterCreatureId: ownedID.String(), ExpectedCreatureVersion: 7, IdempotencyKey: "unequip-held-1"})
	if err != nil {
		t.Fatal(err)
	}
	if stub.replaceHeldItemCommand.ItemID != nil || stub.replaceHeldItemCommand.OwnedCreatureID != ownedID {
		t.Fatalf("unexpected command: %+v", stub.replaceHeldItemCommand)
	}
}

func TestResolvePendingEncounterReturnsCreatedBattleID(t *testing.T) {
	accountID := snowflake.MustParse("1048576199")
	encounterID := snowflake.MustParse("1048576201")
	entryID := snowflake.MustParse("1048576202")
	battleID := snowflake.MustParse("1048576203")
	stub := &worldStub{resolveValue: rpg.PendingEncounter{
		ID: encounterID, EncounterEntryID: entryID, BattleID: battleID,
		State: "accepted", ExpiresAt: time.Date(2026, time.August, 11, 12, 30, 0, 0, time.UTC),
	}}
	service := NewPlayerService(stub, time.Now)
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})
	response, err := service.ResolvePendingEncounter(ctx, &rpgv1.ResolvePendingEncounterRequest{
		PendingEncounterId: encounterID.String(),
		Resolution:         rpgv1.PendingEncounterResolution_PENDING_ENCOUNTER_RESOLUTION_ACCEPT,
		IdempotencyKey:     "accept-encounter-1",
	})
	if err != nil {
		t.Fatal(err)
	}
	if response.GetPendingEncounter().GetBattleId() != battleID.String() {
		t.Fatalf("battle_id = %q, want %q", response.GetPendingEncounter().GetBattleId(), battleID)
	}
}

// TestPurchaseShopItemMapsEquipmentInstances 验证购买响应保留独立实例身份，且不伪造普通背包结果。
func TestPurchaseShopItemMapsEquipmentInstances(t *testing.T) {
	accountID := snowflake.MustParse("1048576199")
	shopItemID := snowflake.MustParse("1048576200")
	operationID := snowflake.MustParse("1048576201")
	instanceID := snowflake.MustParse("1048576202")
	stub := &worldStub{purchaseValue: rpg.ItemAcquisitionResult{OperationID: operationID, EquipmentInstanceIDs: []snowflake.ID{instanceID}, BalanceAfter: 75}}
	service := NewPlayerService(stub, time.Now)
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})
	response, err := service.PurchaseShopItem(ctx, &rpgv1.PurchaseShopItemRequest{ShopItemId: shopItemID.String(), Quantity: 1, IdempotencyKey: "purchase-equipment-1"})
	if err != nil {
		t.Fatal(err)
	}
	if response.GetEquipmentInstanceIds()[0] != instanceID.String() || response.GetInventoryStack() != nil || stub.purchaseCommand.ShopItemID != shopItemID {
		t.Fatalf("unexpected purchase mapping: response=%v command=%+v", response, stub.purchaseCommand)
	}
}

// TestClaimQuestRewardsMapsAllAssets 验证任务领取完整映射装备、普通道具与货币结果。
func TestClaimQuestRewardsMapsAllAssets(t *testing.T) {
	accountID := snowflake.MustParse("1048576199")
	questID := snowflake.MustParse("1048576200")
	operationID := snowflake.MustParse("1048576201")
	instanceID := snowflake.MustParse("1048576202")
	itemID := snowflake.MustParse("1048576203")
	currencyID := snowflake.MustParse("1048576204")
	stub := &worldStub{questRewardValue: rpg.RewardAcquisitionResult{OperationID: operationID, EquipmentInstanceIDs: []snowflake.ID{instanceID}, InventoryStacks: []rpg.InventoryAcquisition{{ItemID: itemID, QuantityDelta: 2, BalanceAfter: 5}}, CurrencyBalances: []rpg.CurrencyAcquisition{{CurrencyID: currencyID, AmountDelta: 10, BalanceAfter: 20}}}}
	service := NewPlayerService(stub, time.Now)
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})
	response, err := service.ClaimQuestRewards(ctx, &rpgv1.ClaimQuestRewardsRequest{QuestId: questID.String(), ExpectedProgressVersion: 3, IdempotencyKey: "claim-quest-1"})
	if err != nil {
		t.Fatal(err)
	}
	if len(response.GetEquipmentInstanceIds()) != 1 || response.GetInventoryStacks()[0].GetItemId() != itemID.String() || response.GetCurrencyBalances()[0].GetCurrencyId() != currencyID.String() || stub.questRewardCommand.ExpectedProgressVersion != 3 {
		t.Fatalf("unexpected quest reward mapping: response=%v command=%+v", response, stub.questRewardCommand)
	}
}

// TestReplaceActiveProfessionsMapsCompleteSet 验证 RPC 把职业集合完整传给原子替换用例。
func TestReplaceActiveProfessionsMapsCompleteSet(t *testing.T) {
	accountID := snowflake.MustParse("1048576199")
	professionID := snowflake.MustParse("1048576200")
	stub := &worldStub{activeProfessions: []rpg.ActiveProfession{{ProfessionID: professionID, Name: "战士", Level: 5, Version: 2}}}
	service := NewPlayerService(stub, time.Now)
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})
	response, err := service.ReplaceActiveProfessions(ctx, &rpgv1.ReplaceActiveProfessionsRequest{ProfessionIds: []string{professionID.String()}, IdempotencyKey: "replace-professions-1"})
	if err != nil {
		t.Fatal(err)
	}
	if response.GetProfessions()[0].GetProfessionId() != professionID.String() || len(stub.replaceProfessionCmd.ProfessionIDs) != 1 || stub.replaceProfessionCmd.ProfessionIDs[0] != professionID {
		t.Fatalf("unexpected profession mapping: response=%v command=%+v", response, stub.replaceProfessionCmd)
	}
}
