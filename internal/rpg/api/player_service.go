// Package api 将 RPG 世界用例适配为生成的 Protobuf/gRPC 契约。
package api

import (
	"context"
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	rpgv1 "github.com/lishangbu/avalon/api/gen/go/avalon/rpg/v1"
	"github.com/lishangbu/avalon/internal/rpg"
	"github.com/lishangbu/avalon/internal/security/authentication"
	"google.golang.org/protobuf/types/known/timestamppb"
)

// WorldService 是玩家 RPC 所需的 RPG 世界用例集合。
type WorldService interface {
	GetMap(context.Context, snowflake.ID) (rpg.WorldMap, error)
	Traverse(context.Context, rpg.TraversalCommand) (rpg.TraversalResult, error)
	GetPendingEncounter(context.Context, snowflake.ID, time.Time) (*rpg.PendingEncounter, error)
	ResolvePendingEncounter(context.Context, rpg.ResolveEncounterCommand) (rpg.PendingEncounter, error)
	GetCheckpoint(context.Context, snowflake.ID) (*rpg.Checkpoint, error)
	SetCheckpoint(context.Context, rpg.SetCheckpointCommand) (rpg.Checkpoint, error)
	GetParty(context.Context, snowflake.ID) (rpg.Party, error)
	ReplaceParty(context.Context, rpg.ReplacePartyCommand) (rpg.Party, error)
	GetInventory(context.Context, snowflake.ID) (rpg.Inventory, error)
	ReplaceHeldItem(context.Context, rpg.ReplaceHeldItemCommand) (rpg.OwnedCreatureHeldItem, error)
	ListEquipmentInstances(context.Context, snowflake.ID, int, string) (rpg.EquipmentInstancePage, error)
	GetEquipmentInstance(context.Context, snowflake.ID, snowflake.ID) (rpg.EquipmentInstance, error)
	GetEquipmentLoadout(context.Context, snowflake.ID) (rpg.EquipmentLoadout, error)
	ReplaceEquipmentLoadout(context.Context, rpg.ReplaceEquipmentLoadoutCommand) (rpg.EquipmentLoadout, error)
	SellEquipmentInstance(context.Context, rpg.SellEquipmentCommand) (rpg.SellEquipmentResult, error)
	PurchaseShopItem(context.Context, rpg.PurchaseShopItemCommand) (rpg.ItemAcquisitionResult, error)
	ListAvailableQuests(context.Context, snowflake.ID) ([]rpg.AvailableQuest, error)
	ListQuestProgress(context.Context, snowflake.ID) ([]rpg.QuestProgress, error)
	StartQuest(context.Context, rpg.StartQuestCommand) (rpg.QuestProgress, error)
	CompleteQuest(context.Context, rpg.CompleteQuestCommand) (rpg.QuestProgress, error)
	ClaimQuestRewards(context.Context, rpg.ClaimQuestRewardsCommand) (rpg.RewardAcquisitionResult, error)
	ClaimLootSettlement(context.Context, rpg.ClaimLootSettlementCommand) (rpg.RewardAcquisitionResult, error)
	GetActiveProfessions(context.Context, snowflake.ID) ([]rpg.ActiveProfession, error)
	ReplaceActiveProfessions(context.Context, rpg.ReplaceActiveProfessionsCommand) ([]rpg.ActiveProfession, error)
}

// PlayerService 实现玩家 RpgWorldService 契约。
type PlayerService struct {
	world WorldService
	now   func() time.Time
}

// NewPlayerService 创建 RPG 玩家 RPC 适配器。
func NewPlayerService(world WorldService, now func() time.Time) *PlayerService {
	if now == nil {
		now = time.Now
	}
	return &PlayerService{world: world, now: now}
}

