package api

import (
	"context"
	"errors"
	"strconv"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/skillailment"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListGameSkillAilments 分页查询维护窗口中的技能异常资料。
func (service *KratosService) ListGameSkillAilments(
	ctx context.Context,
	request *domainv1.ListGameSkillAilmentsRequest,
) (*domainv1.ListGameSkillAilmentsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	sortValue := request.GetSort()
	if sortValue == "" {
		sortValue = string(skillailment.SortCodeAscending)
	}
	result, err := service.services.SkillAilments.List(ctx, skillailment.ListQuery{
		Page: page, PageSize: pageSize, Q: request.GetQ(), Code: request.GetCode(), Name: request.GetName(),
		Enabled: request.Enabled, Sort: skillailment.Sort(sortValue),
	})
	if err != nil {
		return nil, service.skillAilmentError(ctx, "GAME_SKILL_AILMENT_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameSkillAilment, len(result.Items))
	for index := range result.Items {
		items[index] = gameSkillAilmentMessage(result.Items[index])
	}
	return &domainv1.ListGameSkillAilmentsResponse{HttpStatusCode: 200, Body: &domainv1.GameSkillAilmentPage{
		Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize,
	}}, nil
}

// CreateGameSkillAilment 在维护窗口中创建独立技能异常资料。
func (service *KratosService) CreateGameSkillAilment(
	ctx context.Context,
	request *domainv1.CreateGameSkillAilmentRequest,
) (*domainv1.CreateGameSkillAilmentResponse, error) {
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
	created, err := service.services.SkillAilments.Create(ctx, skillailment.CreateCommand{
		GameDataWriteContext: writeContext, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(),
		Enabled: request.GetBody().GetEnabled(),
	})
	if err != nil {
		return nil, service.skillAilmentError(ctx, "GAME_SKILL_AILMENT_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameSkillAilmentResponse{HttpStatusCode: 201, Body: gameSkillAilmentMessage(created)}, nil
}

// GetGameSkillAilment 查询维护窗口中指定稳定身份的技能异常资料。
func (service *KratosService) GetGameSkillAilment(
	ctx context.Context,
	request *domainv1.GetGameSkillAilmentRequest,
) (*domainv1.GetGameSkillAilmentResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	ailmentID, err := gameDataIdentifier(request.GetAilmentId(), "INVALID_SKILL_AILMENT_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.SkillAilments.Get(ctx, ailmentID)
	if err != nil {
		return nil, service.skillAilmentError(ctx, "GAME_SKILL_AILMENT_QUERY_FAILED", err)
	}
	return &domainv1.GetGameSkillAilmentResponse{HttpStatusCode: 200, Body: gameSkillAilmentMessage(value)}, nil
}

// UpdateGameSkillAilment 使用独立乐观版本更新技能异常资料。
func (service *KratosService) UpdateGameSkillAilment(
	ctx context.Context,
	request *domainv1.UpdateGameSkillAilmentRequest,
) (*domainv1.UpdateGameSkillAilmentResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	ailmentID, err := gameDataIdentifier(request.GetAilmentId(), "INVALID_SKILL_AILMENT_ID")
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
	updated, err := service.services.SkillAilments.Update(ctx, skillailment.UpdateCommand{
		GameDataWriteContext: writeContext, AilmentID: ailmentID, ExpectedVersion: version,
		Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(), Enabled: request.GetBody().GetEnabled(),
	})
	if err != nil {
		return nil, service.skillAilmentError(ctx, "GAME_SKILL_AILMENT_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateGameSkillAilmentResponse{HttpStatusCode: 200, Body: gameSkillAilmentMessage(updated)}, nil
}

// DeleteGameSkillAilment 使用独立乐观版本禁用未被引用的技能异常资料。
func gameSkillAilmentMessage(value skillailment.Ailment) *domainv1.GameSkillAilment {
	return &domainv1.GameSkillAilment{
		Id: value.ID.String(), Code: value.Code, Name: value.Name, Enabled: value.Enabled,
		Version: strconv.FormatInt(value.Version, 10),
	}
}

func (service *KratosService) skillAilmentError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, skillailment.ErrInvalidSkillAilment):
		return kratoserrors.BadRequest("INVALID_GAME_SKILL_AILMENT", "技能异常资料字段无效")
	case errors.Is(err, skillailment.ErrSkillAilmentNotFound), errors.Is(err, skillailment.ErrSkillAilmentNotFound):
		return kratoserrors.NotFound("GAME_SKILL_AILMENT_NOT_FOUND", "技能异常资料或维护窗口不存在")
	case errors.Is(err, skillailment.ErrSkillAilmentVersionConflict), errors.Is(err, skillailment.ErrSkillAilmentCodeConflict),
		errors.Is(err, skillailment.ErrSkillAilmentReferenced),
		errors.Is(err, skillailment.ErrSkillAilmentVersionConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("GAME_SKILL_AILMENT_CONFLICT", "技能异常资料状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "技能异常资料 Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
