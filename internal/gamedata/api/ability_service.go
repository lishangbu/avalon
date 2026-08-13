package api

import (
	"context"
	"errors"
	"strconv"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/ability"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListGameAbilities 分页查询维护窗口中的特性资料。
func (service *KratosService) ListGameAbilities(
	ctx context.Context,
	request *domainv1.ListGameAbilitiesRequest,
) (*domainv1.ListGameAbilitiesResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	sortValue := request.GetSort()
	if sortValue == "" {
		sortValue = string(ability.SortCodeAscending)
	}
	result, err := service.services.Abilities.List(ctx, ability.ListQuery{
		Page: page, PageSize: pageSize, Q: request.GetQ(), Code: request.GetCode(), Name: request.GetName(),
		MainSeries: request.MainSeries, Enabled: request.Enabled, Sort: ability.Sort(sortValue),
	})
	if err != nil {
		return nil, service.abilityError(ctx, "GAME_ABILITY_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameAbility, len(result.Items))
	for index := range result.Items {
		items[index] = gameAbilityMessage(result.Items[index])
	}
	return &domainv1.ListGameAbilitiesResponse{HttpStatusCode: 200, Body: &domainv1.GameAbilityPage{
		Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize,
	}}, nil
}

// CreateGameAbility 在维护窗口中创建一条独立特性资料。
func (service *KratosService) CreateGameAbility(
	ctx context.Context,
	request *domainv1.CreateGameAbilityRequest,
) (*domainv1.CreateGameAbilityResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	rules, err := abilityRulesFromMessage(request.GetBody().GetRules())
	if err != nil {
		return nil, err
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	created, err := service.services.Abilities.Create(ctx, ability.CreateCommand{
		GameDataWriteContext: writeContext, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(),
		MainSeries: request.GetBody().GetMainSeries(), Effect: nullableText(request.GetBody().GetEffect()),
		ShortEffect: nullableText(request.GetBody().GetShortEffect()), Introduction: nullableText(request.GetBody().GetIntroduction()),
		Rules: rules, Enabled: request.GetBody().GetEnabled(),
	})
	if err != nil {
		return nil, service.abilityError(ctx, "GAME_ABILITY_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameAbilityResponse{HttpStatusCode: 201, Body: gameAbilityMessage(created)}, nil
}

// GetGameAbility 查询维护窗口中指定稳定身份的特性资料。
func (service *KratosService) GetGameAbility(
	ctx context.Context,
	request *domainv1.GetGameAbilityRequest,
) (*domainv1.GetGameAbilityResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	abilityID, err := gameDataIdentifier(request.GetAbilityId(), "INVALID_ABILITY_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.Abilities.Get(ctx, abilityID)
	if err != nil {
		return nil, service.abilityError(ctx, "GAME_ABILITY_QUERY_FAILED", err)
	}
	return &domainv1.GetGameAbilityResponse{HttpStatusCode: 200, Body: gameAbilityMessage(value)}, nil
}

// UpdateGameAbility 使用独立乐观版本更新特性资料。
func (service *KratosService) UpdateGameAbility(
	ctx context.Context,
	request *domainv1.UpdateGameAbilityRequest,
) (*domainv1.UpdateGameAbilityResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	rules, err := abilityRulesFromMessage(request.GetBody().GetRules())
	if err != nil {
		return nil, err
	}
	abilityID, err := gameDataIdentifier(request.GetAbilityId(), "INVALID_ABILITY_ID")
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
	updated, err := service.services.Abilities.Update(ctx, ability.UpdateCommand{
		GameDataWriteContext: writeContext, AbilityID: abilityID, ExpectedVersion: version,
		Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(),
		MainSeries: request.GetBody().GetMainSeries(), Effect: nullableText(request.GetBody().GetEffect()),
		ShortEffect: nullableText(request.GetBody().GetShortEffect()), Introduction: nullableText(request.GetBody().GetIntroduction()),
		Rules: rules, Enabled: request.GetBody().GetEnabled(),
	})
	if err != nil {
		return nil, service.abilityError(ctx, "GAME_ABILITY_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateGameAbilityResponse{HttpStatusCode: 200, Body: gameAbilityMessage(updated)}, nil
}

// DeleteGameAbility 使用独立乐观版本禁用未被引用的特性资料。
func gameDataPage(page, pageSize int32) (int32, int32) {
	if page == 0 {
		page = 1
	}
	if pageSize == 0 {
		pageSize = 20
	}
	return page, pageSize
}

func gameAbilityMessage(value ability.Ability) *domainv1.GameAbility {
	message := &domainv1.GameAbility{
		Id: value.ID.String(), Code: value.Code, Name: value.Name, MainSeries: value.MainSeries,
		Rules: abilityRulesMessage(value.Rules), Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10),
	}
	if value.Effect != nil {
		message.Effect = *value.Effect
	}
	if value.ShortEffect != nil {
		message.ShortEffect = *value.ShortEffect
	}
	if value.Introduction != nil {
		message.Introduction = *value.Introduction
	}
	return message
}

func (service *KratosService) abilityError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, ability.ErrInvalidAbility):
		return kratoserrors.BadRequest("INVALID_GAME_ABILITY", "特性资料字段无效")
	case errors.Is(err, ability.ErrAbilityNotFound), errors.Is(err, ability.ErrAbilityNotFound):
		return kratoserrors.NotFound("GAME_ABILITY_NOT_FOUND", "特性资料或维护窗口不存在")
	case errors.Is(err, ability.ErrAbilityVersionConflict), errors.Is(err, ability.ErrAbilityCodeConflict),
		errors.Is(err, ability.ErrAbilityReferenced),
		errors.Is(err, ability.ErrAbilityVersionConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("GAME_ABILITY_CONFLICT", "特性资料状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "特性资料 Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
