package api

import (
	"context"
	"errors"
	"strconv"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListGameStats 分页查询维护窗口中的数值项资料。
func (service *KratosService) ListGameStats(
	ctx context.Context,
	request *domainv1.ListGameStatsRequest,
) (*domainv1.ListGameStatsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	sortValue := request.GetSort()
	if sortValue == "" {
		sortValue = string(stat.SortCodeAscending)
	}
	result, err := service.services.Stats.List(ctx, stat.ListQuery{
		Page: page, PageSize: pageSize, Q: request.GetQ(), Code: request.GetCode(), Name: request.GetName(),
		SortOrder: optionalInt32(request.GetSortOrder()), BattleOnly: request.BattleOnly,
		Enabled: request.Enabled, Sort: stat.Sort(sortValue),
	})
	if err != nil {
		return nil, service.statError(ctx, "GAME_STAT_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameStat, len(result.Items))
	for index := range result.Items {
		items[index] = gameStatMessage(result.Items[index])
	}
	return &domainv1.ListGameStatsResponse{HttpStatusCode: 200, Body: &domainv1.GameStatPage{
		Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize,
	}}, nil
}

// CreateGameStat 在维护窗口中创建独立数值项。
func (service *KratosService) CreateGameStat(
	ctx context.Context,
	request *domainv1.CreateGameStatRequest,
) (*domainv1.CreateGameStatResponse, error) {
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
	created, err := service.services.Stats.Create(ctx, stat.CreateCommand{
		GameDataWriteContext: writeContext, Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(),
		SortOrder: request.GetBody().GetSortOrder(), BattleOnly: request.GetBody().GetBattleOnly(),
		Enabled: request.GetBody().GetEnabled(),
	})
	if err != nil {
		return nil, service.statError(ctx, "GAME_STAT_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameStatResponse{HttpStatusCode: 201, Body: gameStatMessage(created)}, nil
}

// GetGameStat 查询维护窗口中指定稳定身份的数值项。
func (service *KratosService) GetGameStat(
	ctx context.Context,
	request *domainv1.GetGameStatRequest,
) (*domainv1.GetGameStatResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	statID, err := gameDataIdentifier(request.GetStatId(), "INVALID_STAT_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.Stats.Get(ctx, statID)
	if err != nil {
		return nil, service.statError(ctx, "GAME_STAT_QUERY_FAILED", err)
	}
	return &domainv1.GetGameStatResponse{HttpStatusCode: 200, Body: gameStatMessage(value)}, nil
}

// UpdateGameStat 使用独立乐观版本完整更新数值项字段。
func (service *KratosService) UpdateGameStat(
	ctx context.Context,
	request *domainv1.UpdateGameStatRequest,
) (*domainv1.UpdateGameStatResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	statID, err := gameDataIdentifier(request.GetStatId(), "INVALID_STAT_ID")
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
	updated, err := service.services.Stats.Update(ctx, stat.UpdateCommand{
		GameDataWriteContext: writeContext, StatID: statID, ExpectedVersion: version,
		Code: request.GetBody().GetCode(), Name: request.GetBody().GetName(), SortOrder: request.GetBody().GetSortOrder(),
		BattleOnly: request.GetBody().GetBattleOnly(), Enabled: request.GetBody().GetEnabled(),
	})
	if err != nil {
		return nil, service.statError(ctx, "GAME_STAT_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateGameStatResponse{HttpStatusCode: 200, Body: gameStatMessage(updated)}, nil
}

// DeleteGameStat 使用独立乐观版本禁用未被引用的数值项。
func gameStatMessage(value stat.Stat) *domainv1.GameStat {
	return &domainv1.GameStat{
		Id: value.ID.String(), Code: value.Code, Name: value.Name, SortOrder: value.SortOrder,
		BattleOnly: value.BattleOnly, Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10),
	}
}

func (service *KratosService) statError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, stat.ErrInvalidStat):
		return kratoserrors.BadRequest("INVALID_GAME_STAT", "数值项字段无效")
	case errors.Is(err, stat.ErrStatNotFound), errors.Is(err, stat.ErrStatNotFound):
		return kratoserrors.NotFound("GAME_STAT_NOT_FOUND", "数值项或维护窗口不存在")
	case errors.Is(err, stat.ErrStatVersionConflict), errors.Is(err, stat.ErrStatCodeConflict),
		errors.Is(err, stat.ErrStatReferenced),
		errors.Is(err, stat.ErrStatVersionConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("GAME_STAT_CONFLICT", "数值项状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "数值项 Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