// GetMap 返回发现子图；游标首期由有界结果集保留为空。
func (service *PlayerService) GetMap(ctx context.Context, _ *rpgv1.GetMapRequest) (*rpgv1.GetMapResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	value, err := service.world.GetMap(ctx, accountID)
	if err != nil {
		return nil, publicError(err)
	}
	response := &rpgv1.GetMapResponse{Locations: make([]*rpgv1.MapLocation, 0, len(value.Locations)), Exits: make([]*rpgv1.MapExit, 0, len(value.Exits)), Position: positionMessage(value.Position)}
	for _, location := range value.Locations {
		parent := ""
		if location.ParentID != snowflake.ID(0) {
			parent = location.ParentID.String()
		}
		response.Locations = append(response.Locations, &rpgv1.MapLocation{Id: location.ID.String(), RegionId: location.RegionID.String(), ParentId: parent, Code: location.Code, Name: location.Name, LocationType: location.LocationType, MapX: location.X, MapY: location.Y, MapZ: location.Z})
	}
	for _, exit := range value.Exits {
		response.Exits = append(response.Exits, &rpgv1.MapExit{Id: exit.ID.String(), SourceLocationId: exit.SourceLocationID.String(), TargetLocationId: exit.TargetLocationID.String(), Code: exit.Code, Name: exit.Name, SortOrder: exit.SortOrder})
	}
	return response, nil
}

