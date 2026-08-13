package api

import (
	"context"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	rpgv1 "github.com/lishangbu/avalon/api/gen/go/avalon/rpg/v1"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/rpg"
	"google.golang.org/protobuf/types/known/timestamppb"
)

// ListEquipments 返回完整 Equipment Catalog Entry 聚合。
func (service *AdminWorldService) ListEquipments(ctx context.Context, request *rpgv1.ListEquipmentsRequest) (*rpgv1.ListEquipmentsResponse, error) {
	page, err := service.store.ListEquipments(ctx, int(request.GetPageSize()), request.GetCursor())
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListEquipmentsResponse{Equipments: make([]*rpgv1.AdminEquipment, 0, len(page.Items)), NextCursor: page.NextCursor}
	for _, row := range page.Items {
		response.Equipments = append(response.Equipments, adminEquipmentMessage(row))
	}
	return response, nil
}

// ListEquipmentOptions 返回管理表单可选择的全部启用装备轻量引用。
func (service *AdminWorldService) ListEquipmentOptions(ctx context.Context, _ *rpgv1.ListEquipmentOptionsRequest) (*rpgv1.ListEquipmentOptionsResponse, error) {
	rows, err := service.store.ListEquipmentOptions(ctx)
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListEquipmentOptionsResponse{Options: make([]*rpgv1.EquipmentOption, 0, len(rows))}
	for _, row := range rows {
		response.Options = append(response.Options, &rpgv1.EquipmentOption{Id: row.ID.String(), ItemName: row.ItemName})
	}
	return response, nil
}

// SaveEquipment 原子创建或替换装备主资料、职业白名单、属性修正和规则时机。
func (service *AdminWorldService) SaveEquipment(ctx context.Context, request *rpgv1.SaveEquipmentRequest) (*rpgv1.SaveEquipmentResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_EQUIPMENT", "装备资料无效")
	}
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	id, err := optionalID(request.GetEquipmentId(), "INVALID_EQUIPMENT_ID", "装备标识无效")
	if err != nil {
		return nil, err
	}
	itemID, err := requiredID(request.GetBody().GetItemId(), "INVALID_ITEM_ID", "道具标识无效")
	if err != nil {
		return nil, err
	}
	sellCurrencyID, err := requiredID(request.GetBody().GetSellCurrencyId(), "INVALID_CURRENCY_ID", "出售货币标识无效")
	if err != nil {
		return nil, err
	}
	value := rpg.AdminEquipment{ID: id, ItemID: itemID, SellCurrencyID: sellCurrencyID, SlotType: rpg.EquipmentSlotType(request.GetBody().GetSlotType()), Handedness: rpg.EquipmentHandedness(request.GetBody().GetHandedness()), MinimumLevel: request.GetBody().GetMinimumLevel(), SellPrice: request.GetBody().GetSellPrice(), Enabled: request.GetBody().GetEnabled(), RuleTimings: append([]string(nil), request.GetBody().GetRuleTimings()...)}
	for _, raw := range request.GetBody().GetProfessionIds() {
		parsed, parseErr := requiredID(raw, "INVALID_PROFESSION_ID", "职业标识无效")
		if parseErr != nil {
			return nil, parseErr
		}
		value.ProfessionIDs = append(value.ProfessionIDs, parsed)
	}
	for _, modifier := range request.GetBody().GetStatModifiers() {
		statID, parseErr := requiredID(modifier.GetStatId(), "INVALID_STAT_ID", "数值项标识无效")
		if parseErr != nil {
			return nil, parseErr
		}
		value.StatModifiers = append(value.StatModifiers, rpg.AdminEquipmentStatModifier{StatID: statID, FlatValue: modifier.GetFlatValue(), PercentageBPS: modifier.GetPercentageBps()})
	}
	saved, err := service.store.SaveEquipment(ctx, rpg.SaveEquipmentCommand{Write: write, Value: value, ExpectedVersion: request.GetExpectedVersion()})
	if err != nil {
		return nil, adminError(err)
	}
	return &rpgv1.SaveEquipmentResponse{Equipment: adminEquipmentMessage(saved)}, nil
}

// ListAdminEquipmentInstances 返回按角色、装备、穿戴状态和来源过滤的实例诊断视图。
func (service *AdminWorldService) ListAdminEquipmentInstances(ctx context.Context, request *rpgv1.ListAdminEquipmentInstancesRequest) (*rpgv1.ListAdminEquipmentInstancesResponse, error) {
	playerID, err := optionalID(request.GetPlayerCharacterId(), "INVALID_PLAYER_CHARACTER_ID", "角色标识无效")
	if err != nil {
		return nil, err
	}
	equipmentID, err := optionalID(request.GetEquipmentId(), "INVALID_EQUIPMENT_ID", "装备标识无效")
	if err != nil {
		return nil, err
	}
	page, err := service.store.ListAdminEquipmentInstances(ctx, rpg.AdminEquipmentInstanceQuery{PageSize: int(request.GetPageSize()), Cursor: request.GetCursor(), PlayerCharacterID: playerID, EquipmentID: equipmentID, Equipped: request.Equipped, SourceType: request.GetSourceType()})
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListAdminEquipmentInstancesResponse{Instances: make([]*rpgv1.AdminEquipmentInstance, 0, len(page.Items)), NextCursor: page.NextCursor}
	for _, row := range page.Items {
		value := &rpgv1.AdminEquipmentInstance{Id: row.ID.String(), PlayerCharacterId: row.PlayerCharacterID.String(), EquipmentId: row.EquipmentID.String(), ItemName: row.ItemName, SourceType: row.SourceType, Version: row.Version, AcquiredAt: timestamppb.New(row.AcquiredAt)}
		if row.SourceReferenceID.IsValid() {
			id := row.SourceReferenceID.String()
			value.SourceReferenceId = &id
		}
		if row.EquippedSlot != "" {
			value.EquippedSlot = &row.EquippedSlot
		}
		if !row.SoldAt.IsZero() {
			value.SoldAt = timestamppb.New(row.SoldAt)
		}
		response.Instances = append(response.Instances, value)
	}
	return response, nil
}

