package api

import (
	"context"
	"errors"
	"strconv"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/elementeffectiveness"
	"github.com/lishangbu/avalon/internal/gamedata/nature"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListGameElementEffectiveness 分页查询非中性属性克制倍率。
func (service *KratosService) ListGameElementEffectiveness(ctx context.Context, request *domainv1.ListGameElementEffectivenessRequest) (*domainv1.ListGameElementEffectivenessResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	attackID, err := optionalGameDataIdentifier(request.GetAttackElementId(), "INVALID_ATTACK_ELEMENT_ID")
	if err != nil {
		return nil, err
	}
	defenseID, err := optionalGameDataIdentifier(request.GetDefenseElementId(), "INVALID_DEFENSE_ELEMENT_ID")
	if err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	result, err := service.services.ElementEffectiveness.List(ctx, elementeffectiveness.ListQuery{Page: page, PageSize: pageSize, AttackElementID: attackID, DefenseElementID: defenseID, Enabled: request.Enabled})
	if err != nil {
		return nil, service.elementEffectivenessError(ctx, "GAME_ELEMENT_EFFECTIVENESS_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameElementEffectiveness, len(result.Items))
	for index, value := range result.Items {
		items[index] = elementEffectivenessMessage(value)
	}
	return &domainv1.ListGameElementEffectivenessResponse{HttpStatusCode: 200, Body: &domainv1.GameElementEffectivenessPage{Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize}}, nil
}

// CreateGameElementEffectiveness 在维护窗口中创建属性克制倍率。
func (service *KratosService) CreateGameElementEffectiveness(ctx context.Context, request *domainv1.CreateGameElementEffectivenessRequest) (*domainv1.CreateGameElementEffectivenessResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	attackID, err := gameDataIdentifier(body.GetAttackElementId(), "INVALID_ATTACK_ELEMENT_ID")
	if err != nil {
		return nil, err
	}
	defenseID, err := gameDataIdentifier(body.GetDefenseElementId(), "INVALID_DEFENSE_ELEMENT_ID")
	if err != nil {
		return nil, err
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	created, err := service.services.ElementEffectiveness.Create(ctx, elementeffectiveness.CreateCommand{GameDataWriteContext: writeContext, AttackElementID: attackID, DefenseElementID: defenseID, Numerator: uint16(body.GetNumerator()), Denominator: uint16(body.GetDenominator()), Enabled: body.GetEnabled()})
	if err != nil {
		return nil, service.elementEffectivenessError(ctx, "GAME_ELEMENT_EFFECTIVENESS_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameElementEffectivenessResponse{HttpStatusCode: 201, Body: elementEffectivenessMessage(created)}, nil
}

// GetGameElementEffectiveness 查询指定属性克制倍率。
func (service *KratosService) GetGameElementEffectiveness(ctx context.Context, request *domainv1.GetGameElementEffectivenessRequest) (*domainv1.GetGameElementEffectivenessResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	id, err := gameDataIdentifier(request.GetEffectivenessId(), "INVALID_EFFECTIVENESS_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.ElementEffectiveness.Get(ctx, id)
	if err != nil {
		return nil, service.elementEffectivenessError(ctx, "GAME_ELEMENT_EFFECTIVENESS_QUERY_FAILED", err)
	}
	return &domainv1.GetGameElementEffectivenessResponse{HttpStatusCode: 200, Body: elementEffectivenessMessage(value)}, nil
}

// UpdateGameElementEffectiveness 使用乐观版本完整更新属性克制倍率。
func (service *KratosService) UpdateGameElementEffectiveness(ctx context.Context, request *domainv1.UpdateGameElementEffectivenessRequest) (*domainv1.UpdateGameElementEffectivenessResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	id, err := gameDataIdentifier(request.GetEffectivenessId(), "INVALID_EFFECTIVENESS_ID")
	if err != nil {
		return nil, err
	}
	attackID, err := gameDataIdentifier(body.GetAttackElementId(), "INVALID_ATTACK_ELEMENT_ID")
	if err != nil {
		return nil, err
	}
	defenseID, err := gameDataIdentifier(body.GetDefenseElementId(), "INVALID_DEFENSE_ELEMENT_ID")
	if err != nil {
		return nil, err
	}
	version, err := gameDataVersion(body.GetExpectedVersion())
	if err != nil {
		return nil, err
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	updated, err := service.services.ElementEffectiveness.Update(ctx, elementeffectiveness.UpdateCommand{GameDataWriteContext: writeContext, ID: id, ExpectedVersion: version, AttackElementID: attackID, DefenseElementID: defenseID, Numerator: uint16(body.GetNumerator()), Denominator: uint16(body.GetDenominator()), Enabled: body.GetEnabled()})
	if err != nil {
		return nil, service.elementEffectivenessError(ctx, "GAME_ELEMENT_EFFECTIVENESS_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateGameElementEffectivenessResponse{HttpStatusCode: 200, Body: elementEffectivenessMessage(updated)}, nil
}

// ListGameNatures 分页查询 Nature 资料。
func (service *KratosService) ListGameNatures(ctx context.Context, request *domainv1.ListGameNaturesRequest) (*domainv1.ListGameNaturesResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	result, err := service.services.Natures.List(ctx, nature.ListQuery{Page: page, PageSize: pageSize, Q: request.GetQ(), Code: request.GetCode(), Name: request.GetName(), Enabled: request.Enabled})
	if err != nil {
		return nil, service.natureError(ctx, "GAME_NATURE_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameNature, len(result.Items))
	for index, value := range result.Items {
		items[index] = natureMessage(value)
	}
	return &domainv1.ListGameNaturesResponse{HttpStatusCode: 200, Body: &domainv1.GameNaturePage{Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize}}, nil
}

// CreateGameNature 在维护窗口中创建 Nature。
func (service *KratosService) CreateGameNature(ctx context.Context, request *domainv1.CreateGameNatureRequest) (*domainv1.CreateGameNatureResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	increased, err := optionalGameDataIdentifier(body.GetIncreasedStatId(), "INVALID_INCREASED_STAT_ID")
	if err != nil {
		return nil, err
	}
	decreased, err := optionalGameDataIdentifier(body.GetDecreasedStatId(), "INVALID_DECREASED_STAT_ID")
	if err != nil {
		return nil, err
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	created, err := service.services.Natures.Create(ctx, nature.CreateCommand{GameDataWriteContext: writeContext, Code: body.GetCode(), Name: body.GetName(), IncreasedStatID: increased, DecreasedStatID: decreased, Enabled: body.GetEnabled()})
	if err != nil {
		return nil, service.natureError(ctx, "GAME_NATURE_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameNatureResponse{HttpStatusCode: 201, Body: natureMessage(created)}, nil
}

// GetGameNature 查询指定 Nature。
func (service *KratosService) GetGameNature(ctx context.Context, request *domainv1.GetGameNatureRequest) (*domainv1.GetGameNatureResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	id, err := gameDataIdentifier(request.GetNatureId(), "INVALID_NATURE_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.Natures.Get(ctx, id)
	if err != nil {
		return nil, service.natureError(ctx, "GAME_NATURE_QUERY_FAILED", err)
	}
	return &domainv1.GetGameNatureResponse{HttpStatusCode: 200, Body: natureMessage(value)}, nil
}

// UpdateGameNature 使用乐观版本完整更新 Nature。
func (service *KratosService) UpdateGameNature(ctx context.Context, request *domainv1.UpdateGameNatureRequest) (*domainv1.UpdateGameNatureResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	id, err := gameDataIdentifier(request.GetNatureId(), "INVALID_NATURE_ID")
	if err != nil {
		return nil, err
	}
	version, err := gameDataVersion(body.GetExpectedVersion())
	if err != nil {
		return nil, err
	}
	increased, err := optionalGameDataIdentifier(body.GetIncreasedStatId(), "INVALID_INCREASED_STAT_ID")
	if err != nil {
		return nil, err
	}
	decreased, err := optionalGameDataIdentifier(body.GetDecreasedStatId(), "INVALID_DECREASED_STAT_ID")
	if err != nil {
		return nil, err
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	updated, err := service.services.Natures.Update(ctx, nature.UpdateCommand{GameDataWriteContext: writeContext, ID: id, ExpectedVersion: version, Code: body.GetCode(), Name: body.GetName(), IncreasedStatID: increased, DecreasedStatID: decreased, Enabled: body.GetEnabled()})
	if err != nil {
		return nil, service.natureError(ctx, "GAME_NATURE_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateGameNatureResponse{HttpStatusCode: 200, Body: natureMessage(updated)}, nil
}

func elementEffectivenessMessage(value elementeffectiveness.Effectiveness) *domainv1.GameElementEffectiveness {
	return &domainv1.GameElementEffectiveness{Id: value.ID.String(), AttackElementId: value.AttackElementID.String(), DefenseElementId: value.DefenseElementID.String(), Numerator: int32(value.Numerator), Denominator: int32(value.Denominator), Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10)}
}
func natureMessage(value nature.Nature) *domainv1.GameNature {
	return &domainv1.GameNature{Id: value.ID.String(), Code: value.Code, Name: value.Name, IncreasedStatId: optionalIdentifierString(value.IncreasedStatID), DecreasedStatId: optionalIdentifierString(value.DecreasedStatID), Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10)}
}
func (service *KratosService) elementEffectivenessError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, elementeffectiveness.ErrInvalidEffectiveness):
		return kratoserrors.BadRequest("INVALID_GAME_ELEMENT_EFFECTIVENESS", "属性克制资料字段无效")
	case errors.Is(err, elementeffectiveness.ErrEffectivenessNotFound), errors.Is(err, elementeffectiveness.ErrEffectivenessNotFound):
		return kratoserrors.NotFound("GAME_ELEMENT_EFFECTIVENESS_NOT_FOUND", "属性克制资料或维护窗口不存在")
	case errors.Is(err, elementeffectiveness.ErrEffectivenessConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("GAME_ELEMENT_EFFECTIVENESS_CONFLICT", "属性克制资料状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "属性克制 Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
func (service *KratosService) natureError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, nature.ErrInvalidNature):
		return kratoserrors.BadRequest("INVALID_GAME_NATURE", "Nature 资料字段无效")
	case errors.Is(err, nature.ErrNatureNotFound), errors.Is(err, nature.ErrNatureNotFound):
		return kratoserrors.NotFound("GAME_NATURE_NOT_FOUND", "Nature 或维护窗口不存在")
	case errors.Is(err, nature.ErrNatureConflict), errors.Is(err, nature.ErrNatureReferenced), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("GAME_NATURE_CONFLICT", "Nature 状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "Nature Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
