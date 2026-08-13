package api

import (
	"context"
	"errors"
	"strconv"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/itemcategory"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListGameItemCategories 分页查询维护窗口中的道具分类。
func (service *KratosService) ListGameItemCategories(
	ctx context.Context,
	request *domainv1.ListGameItemCategoriesRequest,
) (*domainv1.ListGameItemCategoriesResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	sortValue := request.GetSort()
	if sortValue == "" {
		sortValue = string(itemcategory.SortCodeAscending)
	}
	result, err := service.services.ItemCategories.List(ctx, itemcategory.ListQuery{
		Page: page, PageSize: pageSize, Q: request.GetQ(), Code: request.GetCode(), Name: request.GetName(),
		SortOrder: optionalInt32(request.GetSortOrder()), Enabled: request.Enabled,
		Sort: itemcategory.Sort(sortValue),
	})
	if err != nil {
		return nil, service.itemCategoryError(ctx, "GAME_ITEM_CATEGORY_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameItemCategory, len(result.Items))
	for index := range result.Items {
		items[index] = gameItemCategoryMessage(result.Items[index])
	}
	return &domainv1.ListGameItemCategoriesResponse{HttpStatusCode: 200, Body: &domainv1.GameItemCategoryPage{
		Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize,
	}}, nil
}

// CreateGameItemCategory 在维护窗口中创建独立道具分类。
func (service *KratosService) CreateGameItemCategory(
	ctx context.Context,
	request *domainv1.CreateGameItemCategoryRequest,
) (*domainv1.CreateGameItemCategoryResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	pocketID, err := gameDataIdentifier(request.GetBody().GetPocketId(), "INVALID_ITEM_POCKET_ID")
	if err != nil {
		return nil, err
	}
	created, err := service.services.ItemCategories.Create(ctx, itemcategory.CreateCommand{
		GameDataWriteContext: writeContext, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(),
		PocketID: pocketID, SortOrder: request.GetBody().GetSortOrder(), Enabled: request.GetBody().GetEnabled(),
	})
	if err != nil {
		return nil, service.itemCategoryError(ctx, "GAME_ITEM_CATEGORY_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameItemCategoryResponse{HttpStatusCode: 201, Body: gameItemCategoryMessage(created)}, nil
}

// GetGameItemCategory 查询维护窗口中指定稳定身份的道具分类。
func (service *KratosService) GetGameItemCategory(
	ctx context.Context,
	request *domainv1.GetGameItemCategoryRequest,
) (*domainv1.GetGameItemCategoryResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	categoryID, err := gameDataIdentifier(request.GetCategoryId(), "INVALID_ITEM_CATEGORY_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.ItemCategories.Get(ctx, categoryID)
	if err != nil {
		return nil, service.itemCategoryError(ctx, "GAME_ITEM_CATEGORY_QUERY_FAILED", err)
	}
	return &domainv1.GetGameItemCategoryResponse{HttpStatusCode: 200, Body: gameItemCategoryMessage(value)}, nil
}

// UpdateGameItemCategory 使用独立乐观版本更新道具分类。
func (service *KratosService) UpdateGameItemCategory(
	ctx context.Context,
	request *domainv1.UpdateGameItemCategoryRequest,
) (*domainv1.UpdateGameItemCategoryResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	categoryID, err := gameDataIdentifier(request.GetCategoryId(), "INVALID_ITEM_CATEGORY_ID")
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
	pocketID, err := gameDataIdentifier(request.GetBody().GetPocketId(), "INVALID_ITEM_POCKET_ID")
	if err != nil {
		return nil, err
	}
	updated, err := service.services.ItemCategories.Update(ctx, itemcategory.UpdateCommand{
		GameDataWriteContext: writeContext, CategoryID: categoryID, ExpectedVersion: version,
		Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(),
		PocketID: pocketID, SortOrder: request.GetBody().GetSortOrder(), Enabled: request.GetBody().GetEnabled(),
	})
	if err != nil {
		return nil, service.itemCategoryError(ctx, "GAME_ITEM_CATEGORY_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateGameItemCategoryResponse{HttpStatusCode: 200, Body: gameItemCategoryMessage(updated)}, nil
}

// DeleteGameItemCategory 使用独立乐观版本禁用未被引用的道具分类。
func gameItemCategoryMessage(value itemcategory.Category) *domainv1.GameItemCategory {
	return &domainv1.GameItemCategory{
		Id: value.ID.String(), Code: value.Code, Name: value.Name, SortOrder: value.SortOrder,
		PocketId: value.PocketID.String(), Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10),
	}
}

func (service *KratosService) itemCategoryError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, itemcategory.ErrInvalidItemCategory):
		return kratoserrors.BadRequest("INVALID_GAME_ITEM_CATEGORY", "道具分类字段无效")
	case errors.Is(err, itemcategory.ErrItemCategoryNotFound), errors.Is(err, itemcategory.ErrItemCategoryNotFound):
		return kratoserrors.NotFound("GAME_ITEM_CATEGORY_NOT_FOUND", "道具分类或维护窗口不存在")
	case errors.Is(err, itemcategory.ErrItemCategoryVersionConflict), errors.Is(err, itemcategory.ErrItemCategoryCodeConflict),
		errors.Is(err, itemcategory.ErrItemCategoryReferenced),
		errors.Is(err, itemcategory.ErrItemCategoryVersionConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("GAME_ITEM_CATEGORY_CONFLICT", "道具分类状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "道具分类 Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
