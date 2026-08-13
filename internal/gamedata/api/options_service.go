package api

import (
	"context"

	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/element"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
)

const gameDataOptionPageSize int32 = 100

// ListGameElementOptions 返回管理表单可选择的全部已启用属性，不向调用方暴露分页协议。
func (service *KratosService) ListGameElementOptions(
	ctx context.Context,
	_ *domainv1.ListGameElementOptionsRequest,
) (*domainv1.ListGameElementOptionsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	values, err := collectAllEnabledOptions(ctx, func(ctx context.Context, page int32, enabled *bool) ([]element.Element, int64, error) {
		result, listErr := service.services.Elements.List(ctx, element.ListQuery{
			Page: page, PageSize: gameDataOptionPageSize, Enabled: enabled, Sort: element.SortCodeAscending,
		})
		return result.Items, result.Total, listErr
	})
	if err != nil {
		return nil, service.elementError(ctx, "GAME_ELEMENT_OPTIONS_FAILED", err)
	}
	items := make([]*domainv1.GameElementOption, len(values))
	for index, value := range values {
		items[index] = &domainv1.GameElementOption{Id: value.ID.String(), Code: value.Code, Name: value.Name}
	}
	return &domainv1.ListGameElementOptionsResponse{
		HttpStatusCode: 200,
		Body:           &domainv1.GameElementOptions{Items: items},
	}, nil
}

// ListGameStatOptions 返回管理表单可选择的全部已启用能力值，不向调用方暴露分页协议。
func (service *KratosService) ListGameStatOptions(
	ctx context.Context,
	_ *domainv1.ListGameStatOptionsRequest,
) (*domainv1.ListGameStatOptionsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	values, err := collectAllEnabledOptions(ctx, func(ctx context.Context, page int32, enabled *bool) ([]stat.Stat, int64, error) {
		result, listErr := service.services.Stats.List(ctx, stat.ListQuery{
			Page: page, PageSize: gameDataOptionPageSize, Enabled: enabled, Sort: stat.SortCodeAscending,
		})
		return result.Items, result.Total, listErr
	})
	if err != nil {
		return nil, service.statError(ctx, "GAME_STAT_OPTIONS_FAILED", err)
	}
	items := make([]*domainv1.GameStatOption, len(values))
	for index, value := range values {
		items[index] = &domainv1.GameStatOption{Id: value.ID.String(), Code: value.Code, Name: value.Name}
	}
	return &domainv1.ListGameStatOptionsResponse{
		HttpStatusCode: 200,
		Body:           &domainv1.GameStatOptions{Items: items},
	}, nil
}

// ListBattleClauseOptions 返回 BattleFormat 可选择的全部已启用整场条款。
func (service *KratosService) ListBattleClauseOptions(
	ctx context.Context,
	_ *domainv1.ListBattleClauseOptionsRequest,
) (*domainv1.ListBattleClauseOptionsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	values, err := collectAllEnabledOptions(ctx, func(ctx context.Context, page int32, enabled *bool) ([]battleformat.Clause, int64, error) {
		result, listErr := service.services.BattleRules.ListClauses(ctx, battleformat.ClauseListQuery{
			Page: page, PageSize: gameDataOptionPageSize, Enabled: enabled,
		})
		return result.Items, result.Total, listErr
	})
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_CLAUSE_OPTIONS_FAILED", err)
	}
	items := make([]*domainv1.GameBattleClauseOption, len(values))
	for index, value := range values {
		items[index] = &domainv1.GameBattleClauseOption{Id: value.ID.String(), Code: value.Code, Name: value.Name}
	}
	return &domainv1.ListBattleClauseOptionsResponse{
		HttpStatusCode: 200,
		Body:           &domainv1.GameBattleClauseOptions{Items: items},
	}, nil
}

// ListBattleRestrictionOptions 返回 BattleFormat 可选择的全部已启用资料限制。
func (service *KratosService) ListBattleRestrictionOptions(
	ctx context.Context,
	_ *domainv1.ListBattleRestrictionOptionsRequest,
) (*domainv1.ListBattleRestrictionOptionsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	values, err := collectAllEnabledOptions(ctx, func(ctx context.Context, page int32, enabled *bool) ([]battleformat.Restriction, int64, error) {
		result, listErr := service.services.BattleRules.ListRestrictions(ctx, battleformat.RestrictionListQuery{
			Page: page, PageSize: gameDataOptionPageSize, Enabled: enabled,
		})
		return result.Items, result.Total, listErr
	})
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_RESTRICTION_OPTIONS_FAILED", err)
	}
	items := make([]*domainv1.GameBattleRestrictionOption, len(values))
	for index, value := range values {
		items[index] = &domainv1.GameBattleRestrictionOption{Id: value.ID.String(), Code: value.Code, Name: value.Name}
	}
	return &domainv1.ListBattleRestrictionOptionsResponse{
		HttpStatusCode: 200,
		Body:           &domainv1.GameBattleRestrictionOptions{Items: items},
	}, nil
}

// ListBattleMechanicOptions 返回 BattleFormat 可选择的全部已启用特殊机制。
func (service *KratosService) ListBattleMechanicOptions(
	ctx context.Context,
	_ *domainv1.ListBattleMechanicOptionsRequest,
) (*domainv1.ListBattleMechanicOptionsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	values, err := collectAllEnabledOptions(ctx, func(ctx context.Context, page int32, enabled *bool) ([]battleformat.Mechanic, int64, error) {
		result, listErr := service.services.BattleRules.ListMechanics(ctx, battleformat.MechanicListQuery{
			Page: page, PageSize: gameDataOptionPageSize, Enabled: enabled,
		})
		return result.Items, result.Total, listErr
	})
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_MECHANIC_OPTIONS_FAILED", err)
	}
	items := make([]*domainv1.GameBattleMechanicOption, len(values))
	for index, value := range values {
		items[index] = &domainv1.GameBattleMechanicOption{Id: value.ID.String(), Code: value.Code, Name: value.Name}
	}
	return &domainv1.ListBattleMechanicOptionsResponse{
		HttpStatusCode: 200,
		Body:           &domainv1.GameBattleMechanicOptions{Items: items},
	}, nil
}

// collectAllEnabledOptions 使用内部固定小页遍历领域分页接口，对外返回不截断的完整已启用选项集合。
func collectAllEnabledOptions[T any](
	ctx context.Context,
	load func(context.Context, int32, *bool) ([]T, int64, error),
) ([]T, error) {
	enabled := true
	items := make([]T, 0)
	for page := int32(1); ; page++ {
		values, total, err := load(ctx, page, &enabled)
		if err != nil {
			return nil, err
		}
		items = append(items, values...)
		if int64(len(items)) >= total || len(values) == 0 {
			return items, nil
		}
	}
}
