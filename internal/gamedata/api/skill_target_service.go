package api

import (
	"context"
	"errors"
	"strconv"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/skilltarget"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListGameSkillTargets 分页查询维护窗口中的技能目标。
func (service *KratosService) ListGameSkillTargets(ctx context.Context, request *domainv1.ListGameSkillTargetsRequest) (*domainv1.ListGameSkillTargetsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	sortValue := request.GetSort()
	if sortValue == "" {
		sortValue = string(skilltarget.SortCodeAscending)
	}
	result, err := service.services.SkillTargets.List(ctx, skilltarget.ListQuery{
		Page: page, PageSize: pageSize, Q: request.GetQ(), Code: request.GetCode(), Name: request.GetName(),
		Description: request.GetDescription(), Enabled: request.Enabled, Sort: skilltarget.Sort(sortValue),
	})
	if err != nil {
		return nil, service.skillTargetError(ctx, "GAME_SKILL_TARGET_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameSkillTarget, len(result.Items))
	for index := range result.Items {
		items[index] = gameSkillTargetMessage(result.Items[index])
	}
	return &domainv1.ListGameSkillTargetsResponse{HttpStatusCode: 200, Body: &domainv1.GameSkillTargetPage{Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize}}, nil
}

// CreateGameSkillTarget 在维护窗口中创建独立的技能目标。
func (service *KratosService) CreateGameSkillTarget(ctx context.Context, request *domainv1.CreateGameSkillTargetRequest) (*domainv1.CreateGameSkillTargetResponse, error) {
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
	created, err := service.services.SkillTargets.Create(ctx, skilltarget.CreateCommand{GameDataWriteContext: writeContext, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(), Description: nullableText(request.GetBody().GetDescription()), Enabled: request.GetBody().GetEnabled()})
	if err != nil {
		return nil, service.skillTargetError(ctx, "GAME_SKILL_TARGET_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameSkillTargetResponse{HttpStatusCode: 201, Body: gameSkillTargetMessage(created)}, nil
}

// GetGameSkillTarget 查询维护窗口中指定稳定身份的技能目标。
func (service *KratosService) GetGameSkillTarget(ctx context.Context, request *domainv1.GetGameSkillTargetRequest) (*domainv1.GetGameSkillTargetResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	targetID, err := gameDataIdentifier(request.GetTargetId(), "INVALID_SKILL_TARGET_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.SkillTargets.Get(ctx, targetID)
	if err != nil {
		return nil, service.skillTargetError(ctx, "GAME_SKILL_TARGET_QUERY_FAILED", err)
	}
	return &domainv1.GetGameSkillTargetResponse{HttpStatusCode: 200, Body: gameSkillTargetMessage(value)}, nil
}

// UpdateGameSkillTarget 使用独立乐观版本更新技能目标。
func (service *KratosService) UpdateGameSkillTarget(ctx context.Context, request *domainv1.UpdateGameSkillTargetRequest) (*domainv1.UpdateGameSkillTargetResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	targetID, err := gameDataIdentifier(request.GetTargetId(), "INVALID_SKILL_TARGET_ID")
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
	updated, err := service.services.SkillTargets.Update(ctx, skilltarget.UpdateCommand{GameDataWriteContext: writeContext, TargetID: targetID, ExpectedVersion: version, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(), Description: skilltarget.DescriptionChange{Specified: true, Value: nullableText(request.GetBody().GetDescription())}, Enabled: request.GetBody().GetEnabled()})
	if err != nil {
		return nil, service.skillTargetError(ctx, "GAME_SKILL_TARGET_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateGameSkillTargetResponse{HttpStatusCode: 200, Body: gameSkillTargetMessage(updated)}, nil
}

// DeleteGameSkillTarget 使用独立乐观版本禁用未被引用的技能目标。
func gameSkillTargetMessage(value skilltarget.Target) *domainv1.GameSkillTarget {
	description := ""
	if value.Description != nil {
		description = *value.Description
	}
	return &domainv1.GameSkillTarget{Id: value.ID.String(), Code: value.Code, Name: value.Name, Description: description, Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10)}
}

func (service *KratosService) skillTargetError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, skilltarget.ErrInvalidSkillTarget):
		return kratoserrors.BadRequest("INVALID_GAME_SKILL_TARGET", "技能目标字段无效")
	case errors.Is(err, skilltarget.ErrSkillTargetNotFound), errors.Is(err, skilltarget.ErrSkillTargetNotFound):
		return kratoserrors.NotFound("GAME_SKILL_TARGET_NOT_FOUND", "技能目标或维护窗口不存在")
	case errors.Is(err, skilltarget.ErrSkillTargetVersionConflict), errors.Is(err, skilltarget.ErrSkillTargetCodeConflict), errors.Is(err, skilltarget.ErrSkillTargetReferenced), errors.Is(err, skilltarget.ErrSkillTargetVersionConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("GAME_SKILL_TARGET_CONFLICT", "技能目标状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "技能目标 Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
