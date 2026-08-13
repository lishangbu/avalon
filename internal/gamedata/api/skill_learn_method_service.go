package api

import (
	"context"
	"errors"
	"strconv"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/skilllearnmethod"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListGameSkillLearnMethods 分页查询维护窗口中的技能学习方式。
func (service *KratosService) ListGameSkillLearnMethods(ctx context.Context, request *domainv1.ListGameSkillLearnMethodsRequest) (*domainv1.ListGameSkillLearnMethodsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	sortValue := request.GetSort()
	if sortValue == "" {
		sortValue = string(skilllearnmethod.SortCodeAscending)
	}
	result, err := service.services.SkillLearnMethods.List(ctx, skilllearnmethod.ListQuery{Page: page, PageSize: pageSize, Q: request.GetQ(), Code: request.GetCode(), Name: request.GetName(), Description: request.GetDescription(), Enabled: request.Enabled, Sort: skilllearnmethod.Sort(sortValue)})
	if err != nil {
		return nil, service.skillLearnMethodError(ctx, "GAME_SKILL_LEARN_METHOD_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameSkillLearnMethod, len(result.Items))
	for index := range result.Items {
		items[index] = gameSkillLearnMethodMessage(result.Items[index])
	}
	return &domainv1.ListGameSkillLearnMethodsResponse{HttpStatusCode: 200, Body: &domainv1.GameSkillLearnMethodPage{Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize}}, nil
}

// CreateGameSkillLearnMethod 在维护窗口中创建独立的技能学习方式。
func (service *KratosService) CreateGameSkillLearnMethod(ctx context.Context, request *domainv1.CreateGameSkillLearnMethodRequest) (*domainv1.CreateGameSkillLearnMethodResponse, error) {
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
	created, err := service.services.SkillLearnMethods.Create(ctx, skilllearnmethod.CreateCommand{GameDataWriteContext: writeContext, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(), Description: nullableText(request.GetBody().GetDescription()), Enabled: request.GetBody().GetEnabled()})
	if err != nil {
		return nil, service.skillLearnMethodError(ctx, "GAME_SKILL_LEARN_METHOD_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameSkillLearnMethodResponse{HttpStatusCode: 201, Body: gameSkillLearnMethodMessage(created)}, nil
}

// GetGameSkillLearnMethod 查询维护窗口中指定稳定身份的技能学习方式。
func (service *KratosService) GetGameSkillLearnMethod(ctx context.Context, request *domainv1.GetGameSkillLearnMethodRequest) (*domainv1.GetGameSkillLearnMethodResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	methodID, err := gameDataIdentifier(request.GetMethodId(), "INVALID_SKILL_LEARN_METHOD_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.SkillLearnMethods.Get(ctx, methodID)
	if err != nil {
		return nil, service.skillLearnMethodError(ctx, "GAME_SKILL_LEARN_METHOD_QUERY_FAILED", err)
	}
	return &domainv1.GetGameSkillLearnMethodResponse{HttpStatusCode: 200, Body: gameSkillLearnMethodMessage(value)}, nil
}

// UpdateGameSkillLearnMethod 使用独立乐观版本更新技能学习方式。
func (service *KratosService) UpdateGameSkillLearnMethod(ctx context.Context, request *domainv1.UpdateGameSkillLearnMethodRequest) (*domainv1.UpdateGameSkillLearnMethodResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	methodID, err := gameDataIdentifier(request.GetMethodId(), "INVALID_SKILL_LEARN_METHOD_ID")
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
	updated, err := service.services.SkillLearnMethods.Update(ctx, skilllearnmethod.UpdateCommand{GameDataWriteContext: writeContext, MethodID: methodID, ExpectedVersion: version, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(), Description: skilllearnmethod.DescriptionChange{Specified: true, Value: nullableText(request.GetBody().GetDescription())}, Enabled: request.GetBody().GetEnabled()})
	if err != nil {
		return nil, service.skillLearnMethodError(ctx, "GAME_SKILL_LEARN_METHOD_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateGameSkillLearnMethodResponse{HttpStatusCode: 200, Body: gameSkillLearnMethodMessage(updated)}, nil
}

// DeleteGameSkillLearnMethod 使用独立乐观版本禁用未被引用的技能学习方式。
func gameSkillLearnMethodMessage(value skilllearnmethod.Method) *domainv1.GameSkillLearnMethod {
	description := ""
	if value.Description != nil {
		description = *value.Description
	}
	return &domainv1.GameSkillLearnMethod{Id: value.ID.String(), Code: value.Code, Name: value.Name, Description: description, Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10)}
}

func (service *KratosService) skillLearnMethodError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, skilllearnmethod.ErrInvalidSkillLearnMethod):
		return kratoserrors.BadRequest("INVALID_GAME_SKILL_LEARN_METHOD", "技能学习方式字段无效")
	case errors.Is(err, skilllearnmethod.ErrSkillLearnMethodNotFound), errors.Is(err, skilllearnmethod.ErrSkillLearnMethodNotFound):
		return kratoserrors.NotFound("GAME_SKILL_LEARN_METHOD_NOT_FOUND", "技能学习方式或维护窗口不存在")
	case errors.Is(err, skilllearnmethod.ErrSkillLearnMethodVersionConflict), errors.Is(err, skilllearnmethod.ErrSkillLearnMethodCodeConflict), errors.Is(err, skilllearnmethod.ErrSkillLearnMethodReferenced), errors.Is(err, skilllearnmethod.ErrSkillLearnMethodVersionConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("GAME_SKILL_LEARN_METHOD_CONFLICT", "技能学习方式状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "技能学习方式 Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
