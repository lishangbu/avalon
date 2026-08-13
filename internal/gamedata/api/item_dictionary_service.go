package api

import (
	"context"
	"errors"
	"strconv"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/itemdictionary"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListGameItemPockets 读取全部道具 Pocket。
func (service *KratosService) ListGameItemPockets(ctx context.Context, _ *domainv1.ListGameItemPocketsRequest) (*domainv1.ListGameItemPocketsResponse, error) {
	items, err := service.listItemDictionary(ctx, itemdictionary.KindPocket)
	if err != nil {
		return nil, err
	}
	body := make([]*domainv1.GameItemPocket, len(items))
	for i, value := range items {
		body[i] = pocketMessage(value)
	}
	return &domainv1.ListGameItemPocketsResponse{HttpStatusCode: 200, Body: &domainv1.GameItemPocketPage{Items: body}}, nil
}

// CreateGameItemPocket 创建道具 Pocket。
func (service *KratosService) CreateGameItemPocket(ctx context.Context, request *domainv1.CreateGameItemPocketRequest) (*domainv1.CreateGameItemPocketResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	value, err := service.createItemDictionary(ctx, request.GetHeaderIdempotencyKey(), itemdictionary.CreateCommand{Kind: itemdictionary.KindPocket, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(), SortOrder: request.GetBody().GetSortOrder(), Enabled: request.GetBody().GetEnabled()})
	if err != nil {
		return nil, err
	}
	return &domainv1.CreateGameItemPocketResponse{HttpStatusCode: 201, Body: pocketMessage(value)}, nil
}

// UpdateGameItemPocket 更新道具 Pocket。
func (service *KratosService) UpdateGameItemPocket(ctx context.Context, request *domainv1.UpdateGameItemPocketRequest) (*domainv1.UpdateGameItemPocketResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	value, err := service.updateItemDictionary(ctx, request.GetHeaderIdempotencyKey(), request.GetPocketId(), request.GetBody().GetExpectedVersion(), itemdictionary.Entry{Kind: itemdictionary.KindPocket, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(), SortOrder: request.GetBody().GetSortOrder(), Enabled: request.GetBody().GetEnabled()})
	if err != nil {
		return nil, err
	}
	return &domainv1.UpdateGameItemPocketResponse{HttpStatusCode: 200, Body: pocketMessage(value)}, nil
}

// ListGameItemAttributes 读取全部道具 Attribute。
func (service *KratosService) ListGameItemAttributes(ctx context.Context, _ *domainv1.ListGameItemAttributesRequest) (*domainv1.ListGameItemAttributesResponse, error) {
	items, err := service.listItemDictionary(ctx, itemdictionary.KindAttribute)
	if err != nil {
		return nil, err
	}
	body := make([]*domainv1.GameItemAttribute, len(items))
	for i, value := range items {
		body[i] = attributeMessage(value)
	}
	return &domainv1.ListGameItemAttributesResponse{HttpStatusCode: 200, Body: &domainv1.GameItemAttributePage{Items: body}}, nil
}

// CreateGameItemAttribute 创建道具 Attribute。
func (service *KratosService) CreateGameItemAttribute(ctx context.Context, request *domainv1.CreateGameItemAttributeRequest) (*domainv1.CreateGameItemAttributeResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	value, err := service.createItemDictionary(ctx, request.GetHeaderIdempotencyKey(), itemdictionary.CreateCommand{Kind: itemdictionary.KindAttribute, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(), Description: nullableText(request.GetBody().GetDescription()), Enabled: request.GetBody().GetEnabled()})
	if err != nil {
		return nil, err
	}
	return &domainv1.CreateGameItemAttributeResponse{HttpStatusCode: 201, Body: attributeMessage(value)}, nil
}

// UpdateGameItemAttribute 更新道具 Attribute。
func (service *KratosService) UpdateGameItemAttribute(ctx context.Context, request *domainv1.UpdateGameItemAttributeRequest) (*domainv1.UpdateGameItemAttributeResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	value, err := service.updateItemDictionary(ctx, request.GetHeaderIdempotencyKey(), request.GetAttributeId(), request.GetBody().GetExpectedVersion(), itemdictionary.Entry{Kind: itemdictionary.KindAttribute, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(), Description: nullableText(request.GetBody().GetDescription()), Enabled: request.GetBody().GetEnabled()})
	if err != nil {
		return nil, err
	}
	return &domainv1.UpdateGameItemAttributeResponse{HttpStatusCode: 200, Body: attributeMessage(value)}, nil
}

// ListGameItemFlingEffects 读取全部投掷效果。
func (service *KratosService) ListGameItemFlingEffects(ctx context.Context, _ *domainv1.ListGameItemFlingEffectsRequest) (*domainv1.ListGameItemFlingEffectsResponse, error) {
	items, err := service.listItemDictionary(ctx, itemdictionary.KindFlingEffect)
	if err != nil {
		return nil, err
	}
	body := make([]*domainv1.GameItemFlingEffect, len(items))
	for i, value := range items {
		body[i] = flingEffectMessage(value)
	}
	return &domainv1.ListGameItemFlingEffectsResponse{HttpStatusCode: 200, Body: &domainv1.GameItemFlingEffectPage{Items: body}}, nil
}

// CreateGameItemFlingEffect 创建投掷效果。
func (service *KratosService) CreateGameItemFlingEffect(ctx context.Context, request *domainv1.CreateGameItemFlingEffectRequest) (*domainv1.CreateGameItemFlingEffectResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	value, err := service.createItemDictionary(ctx, request.GetHeaderIdempotencyKey(), itemdictionary.CreateCommand{Kind: itemdictionary.KindFlingEffect, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(), Description: nullableText(request.GetBody().GetEffect()), Enabled: request.GetBody().GetEnabled()})
	if err != nil {
		return nil, err
	}
	return &domainv1.CreateGameItemFlingEffectResponse{HttpStatusCode: 201, Body: flingEffectMessage(value)}, nil
}

// UpdateGameItemFlingEffect 更新投掷效果。
func (service *KratosService) UpdateGameItemFlingEffect(ctx context.Context, request *domainv1.UpdateGameItemFlingEffectRequest) (*domainv1.UpdateGameItemFlingEffectResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	value, err := service.updateItemDictionary(ctx, request.GetHeaderIdempotencyKey(), request.GetFlingEffectId(), request.GetBody().GetExpectedVersion(), itemdictionary.Entry{Kind: itemdictionary.KindFlingEffect, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(), Description: nullableText(request.GetBody().GetEffect()), Enabled: request.GetBody().GetEnabled()})
	if err != nil {
		return nil, err
	}
	return &domainv1.UpdateGameItemFlingEffectResponse{HttpStatusCode: 200, Body: flingEffectMessage(value)}, nil
}

func (service *KratosService) listItemDictionary(ctx context.Context, kind itemdictionary.Kind) ([]itemdictionary.Entry, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	values, err := service.services.ItemDictionaries.List(ctx, kind)
	if err != nil {
		return nil, service.itemDictionaryError(ctx, err)
	}
	return values, nil
}
func (service *KratosService) createItemDictionary(ctx context.Context, key string, command itemdictionary.CreateCommand) (itemdictionary.Entry, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return itemdictionary.Entry{}, err
	}
	command.GameDataWriteContext, err = gameDataWriteContext(ctx, principal.AccountID, key)
	if err != nil {
		return itemdictionary.Entry{}, err
	}
	value, err := service.services.ItemDictionaries.Create(ctx, command)
	if err != nil {
		return itemdictionary.Entry{}, service.itemDictionaryError(ctx, err)
	}
	return value, nil
}
func (service *KratosService) updateItemDictionary(ctx context.Context, key, rawID, rawVersion string, entry itemdictionary.Entry) (itemdictionary.Entry, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return itemdictionary.Entry{}, err
	}
	entry.ID, err = gameDataIdentifier(rawID, "INVALID_ITEM_DICTIONARY_ID")
	if err != nil {
		return itemdictionary.Entry{}, err
	}
	version, err := gameDataVersion(rawVersion)
	if err != nil {
		return itemdictionary.Entry{}, err
	}
	write, err := gameDataWriteContext(ctx, principal.AccountID, key)
	if err != nil {
		return itemdictionary.Entry{}, err
	}
	value, err := service.services.ItemDictionaries.Update(ctx, itemdictionary.UpdateCommand{GameDataWriteContext: write, Entry: entry, ExpectedVersion: version})
	if err != nil {
		return itemdictionary.Entry{}, service.itemDictionaryError(ctx, err)
	}
	return value, nil
}
func (service *KratosService) itemDictionaryError(ctx context.Context, err error) error {
	switch {
	case errors.Is(err, itemdictionary.ErrInvalid):
		return kratoserrors.BadRequest("INVALID_ITEM_DICTIONARY", "道具字典字段无效")
	case errors.Is(err, itemdictionary.ErrNotFound):
		return kratoserrors.NotFound("ITEM_DICTIONARY_NOT_FOUND", "道具字典记录不存在")
	case errors.Is(err, itemdictionary.ErrConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("ITEM_DICTIONARY_CONFLICT", "道具字典编码、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "道具字典服务调用失败", "error", err)
		return kratoserrors.InternalServer("ITEM_DICTIONARY_FAILED", "服务端无法完成请求")
	}
}
func pocketMessage(value itemdictionary.Entry) *domainv1.GameItemPocket {
	return &domainv1.GameItemPocket{Id: value.ID.String(), Code: value.Code, Name: value.Name, SortOrder: value.SortOrder, Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10)}
}
func attributeMessage(value itemdictionary.Entry) *domainv1.GameItemAttribute {
	return &domainv1.GameItemAttribute{Id: value.ID.String(), Code: value.Code, Name: value.Name, Description: itemDictionaryText(value.Description), Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10)}
}
func flingEffectMessage(value itemdictionary.Entry) *domainv1.GameItemFlingEffect {
	return &domainv1.GameItemFlingEffect{Id: value.ID.String(), Code: value.Code, Name: value.Name, Effect: itemDictionaryText(value.Description), Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10)}
}

func itemDictionaryText(value *string) string {
	if value == nil {
		return ""
	}
	return *value
}