// Traverse 执行权威移动。
func (service *PlayerService) Traverse(ctx context.Context, request *rpgv1.TraverseRequest) (*rpgv1.TraverseResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	exitID, err := parseIdentifier(request.GetExitId())
	if err != nil {
		return nil, err
	}
	value, err := service.world.Traverse(ctx, rpg.TraversalCommand{AccountID: accountID, ExitID: exitID, ExpectedPositionVersion: request.GetExpectedPositionVersion(), IdempotencyKey: request.GetIdempotencyKey(), Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.TraverseResponse{Position: positionMessage(value.Position), PendingEncounter: pendingMessage(value.PendingEncounter)}, nil
}

// GetPendingEncounter 返回当前待处理遭遇。
func (service *PlayerService) GetPendingEncounter(ctx context.Context, _ *rpgv1.GetPendingEncounterRequest) (*rpgv1.GetPendingEncounterResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	value, err := service.world.GetPendingEncounter(ctx, accountID, service.now().UTC())
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.GetPendingEncounterResponse{PendingEncounter: pendingMessage(value)}, nil
}

// ResolvePendingEncounter 接受或取消待处理遭遇。
func (service *PlayerService) ResolvePendingEncounter(ctx context.Context, request *rpgv1.ResolvePendingEncounterRequest) (*rpgv1.ResolvePendingEncounterResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	encounterID, err := parseIdentifier(request.GetPendingEncounterId())
	if err != nil {
		return nil, err
	}
	resolution := rpg.EncounterResolution("")
	switch request.GetResolution() {
	case rpgv1.PendingEncounterResolution_PENDING_ENCOUNTER_RESOLUTION_ACCEPT:
		resolution = rpg.EncounterResolutionAccept
	case rpgv1.PendingEncounterResolution_PENDING_ENCOUNTER_RESOLUTION_CANCEL:
		resolution = rpg.EncounterResolutionCancel
	default:
		return nil, kratoserrors.BadRequest("INVALID_ENCOUNTER_RESOLUTION", "遭遇操作无效")
	}
	value, err := service.world.ResolvePendingEncounter(ctx, rpg.ResolveEncounterCommand{AccountID: accountID, PendingEncounterID: encounterID, Resolution: resolution, IdempotencyKey: request.GetIdempotencyKey(), Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.ResolvePendingEncounterResponse{PendingEncounter: pendingMessage(&value)}, nil
}

// GetCheckpoint 返回当前恢复点。
func (service *PlayerService) GetCheckpoint(ctx context.Context, _ *rpgv1.GetCheckpointRequest) (*rpgv1.GetCheckpointResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	value, err := service.world.GetCheckpoint(ctx, accountID)
	if err != nil {
		return nil, publicError(err)
	}
	response := &rpgv1.GetCheckpointResponse{}
	if value != nil {
		response.Checkpoint = checkpointMessage(*value)
	}
	return response, nil
}

// SetCheckpoint 更新当前恢复点。
func (service *PlayerService) SetCheckpoint(ctx context.Context, request *rpgv1.SetCheckpointRequest) (*rpgv1.SetCheckpointResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	checkpointID, err := parseIdentifier(request.GetCheckpointId())
	if err != nil {
		return nil, err
	}
	value, err := service.world.SetCheckpoint(ctx, rpg.SetCheckpointCommand{AccountID: accountID, CheckpointID: checkpointID, ExpectedVersion: request.GetExpectedVersion(), IdempotencyKey: request.GetIdempotencyKey(), Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.SetCheckpointResponse{Checkpoint: checkpointMessage(value), Version: value.Version}, nil
}

// GetParty 返回当前 RPG Party。
func (service *PlayerService) GetParty(ctx context.Context, _ *rpgv1.GetPartyRequest) (*rpgv1.GetPartyResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	value, err := service.world.GetParty(ctx, accountID)
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.GetPartyResponse{Party: partyMessage(value)}, nil
}

// ReplaceParty 替换 Party 成员。
func (service *PlayerService) ReplaceParty(ctx context.Context, request *rpgv1.ReplacePartyRequest) (*rpgv1.ReplacePartyResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	members := make([]rpg.PartyMember, 0, len(request.GetMembers()))
	for _, member := range request.GetMembers() {
		creatureID, parseErr := parseIdentifier(member.GetPlayerCharacterCreatureId())
		if parseErr != nil {
			return nil, parseErr
		}
		members = append(members, rpg.PartyMember{Position: int16(member.GetPosition()), PlayerCharacterCreatureID: creatureID})
	}
	value, err := service.world.ReplaceParty(ctx, rpg.ReplacePartyCommand{AccountID: accountID, ExpectedVersion: request.GetExpectedVersion(), IdempotencyKey: request.GetIdempotencyKey(), Members: members, Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.ReplacePartyResponse{Party: partyMessage(value)}, nil
}

// GetInventory 返回活动角色的非零背包与 Owned Creature 携带物。
func (service *PlayerService) GetInventory(ctx context.Context, _ *rpgv1.GetInventoryRequest) (*rpgv1.GetInventoryResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	value, err := service.world.GetInventory(ctx, accountID)
	if err != nil {
		return nil, publicError(err)
	}
	response := &rpgv1.GetInventoryResponse{Items: make([]*rpgv1.InventoryItem, 0, len(value.Items)), OwnedCreatures: make([]*rpgv1.OwnedCreatureHeldItem, 0, len(value.OwnedCreatures))}
	for _, item := range value.Items {
		response.Items = append(response.Items, &rpgv1.InventoryItem{ItemId: item.ItemID.String(), ItemName: item.ItemName, UsageType: item.UsageType, Quantity: item.Quantity, Version: item.Version})
	}
	for _, owned := range value.OwnedCreatures {
		response.OwnedCreatures = append(response.OwnedCreatures, ownedCreatureHeldItemMessage(owned))
	}
	return response, nil
}

// ReplaceHeldItem 原子替换或卸下 Owned Creature 当前携带的战斗道具。
func (service *PlayerService) ReplaceHeldItem(ctx context.Context, request *rpgv1.ReplaceHeldItemRequest) (*rpgv1.ReplaceHeldItemResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	ownedID, err := parseIdentifier(request.GetPlayerCharacterCreatureId())
	if err != nil {
		return nil, err
	}
	var itemID *snowflake.ID
	if request.ItemId != nil {
		parsed, parseErr := parseIdentifier(request.GetItemId())
		if parseErr != nil {
			return nil, parseErr
		}
		itemID = &parsed
	}
	value, err := service.world.ReplaceHeldItem(ctx, rpg.ReplaceHeldItemCommand{AccountID: accountID, OwnedCreatureID: ownedID, ItemID: itemID, ExpectedCreatureVersion: request.GetExpectedCreatureVersion(), IdempotencyKey: request.GetIdempotencyKey(), Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.ReplaceHeldItemResponse{OwnedCreature: ownedCreatureHeldItemMessage(value)}, nil
}

// ListEquipmentInstances 返回活动角色拥有的装备实例。
func (service *PlayerService) ListEquipmentInstances(ctx context.Context, request *rpgv1.ListEquipmentInstancesRequest) (*rpgv1.ListEquipmentInstancesResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	page, err := service.world.ListEquipmentInstances(ctx, accountID, int(request.GetPageSize()), request.GetCursor())
	if err != nil {
		return nil, publicError(err)
	}
	response := &rpgv1.ListEquipmentInstancesResponse{Instances: make([]*rpgv1.EquipmentInstance, 0, len(page.Items)), NextCursor: page.NextCursor}
	for _, value := range page.Items {
		response.Instances = append(response.Instances, equipmentInstanceMessage(value))
	}
	return response, nil
}

// GetEquipmentInstance 返回活动角色拥有的一个装备实例详情。
func (service *PlayerService) GetEquipmentInstance(ctx context.Context, request *rpgv1.GetEquipmentInstanceRequest) (*rpgv1.GetEquipmentInstanceResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	instanceID, err := parseIdentifier(request.GetEquipmentInstanceId())
	if err != nil {
		return nil, err
	}
	value, err := service.world.GetEquipmentInstance(ctx, accountID, instanceID)
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.GetEquipmentInstanceResponse{Instance: equipmentInstanceMessage(value)}, nil
}

// GetEquipmentLoadout 返回活动角色当前完整 Loadout。
func (service *PlayerService) GetEquipmentLoadout(ctx context.Context, _ *rpgv1.GetEquipmentLoadoutRequest) (*rpgv1.GetEquipmentLoadoutResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	value, err := service.world.GetEquipmentLoadout(ctx, accountID)
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.GetEquipmentLoadoutResponse{Loadout: equipmentLoadoutMessage(value)}, nil
}

// ReplaceEquipmentLoadout 原子替换活动角色整套装备。
func (service *PlayerService) ReplaceEquipmentLoadout(ctx context.Context, request *rpgv1.ReplaceEquipmentLoadoutRequest) (*rpgv1.ReplaceEquipmentLoadoutResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	entries := make([]rpg.EquipmentLoadoutEntry, 0, len(request.GetEntries()))
	for _, entry := range request.GetEntries() {
		id, parseErr := parseIdentifier(entry.GetEquipmentInstanceId())
		if parseErr != nil {
			return nil, parseErr
		}
		entries = append(entries, rpg.EquipmentLoadoutEntry{Slot: rpg.EquipmentSlot(entry.GetSlot()), InstanceID: id})
	}
	value, err := service.world.ReplaceEquipmentLoadout(ctx, rpg.ReplaceEquipmentLoadoutCommand{AccountID: accountID, Entries: entries, ExpectedVersion: request.GetExpectedVersion(), IdempotencyKey: request.GetIdempotencyKey(), Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.ReplaceEquipmentLoadoutResponse{Loadout: equipmentLoadoutMessage(value)}, nil
}

// SellEquipmentInstance 出售活动角色拥有且未穿戴的一个装备实例。
func (service *PlayerService) SellEquipmentInstance(ctx context.Context, request *rpgv1.SellEquipmentInstanceRequest) (*rpgv1.SellEquipmentInstanceResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	id, err := parseIdentifier(request.GetEquipmentInstanceId())
	if err != nil {
		return nil, err
	}
	value, err := service.world.SellEquipmentInstance(ctx, rpg.SellEquipmentCommand{AccountID: accountID, InstanceID: id, ExpectedVersion: request.GetExpectedVersion(), IdempotencyKey: request.GetIdempotencyKey(), Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.SellEquipmentInstanceResponse{OperationId: value.OperationID.String(), SellPrice: value.SellPrice, CurrencyId: value.CurrencyID.String(), BalanceAfter: value.BalanceAfter}, nil
}

// PurchaseShopItem 把玩家商品身份映射为服务端定价的原子购买命令。
func (service *PlayerService) PurchaseShopItem(ctx context.Context, request *rpgv1.PurchaseShopItemRequest) (*rpgv1.PurchaseShopItemResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	shopItemID, err := parseIdentifier(request.GetShopItemId())
	if err != nil {
		return nil, err
	}
	value, err := service.world.PurchaseShopItem(ctx, rpg.PurchaseShopItemCommand{AccountID: accountID, ShopItemID: shopItemID, Quantity: request.GetQuantity(), IdempotencyKey: request.GetIdempotencyKey(), Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	return purchaseShopItemMessage(value), nil
}

// ListAvailableQuests 返回当前角色此刻可以开始的任务定义。
func (service *PlayerService) ListAvailableQuests(ctx context.Context, _ *rpgv1.ListAvailableQuestsRequest) (*rpgv1.ListAvailableQuestsResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	values, err := service.world.ListAvailableQuests(ctx, accountID)
	if err != nil {
		return nil, publicError(err)
	}
	response := &rpgv1.ListAvailableQuestsResponse{Quests: make([]*rpgv1.AvailableQuest, 0, len(values))}
	for _, value := range values {
		response.Quests = append(response.Quests, &rpgv1.AvailableQuest{QuestId: value.QuestID.String(), Code: value.Code, Name: value.Name, Description: value.Description, QuestType: value.QuestType, Repeatable: value.Repeatable})
	}
	return response, nil
}

// ListQuestProgress 返回当前角色全部已开始任务与目标累计值。
func (service *PlayerService) ListQuestProgress(ctx context.Context, _ *rpgv1.ListQuestProgressRequest) (*rpgv1.ListQuestProgressResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	values, err := service.world.ListQuestProgress(ctx, accountID)
	if err != nil {
		return nil, publicError(err)
	}
	response := &rpgv1.ListQuestProgressResponse{Quests: make([]*rpgv1.PlayerQuestProgress, 0, len(values))}
	for _, value := range values {
		response.Quests = append(response.Quests, questProgressMessage(value))
	}
	return response, nil
}

// StartQuest 把任务身份映射为服务端权威的开始命令。
func (service *PlayerService) StartQuest(ctx context.Context, request *rpgv1.StartQuestRequest) (*rpgv1.StartQuestResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	questID, err := parseIdentifier(request.GetQuestId())
	if err != nil {
		return nil, err
	}
	value, err := service.world.StartQuest(ctx, rpg.StartQuestCommand{AccountID: accountID, QuestID: questID, IdempotencyKey: request.GetIdempotencyKey(), Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.StartQuestResponse{Quest: questProgressMessage(value)}, nil
}

// CompleteQuest 映射期望版本，并由服务端校验目标与交付地点。
func (service *PlayerService) CompleteQuest(ctx context.Context, request *rpgv1.CompleteQuestRequest) (*rpgv1.CompleteQuestResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	questID, err := parseIdentifier(request.GetQuestId())
	if err != nil {
		return nil, err
	}
	value, err := service.world.CompleteQuest(ctx, rpg.CompleteQuestCommand{AccountID: accountID, QuestID: questID, ExpectedVersion: request.GetExpectedVersion(), IdempotencyKey: request.GetIdempotencyKey(), Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.CompleteQuestResponse{Quest: questProgressMessage(value)}, nil
}

// ClaimQuestRewards 映射任务进度版本，并返回服务端权威的完整奖励集合。
func (service *PlayerService) ClaimQuestRewards(ctx context.Context, request *rpgv1.ClaimQuestRewardsRequest) (*rpgv1.ClaimQuestRewardsResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	questID, err := parseIdentifier(request.GetQuestId())
	if err != nil {
		return nil, err
	}
	value, err := service.world.ClaimQuestRewards(ctx, rpg.ClaimQuestRewardsCommand{AccountID: accountID, QuestID: questID, ExpectedProgressVersion: request.GetExpectedProgressVersion(), IdempotencyKey: request.GetIdempotencyKey(), Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	assets, currencies := rewardAcquisitionMessages(value)
	return &rpgv1.ClaimQuestRewardsResponse{OperationId: value.OperationID.String(), EquipmentInstanceIds: identifierMessages(value.EquipmentInstanceIDs), InventoryStacks: assets, CurrencyBalances: currencies}, nil
}

// ClaimLootSettlement 映射权威结算身份，并返回一次性领取结果。
func (service *PlayerService) ClaimLootSettlement(ctx context.Context, request *rpgv1.ClaimLootSettlementRequest) (*rpgv1.ClaimLootSettlementResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	settlementID, err := parseIdentifier(request.GetLootSettlementId())
	if err != nil {
		return nil, err
	}
	value, err := service.world.ClaimLootSettlement(ctx, rpg.ClaimLootSettlementCommand{AccountID: accountID, LootSettlementID: settlementID, IdempotencyKey: request.GetIdempotencyKey(), Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	assets, currencies := rewardAcquisitionMessages(value)
	return &rpgv1.ClaimLootSettlementResponse{OperationId: value.OperationID.String(), EquipmentInstanceIds: identifierMessages(value.EquipmentInstanceIDs), InventoryStacks: assets, CurrencyBalances: currencies}, nil
}

// GetActiveProfessions 返回当前活动角色参与资格判定的职业集合。
func (service *PlayerService) GetActiveProfessions(ctx context.Context, _ *rpgv1.GetActiveProfessionsRequest) (*rpgv1.GetActiveProfessionsResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	values, err := service.world.GetActiveProfessions(ctx, accountID)
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.GetActiveProfessionsResponse{Professions: activeProfessionMessages(values)}, nil
}

// ReplaceActiveProfessions 映射完整目标职业集合，并保留服务端 Loadout 合法性拒绝语义。
func (service *PlayerService) ReplaceActiveProfessions(ctx context.Context, request *rpgv1.ReplaceActiveProfessionsRequest) (*rpgv1.ReplaceActiveProfessionsResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	ids := make([]snowflake.ID, 0, len(request.GetProfessionIds()))
	for _, raw := range request.GetProfessionIds() {
		id, parseErr := parseIdentifier(raw)
		if parseErr != nil {
			return nil, parseErr
		}
		ids = append(ids, id)
	}
	values, err := service.world.ReplaceActiveProfessions(ctx, rpg.ReplaceActiveProfessionsCommand{AccountID: accountID, ProfessionIDs: ids, IdempotencyKey: request.GetIdempotencyKey(), Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.ReplaceActiveProfessionsResponse{Professions: activeProfessionMessages(values)}, nil
}

func playerAccountID(ctx context.Context) (snowflake.ID, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok || principal.AccountID == snowflake.ID(0) {
		return snowflake.ID(0), kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	return principal.AccountID, nil
}
func parseIdentifier(raw string) (snowflake.ID, error) {
	value, err := snowflake.Parse(raw)
	if err != nil || value == snowflake.ID(0) {
		return snowflake.ID(0), kratoserrors.BadRequest("INVALID_ID", "标识格式无效")
	}
	return value, nil
}
func positionMessage(value rpg.Position) *rpgv1.Position {
	return &rpgv1.Position{LocationId: value.LocationID.String(), MoveSequence: value.MoveSequence, Version: value.Version, UpdatedAt: timestamppb.New(value.UpdatedAt)}
}
func pendingMessage(value *rpg.PendingEncounter) *rpgv1.PendingEncounter {
	if value == nil {
		return nil
	}
	message := &rpgv1.PendingEncounter{Id: value.ID.String(), EncounterEntryId: value.EncounterEntryID.String(), State: value.State, ExpiresAt: timestamppb.New(value.ExpiresAt)}
	if value.BattleID != snowflake.ID(0) {
		battleID := value.BattleID.String()
		message.BattleId = &battleID
	}
	return message
}
func checkpointMessage(value rpg.Checkpoint) *rpgv1.Checkpoint {
	return &rpgv1.Checkpoint{Id: value.ID.String(), LocationId: value.LocationID.String(), Code: value.Code, Name: value.Name}
}
func partyMessage(value rpg.Party) *rpgv1.Party {
	message := &rpgv1.Party{Id: value.ID.String(), Version: value.Version, Members: make([]*rpgv1.PartyMember, 0, len(value.Members))}
	for _, member := range value.Members {
		message.Members = append(message.Members, &rpgv1.PartyMember{Position: int32(member.Position), PlayerCharacterCreatureId: member.PlayerCharacterCreatureID.String()})
	}
	return message
}
func ownedCreatureHeldItemMessage(value rpg.OwnedCreatureHeldItem) *rpgv1.OwnedCreatureHeldItem {
	message := &rpgv1.OwnedCreatureHeldItem{PlayerCharacterCreatureId: value.PlayerCharacterCreatureID.String(), CreatureId: value.CreatureID.String(), Version: value.Version}
	if value.Nickname != "" {
		message.Nickname = &value.Nickname
	}
	if value.HeldItemID.IsValid() {
		itemID, itemName := value.HeldItemID.String(), value.HeldItemName
		message.HeldItemId, message.HeldItemName = &itemID, &itemName
	}
	return message
}
func equipmentInstanceMessage(value rpg.EquipmentInstance) *rpgv1.EquipmentInstance {
	message := &rpgv1.EquipmentInstance{Id: value.ID.String(), EquipmentId: value.EquipmentID.String(), ItemId: value.ItemID.String(), Name: value.Name, SlotType: string(value.SlotType), SourceType: value.SourceType, Version: value.Version, MinimumLevel: value.MinimumLevel, RuleTimings: value.RuleTimings, AcquiredAt: timestamppb.New(value.AcquiredAt)}
	if value.Handedness != "" {
		handedness := string(value.Handedness)
		message.Handedness = &handedness
	}
	if value.EquippedSlot != "" {
		message.EquippedSlot = &value.EquippedSlot
	}
	return message
}
func equipmentLoadoutMessage(value rpg.EquipmentLoadout) *rpgv1.EquipmentLoadout {
	message := &rpgv1.EquipmentLoadout{Version: value.Version, UpdatedAt: timestamppb.New(value.UpdatedAt), Entries: make([]*rpgv1.EquipmentLoadoutEntry, 0, len(value.Entries))}
	for _, entry := range value.Entries {
		message.Entries = append(message.Entries, &rpgv1.EquipmentLoadoutEntry{Slot: string(entry.Slot), EquipmentInstanceId: entry.InstanceID.String()})
	}
	return message
}

func purchaseShopItemMessage(value rpg.ItemAcquisitionResult) *rpgv1.PurchaseShopItemResponse {
	message := &rpgv1.PurchaseShopItemResponse{OperationId: value.OperationID.String(), EquipmentInstanceIds: identifierMessages(value.EquipmentInstanceIDs), BalanceAfter: value.BalanceAfter}
	if value.InventoryStack != nil {
		message.InventoryStack = inventoryAcquisitionMessage(*value.InventoryStack)
	}
	return message
}

func questProgressMessage(value rpg.QuestProgress) *rpgv1.PlayerQuestProgress {
	message := &rpgv1.PlayerQuestProgress{QuestId: value.QuestID.String(), Code: value.Code, Name: value.Name, Description: value.Description, Status: value.Status, CompletionCount: value.CompletionCount, Version: value.Version, StartedAt: timestamppb.New(value.StartedAt), Objectives: make([]*rpgv1.QuestObjectiveProgress, 0, len(value.Objectives))}
	if value.CompletedAt != nil {
		message.CompletedAt = timestamppb.New(*value.CompletedAt)
	}
	for _, objective := range value.Objectives {
		item := &rpgv1.QuestObjectiveProgress{ObjectiveId: objective.ObjectiveID.String(), Code: objective.Code, ObjectiveType: objective.ObjectiveType, CurrentCount: objective.CurrentCount, RequiredCount: objective.RequiredCount, Description: objective.Description}
		if objective.CompletedAt != nil {
			item.CompletedAt = timestamppb.New(*objective.CompletedAt)
		}
		message.Objectives = append(message.Objectives, item)
	}
	return message
}

func rewardAcquisitionMessages(value rpg.RewardAcquisitionResult) ([]*rpgv1.AcquiredInventoryStack, []*rpgv1.AcquiredCurrencyBalance) {
	items := make([]*rpgv1.AcquiredInventoryStack, 0, len(value.InventoryStacks))
	for _, item := range value.InventoryStacks {
		items = append(items, inventoryAcquisitionMessage(item))
	}
	currencies := make([]*rpgv1.AcquiredCurrencyBalance, 0, len(value.CurrencyBalances))
	for _, currency := range value.CurrencyBalances {
		currencies = append(currencies, &rpgv1.AcquiredCurrencyBalance{CurrencyId: currency.CurrencyID.String(), AmountDelta: currency.AmountDelta, BalanceAfter: currency.BalanceAfter})
	}
	return items, currencies
}

func inventoryAcquisitionMessage(value rpg.InventoryAcquisition) *rpgv1.AcquiredInventoryStack {
	return &rpgv1.AcquiredInventoryStack{ItemId: value.ItemID.String(), QuantityDelta: value.QuantityDelta, BalanceAfter: value.BalanceAfter}
}

func identifierMessages(values []snowflake.ID) []string {
	result := make([]string, 0, len(values))
	for _, value := range values {
		result = append(result, value.String())
	}
	return result
}

func activeProfessionMessages(values []rpg.ActiveProfession) []*rpgv1.ActiveProfession {
	result := make([]*rpgv1.ActiveProfession, 0, len(values))
	for _, value := range values {
		result = append(result, &rpgv1.ActiveProfession{ProfessionId: value.ProfessionID.String(), Name: value.Name, Level: value.Level, Experience: value.Experience, Version: value.Version})
	}
	return result
}
func publicError(err error) error {
	switch {
	case errors.Is(err, rpg.ErrActivePlayerCharacterMissing):
		return kratoserrors.New(412, "ACTIVE_PLAYER_CHARACTER_REQUIRED", "请先选择活动角色")
	case errors.Is(err, rpg.ErrPositionConflict):
		return kratoserrors.Conflict("POSITION_VERSION_CONFLICT", "角色位置已经变化，请刷新后重试")
	case errors.Is(err, rpg.ErrExitUnavailable):
		return kratoserrors.New(412, "EXIT_UNAVAILABLE", "出口当前不可用")
	case errors.Is(err, rpg.ErrExitConditionNotMet):
		return kratoserrors.New(412, "EXIT_CONDITION_NOT_MET", "尚未满足出口条件")
	case errors.Is(err, rpg.ErrPendingEncounterBlocksMovement):
		return kratoserrors.New(412, "PENDING_ENCOUNTER", "请先处理当前遭遇")
	case errors.Is(err, rpg.ErrIdempotencyConflict):
		return kratoserrors.Conflict("IDEMPOTENCY_CONFLICT", "幂等键已用于不同请求")
	case errors.Is(err, rpg.ErrHeldItemUnavailable):
		return kratoserrors.New(412, "HELD_ITEM_UNAVAILABLE", "携带道具当前不可用")
	case errors.Is(err, rpg.ErrCreatureInBattle):
		return kratoserrors.Conflict("CREATURE_IN_BATTLE", "Creature 正在战斗中，无法更换携带道具")
	case errors.Is(err, rpg.ErrOwnedCreatureConflict):
		return kratoserrors.Conflict("OWNED_CREATURE_CONFLICT", "Creature 归属或版本已经变化")
	case errors.Is(err, rpg.ErrEquipmentNotFound):
		return kratoserrors.NotFound("EQUIPMENT_NOT_FOUND", "装备不存在")
	case errors.Is(err, rpg.ErrEquipmentNotOwned):
		return kratoserrors.New(403, "EQUIPMENT_NOT_OWNED", "装备不属于当前角色")
	case errors.Is(err, rpg.ErrEquipmentSlotMismatch):
		return kratoserrors.New(412, "EQUIPMENT_SLOT_MISMATCH", "装备槽位不匹配")
	case errors.Is(err, rpg.ErrEquipmentRequirementNotMet):
		return kratoserrors.New(412, "EQUIPMENT_REQUIREMENT_NOT_MET", "角色尚未满足装备要求")
	case errors.Is(err, rpg.ErrEquipmentInBattle):
		return kratoserrors.Conflict("EQUIPMENT_IN_BATTLE", "角色正在战斗中，无法换装")
	case errors.Is(err, rpg.ErrEquipmentLoadoutConflict):
		return kratoserrors.Conflict("EQUIPMENT_LOADOUT_CONFLICT", "装备配置存在冲突")
	case errors.Is(err, rpg.ErrEquipmentTwoHandedConflict):
		return kratoserrors.Conflict("EQUIPMENT_TWO_HANDED_CONFLICT", "双手装备与副手冲突")
	case errors.Is(err, rpg.ErrInvalidEquipmentCursor):
		return kratoserrors.BadRequest("INVALID_EQUIPMENT_CURSOR", "装备列表游标无效")
	case errors.Is(err, rpg.ErrShopItemUnavailable):
		return kratoserrors.New(412, "SHOP_ITEM_UNAVAILABLE", "商店商品当前不可购买")
	case errors.Is(err, rpg.ErrInsufficientCurrency):
		return kratoserrors.New(412, "INSUFFICIENT_CURRENCY", "货币余额不足")
	case errors.Is(err, rpg.ErrQuestRewardUnavailable):
		return kratoserrors.New(412, "QUEST_REWARD_UNAVAILABLE", "任务奖励当前不可领取")
	case errors.Is(err, rpg.ErrQuestProgressConflict):
		return kratoserrors.Conflict("QUEST_PROGRESS_CONFLICT", "任务进度已经变化，请刷新后重试")
	case errors.Is(err, rpg.ErrQuestObjectivesIncomplete):
		return kratoserrors.New(412, "QUEST_OBJECTIVES_INCOMPLETE", "任务目标尚未全部完成")
	case errors.Is(err, rpg.ErrQuestUnavailable):
		return kratoserrors.New(412, "QUEST_UNAVAILABLE", "任务当前不可开始或完成")
	case errors.Is(err, rpg.ErrLootSettlementUnavailable):
		return kratoserrors.New(412, "LOOT_SETTLEMENT_UNAVAILABLE", "掉落结算当前不可领取")
	case errors.Is(err, rpg.ErrProfessionUnavailable):
		return kratoserrors.New(412, "PROFESSION_UNAVAILABLE", "职业当前不可激活")
	case errors.Is(err, rpg.ErrProfessionChangeInBattle):
		return kratoserrors.Conflict("PROFESSION_IN_BATTLE", "角色正在战斗中，无法切换职业")
	default:
		return kratoserrors.InternalServer("RPG_WORLD_FAILED", "服务端无法完成请求")
	}
}
