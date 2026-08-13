package api

import (
	"context"
	"errors"
	"strconv"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/skilldamageclass"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListGameSkillDamageClasses 分页查询维护窗口中的技能伤害分类。
func (service *KratosService) ListGameSkillDamageClasses(
	ctx context.Context,
	request *domainv1.ListGameSkillDamageClassesRequest,
) (*domainv1.ListGameSkillDamageClassesResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	sortValue := request.GetSort()
	if sortValue == "" {
		sortValue = string(skilldamageclass.SortCodeAscending)
	}
	result, err := service.services.DamageClasses.List(ctx, skilldamageclass.ListQuery{
		Page: page, PageSize: pageSize, Q: request.GetQ(), Code: request.GetCode(),
		Name: request.GetName(), Description: request.GetDescription(),
		SortOrder: optionalInt32(request.GetSortOrder()), Enabled: request.Enabled,
		Sort: skilldamageclass.Sort(sortValue),
	})
	if err != nil {
		return nil, service.skillDamageClassError(ctx, "GAME_SKILL_DAMAGE_CLASS_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameSkillDamageClass, len(result.Items))
	for index := range result.Items {
		items[index] = gameSkillDamageClassMessage(result.Items[index])
	}
	return &domainv1.ListGameSkillDamageClassesResponse{HttpStatusCode: 200, Body: &domainv1.GameSkillDamageClassPage{
		Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize,
	}}, nil
}

// CreateGameSkillDamageClass 在维护窗口中创建独立的技能伤害分类。
func (service *KratosService) CreateGameSkillDamageClass(
	ctx context.Context,
	request *domainv1.CreateGameSkillDamageClassRequest,
) (*domainv1.CreateGameSkillDamageClassResponse, error) {
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
	created, err := service.services.DamageClasses.Create(ctx, skilldamageclass.CreateCommand{
		GameDataWriteContext: writeContext, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(),
		Description: nullableText(request.GetBody().GetDescription()), SortOrder: request.GetBody().GetSortOrder(),
		Enabled: request.GetBody().GetEnabled(),
	})
	if err != nil {
		return nil, service.skillDamageClassError(ctx, "GAME_SKILL_DAMAGE_CLASS_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameSkillDamageClassResponse{HttpStatusCode: 201, Body: gameSkillDamageClassMessage(created)}, nil
}

// GetGameSkillDamageClass 查询维护窗口中指定稳定身份的技能伤害分类。
func (service *KratosService) GetGameSkillDamageClass(
	ctx context.Context,
	request *domainv1.GetGameSkillDamageClassRequest,
) (*domainv1.GetGameSkillDamageClassResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	damageClassID, err := gameDataIdentifier(request.GetDamageClassId(), "INVALID_SKILL_DAMAGE_CLASS_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.DamageClasses.Get(ctx, damageClassID)
	if err != nil {
		return nil, service.skillDamageClassError(ctx, "GAME_SKILL_DAMAGE_CLASS_QUERY_FAILED", err)
	}
	return &domainv1.GetGameSkillDamageClassResponse{HttpStatusCode: 200, Body: gameSkillDamageClassMessage(value)}, nil
}

// UpdateGameSkillDamageClass 使用独立乐观版本更新技能伤害分类。
func (service *KratosService) UpdateGameSkillDamageClass(
	ctx context.Context,
	request *domainv1.UpdateGameSkillDamageClassRequest,
) (*domainv1.UpdateGameSkillDamageClassResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	damageClassID, err := gameDataIdentifier(request.GetDamageClassId(), "INVALID_SKILL_DAMAGE_CLASS_ID")
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
	updated, err := service.services.DamageClasses.Update(ctx, skilldamageclass.UpdateCommand{
		GameDataWriteContext: writeContext, DamageClassID: damageClassID, ExpectedVersion: version,
		Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(),
		Description: skilldamageclass.DescriptionChange{Specified: true, Value: nullableText(request.GetBody().GetDescription())},
		SortOrder:   request.GetBody().GetSortOrder(), Enabled: request.GetBody().GetEnabled(),
	})
	if err != nil {
		return nil, service.skillDamageClassError(ctx, "GAME_SKILL_DAMAGE_CLASS_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateGameSkillDamageClassResponse{HttpStatusCode: 200, Body: gameSkillDamageClassMessage(updated)}, nil
}

// DeleteGameSkillDamageClass 使用独立乐观版本禁用未被引用的技能伤害分类。
// nullableText 把当前过渡 Proto 中的空字符串转换为领域层的空值。
// 最终契约会使用显式三态字段，因此该函数只承担原生服务迁移期间的边界转换。
func nullableText(value string) *string {
	if value == "" {
		return nil
	}
	return &value
}

func gameSkillDamageClassMessage(value skilldamageclass.DamageClass) *domainv1.GameSkillDamageClass {
	description := ""
	if value.Description != nil {
		description = *value.Description
	}
	return &domainv1.GameSkillDamageClass{
		Id: value.ID.String(), Code: value.Code, Name: value.Name, Description: description,
		SortOrder: value.SortOrder, Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10),
	}
}

func (service *KratosService) skillDamageClassError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, skilldamageclass.ErrInvalidSkillDamageClass):
		return kratoserrors.BadRequest("INVALID_GAME_SKILL_DAMAGE_CLASS", "技能伤害分类字段无效")
	case errors.Is(err, skilldamageclass.ErrSkillDamageClassNotFound), errors.Is(err, skilldamageclass.ErrSkillDamageClassNotFound):
		return kratoserrors.NotFound("GAME_SKILL_DAMAGE_CLASS_NOT_FOUND", "技能伤害分类或维护窗口不存在")
	case errors.Is(err, skilldamageclass.ErrSkillDamageClassVersionConflict),
		errors.Is(err, skilldamageclass.ErrSkillDamageClassCodeConflict),
		errors.Is(err, skilldamageclass.ErrSkillDamageClassReferenced),
		errors.Is(err, skilldamageclass.ErrSkillDamageClassVersionConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("GAME_SKILL_DAMAGE_CLASS_CONFLICT", "技能伤害分类状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "技能伤害分类 Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
