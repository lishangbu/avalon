package api

import (
	"context"
	"errors"
	"strconv"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/skillcategory"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListGameSkillCategories 分页查询维护窗口中的技能元分类。
func (service *KratosService) ListGameSkillCategories(ctx context.Context, request *domainv1.ListGameSkillCategoriesRequest) (*domainv1.ListGameSkillCategoriesResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	sortValue := request.GetSort()
	if sortValue == "" {
		sortValue = string(skillcategory.SortCodeAscending)
	}
	result, err := service.services.SkillCategories.List(ctx, skillcategory.ListQuery{
		Page: page, PageSize: pageSize, Q: request.GetQ(), Code: request.GetCode(), Name: request.GetName(),
		Description: request.GetDescription(), Enabled: request.Enabled, Sort: skillcategory.Sort(sortValue),
	})
	if err != nil {
		return nil, service.skillCategoryError(ctx, "GAME_SKILL_CATEGORY_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameSkillCategory, len(result.Items))
	for index := range result.Items {
		items[index] = gameSkillCategoryMessage(result.Items[index])
	}
	return &domainv1.ListGameSkillCategoriesResponse{HttpStatusCode: 200, Body: &domainv1.GameSkillCategoryPage{
		Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize,
	}}, nil
}

// CreateGameSkillCategory 在维护窗口中创建独立的技能元分类。
func (service *KratosService) CreateGameSkillCategory(ctx context.Context, request *domainv1.CreateGameSkillCategoryRequest) (*domainv1.CreateGameSkillCategoryResponse, error) {
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
	created, err := service.services.SkillCategories.Create(ctx, skillcategory.CreateCommand{
		GameDataWriteContext: writeContext, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(),
		Description: nullableText(request.GetBody().GetDescription()), Enabled: request.GetBody().GetEnabled(),
	})
	if err != nil {
		return nil, service.skillCategoryError(ctx, "GAME_SKILL_CATEGORY_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameSkillCategoryResponse{HttpStatusCode: 201, Body: gameSkillCategoryMessage(created)}, nil
}

// GetGameSkillCategory 查询维护窗口中指定稳定身份的技能元分类。
func (service *KratosService) GetGameSkillCategory(ctx context.Context, request *domainv1.GetGameSkillCategoryRequest) (*domainv1.GetGameSkillCategoryResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	categoryID, err := gameDataIdentifier(request.GetCategoryId(), "INVALID_SKILL_CATEGORY_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.SkillCategories.Get(ctx, categoryID)
	if err != nil {
		return nil, service.skillCategoryError(ctx, "GAME_SKILL_CATEGORY_QUERY_FAILED", err)
	}
	return &domainv1.GetGameSkillCategoryResponse{HttpStatusCode: 200, Body: gameSkillCategoryMessage(value)}, nil
}

// UpdateGameSkillCategory 使用独立乐观版本更新技能元分类。
func (service *KratosService) UpdateGameSkillCategory(ctx context.Context, request *domainv1.UpdateGameSkillCategoryRequest) (*domainv1.UpdateGameSkillCategoryResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	categoryID, err := gameDataIdentifier(request.GetCategoryId(), "INVALID_SKILL_CATEGORY_ID")
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
	updated, err := service.services.SkillCategories.Update(ctx, skillcategory.UpdateCommand{
		GameDataWriteContext: writeContext, CategoryID: categoryID, ExpectedVersion: version,
		Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(),
		Description: skillcategory.DescriptionChange{Specified: true, Value: nullableText(request.GetBody().GetDescription())},
		Enabled:     request.GetBody().GetEnabled(),
	})
	if err != nil {
		return nil, service.skillCategoryError(ctx, "GAME_SKILL_CATEGORY_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateGameSkillCategoryResponse{HttpStatusCode: 200, Body: gameSkillCategoryMessage(updated)}, nil
}

// DeleteGameSkillCategory 使用独立乐观版本禁用未被引用的技能元分类。
func gameSkillCategoryMessage(value skillcategory.Category) *domainv1.GameSkillCategory {
	description := ""
	if value.Description != nil {
		description = *value.Description
	}
	return &domainv1.GameSkillCategory{Id: value.ID.String(), Code: value.Code, Name: value.Name, Description: description, Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10)}
}

func (service *KratosService) skillCategoryError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, skillcategory.ErrInvalidSkillCategory):
		return kratoserrors.BadRequest("INVALID_GAME_SKILL_CATEGORY", "技能元分类字段无效")
	case errors.Is(err, skillcategory.ErrSkillCategoryNotFound), errors.Is(err, skillcategory.ErrSkillCategoryNotFound):
		return kratoserrors.NotFound("GAME_SKILL_CATEGORY_NOT_FOUND", "技能元分类或维护窗口不存在")
	case errors.Is(err, skillcategory.ErrSkillCategoryVersionConflict), errors.Is(err, skillcategory.ErrSkillCategoryCodeConflict),
		errors.Is(err, skillcategory.ErrSkillCategoryReferenced),
		errors.Is(err, skillcategory.ErrSkillCategoryVersionConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("GAME_SKILL_CATEGORY_CONFLICT", "技能元分类状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "技能元分类 Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
