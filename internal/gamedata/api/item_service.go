package api

import (
	"context"
	"errors"
	"strconv"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/item"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListGameItems 分页查询维护窗口中的道具资料。
func (service *KratosService) ListGameItems(ctx context.Context, request *domainv1.ListGameItemsRequest) (*domainv1.ListGameItemsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	categoryID, err := optionalGameDataIdentifier(request.GetCategoryId(), "INVALID_ITEM_CATEGORY_ID")
	if err != nil {
		return nil, err
	}
	var usageType *item.UsageType
	if request.GetUsageType() != "" {
		value := item.UsageType(request.GetUsageType())
		usageType = &value
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	sortValue := request.GetSort()
	if sortValue == "" {
		sortValue = string(item.SortCodeAscending)
	}
	result, err := service.services.Items.List(ctx, item.ListQuery{Page: page, PageSize: pageSize, Q: request.GetQ(), Code: request.GetCode(), Name: request.GetName(), UsageType: usageType, CategoryID: categoryID, Cost: optionalInt32(request.GetCost()), Enabled: request.Enabled, Sort: item.Sort(sortValue)})
	if err != nil {
		return nil, service.itemError(ctx, "GAME_ITEM_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameItem, len(result.Items))
	for index := range result.Items {
		items[index] = gameItemMessage(result.Items[index])
	}
	return &domainv1.ListGameItemsResponse{HttpStatusCode: 200, Body: &domainv1.GameItemPage{Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize}}, nil
}

// CreateGameItem 在维护窗口中创建独立的道具资料。
func (service *KratosService) CreateGameItem(ctx context.Context, request *domainv1.CreateGameItemRequest) (*domainv1.CreateGameItemResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	categoryID, err := optionalGameDataIdentifier(request.GetBody().GetCategoryId(), "INVALID_ITEM_CATEGORY_ID")
	if err != nil {
		return nil, err
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	created, err := service.services.Items.Create(ctx, item.CreateCommand{GameDataWriteContext: writeContext, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(), Description: optionalText(request.GetBody().GetDescription()), Effect: optionalText(request.GetBody().GetEffect()), ShortEffect: optionalText(request.GetBody().GetShortEffect()), FlingEffectID: optionalIdentifierPtr(request.GetBody().GetFlingEffectId()), UsageType: item.UsageType(request.GetBody().GetUsageType()), CategoryID: categoryID, Cost: request.GetBody().GetCost(), FlingPower: optionalInt32(request.GetBody().GetFlingPower()), Enabled: request.GetBody().GetEnabled()})
	if err != nil {
		return nil, service.itemError(ctx, "GAME_ITEM_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameItemResponse{HttpStatusCode: 201, Body: gameItemMessage(created)}, nil
}

// GetGameItem 查询维护窗口中指定稳定身份的道具资料。
func (service *KratosService) GetGameItem(ctx context.Context, request *domainv1.GetGameItemRequest) (*domainv1.GetGameItemResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	itemID, err := gameDataIdentifier(request.GetItemId(), "INVALID_ITEM_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.Items.Get(ctx, itemID)
	if err != nil {
		return nil, service.itemError(ctx, "GAME_ITEM_QUERY_FAILED", err)
	}
	return &domainv1.GetGameItemResponse{HttpStatusCode: 200, Body: gameItemMessage(value)}, nil
}

// UpdateGameItem 使用独立乐观版本完整更新道具资料。
func (service *KratosService) UpdateGameItem(ctx context.Context, request *domainv1.UpdateGameItemRequest) (*domainv1.UpdateGameItemResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	itemID, err := gameDataIdentifier(request.GetItemId(), "INVALID_ITEM_ID")
	if err != nil {
		return nil, err
	}
	categoryID, err := optionalGameDataIdentifier(request.GetBody().GetCategoryId(), "INVALID_ITEM_CATEGORY_ID")
	if err != nil {
		return nil, err
	}
	version, err := gameDataVersion(request.GetBody().GetExpectedVersion())
	if err != nil {
		return nil, err
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	updated, err := service.services.Items.Update(ctx, item.UpdateCommand{GameDataWriteContext: writeContext, ItemID: itemID, ExpectedVersion: version, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(), Description: optionalText(request.GetBody().GetDescription()), Effect: optionalText(request.GetBody().GetEffect()), ShortEffect: optionalText(request.GetBody().GetShortEffect()), FlingEffectID: optionalIdentifierPtr(request.GetBody().GetFlingEffectId()), UsageType: item.UsageType(request.GetBody().GetUsageType()), CategoryID: categoryID, Cost: request.GetBody().GetCost(), FlingPower: optionalInt32(request.GetBody().GetFlingPower()), Enabled: request.GetBody().GetEnabled()})
	if err != nil {
		return nil, service.itemError(ctx, "GAME_ITEM_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateGameItemResponse{HttpStatusCode: 200, Body: gameItemMessage(updated)}, nil
}

// DeleteGameItem 使用独立乐观版本禁用未被引用的道具资料。
func gameItemMessage(value item.Item) *domainv1.GameItem {
	categoryID := ""
	if value.CategoryID != nil {
		categoryID = value.CategoryID.String()
	}
	flingPower := int32(0)
	if value.FlingPower != nil {
		flingPower = *value.FlingPower
	}
	assetID := ""
	if value.AssetID != nil {
		assetID = value.AssetID.String()
	}
	flingEffectID := ""
	if value.FlingEffectID != nil {
		flingEffectID = value.FlingEffectID.String()
	}
	return &domainv1.GameItem{Id: value.ID.String(), Code: value.Code, Name: value.Name, UsageType: string(value.UsageType), CategoryId: categoryID, Cost: value.Cost, FlingPower: flingPower, Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10), AssetId: assetID, Description: textValue(value.Description), Effect: textValue(value.Effect), ShortEffect: textValue(value.ShortEffect), FlingEffectId: flingEffectID}
}

func (service *KratosService) itemError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, item.ErrInvalidItem):
		return kratoserrors.BadRequest("INVALID_GAME_ITEM", "道具资料字段无效")
	case errors.Is(err, item.ErrItemNotFound), errors.Is(err, item.ErrItemCategoryNotFound), errors.Is(err, item.ErrItemNotFound):
		return kratoserrors.NotFound("GAME_ITEM_NOT_FOUND", "道具资料、分类或维护窗口不存在")
	case errors.Is(err, item.ErrItemVersionConflict), errors.Is(err, item.ErrItemCodeConflict), errors.Is(err, item.ErrItemReferenced), errors.Is(err, item.ErrItemVersionConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("GAME_ITEM_CONFLICT", "道具资料状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "道具资料 Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