// ListEquipmentTransactions 返回不可变装备资产流水。
func (service *AdminWorldService) ListEquipmentTransactions(ctx context.Context, request *rpgv1.ListEquipmentTransactionsRequest) (*rpgv1.ListEquipmentTransactionsResponse, error) {
	playerID, err := optionalID(request.GetPlayerCharacterId(), "INVALID_PLAYER_CHARACTER_ID", "角色标识无效")
	if err != nil {
		return nil, err
	}
	instanceID, err := optionalID(request.GetEquipmentInstanceId(), "INVALID_EQUIPMENT_INSTANCE_ID", "装备实例标识无效")
	if err != nil {
		return nil, err
	}
	page, err := service.store.ListEquipmentTransactions(ctx, rpg.EquipmentTransactionQuery{PageSize: int(request.GetPageSize()), Cursor: request.GetCursor(), PlayerCharacterID: playerID, EquipmentInstanceID: instanceID, Action: request.GetAction()})
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListEquipmentTransactionsResponse{Transactions: make([]*rpgv1.EquipmentTransaction, 0, len(page.Items)), NextCursor: page.NextCursor}
	for _, row := range page.Items {
		value := &rpgv1.EquipmentTransaction{Id: row.ID.String(), OperationId: row.OperationID.String(), PlayerCharacterId: row.PlayerCharacterID.String(), EquipmentInstanceId: row.InstanceID.String(), Action: row.Action, CreatedAt: timestamppb.New(row.CreatedAt)}
		if row.SourceType != "" {
			value.SourceType = &row.SourceType
		}
		if row.Slot != "" {
			value.Slot = &row.Slot
		}
		response.Transactions = append(response.Transactions, value)
	}
	return response, nil
}

// GrantEquipment 幂等授予一个角色一至一百件同类装备实例。
func (service *AdminWorldService) GrantEquipment(ctx context.Context, request *rpgv1.GrantEquipmentRequest) (*rpgv1.GrantEquipmentResponse, error) {
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	playerID, err := requiredID(request.GetPlayerCharacterId(), "INVALID_PLAYER_CHARACTER_ID", "角色标识无效")
	if err != nil {
		return nil, err
	}
	equipmentID, err := requiredID(request.GetEquipmentId(), "INVALID_EQUIPMENT_ID", "装备标识无效")
	if err != nil {
		return nil, err
	}
	result, err := service.store.GrantEquipment(ctx, rpg.GrantEquipmentCommand{Write: write, PlayerCharacterID: playerID, EquipmentID: equipmentID, Quantity: request.GetQuantity(), Reason: request.GetReason()})
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.GrantEquipmentResponse{OperationId: result.OperationID.String(), EquipmentInstanceIds: make([]string, 0, len(result.InstanceIDs))}
	for _, id := range result.InstanceIDs {
		response.EquipmentInstanceIds = append(response.EquipmentInstanceIds, id.String())
	}
	return response, nil
}

func adminEquipmentMessage(row rpg.AdminEquipment) *rpgv1.AdminEquipment {
	value := &rpgv1.AdminEquipment{Id: row.ID.String(), ItemId: row.ItemID.String(), ItemName: row.ItemName, SlotType: string(row.SlotType), MinimumLevel: row.MinimumLevel, SellPrice: row.SellPrice, SellCurrencyId: row.SellCurrencyID.String(), Enabled: row.Enabled, Version: row.Version, RuleTimings: row.RuleTimings, InstanceCount: row.InstanceCount, ProfessionIds: make([]string, 0, len(row.ProfessionIDs)), StatModifiers: make([]*rpgv1.EquipmentStatModifier, 0, len(row.StatModifiers))}
	if row.Handedness != "" {
		text := string(row.Handedness)
		value.Handedness = &text
	}
	for _, id := range row.ProfessionIDs {
		value.ProfessionIds = append(value.ProfessionIds, id.String())
	}
	for _, modifier := range row.StatModifiers {
		value.StatModifiers = append(value.StatModifiers, &rpgv1.EquipmentStatModifier{Id: identifierText(modifier.ID), StatId: modifier.StatID.String(), FlatValue: modifier.FlatValue, PercentageBps: modifier.PercentageBPS})
	}
	return value
}
func identifierText(id snowflake.ID) string {
	if !id.IsValid() {
		return ""
	}
	return id.String()
}
