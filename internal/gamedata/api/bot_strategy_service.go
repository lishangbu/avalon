package api

import (
	"context"
	"encoding/json"
	"errors"
	"strconv"
	"time"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListGameBotStrategies 分页读取所有 Bot 的不可变版本资料，供管理员审计和发布前复核。
func (service *KratosService) ListGameBotStrategies(
	ctx context.Context,
	request *domainv1.ListGameBotStrategiesRequest,
) (*domainv1.ListGameBotStrategiesResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	result, err := service.services.BotStrategies.List(ctx, battle.BotStrategyListQuery{
		Page: page, PageSize: pageSize, Code: request.GetCode(), Enabled: request.Enabled,
	})
	if err != nil {
		return nil, service.botStrategyError(ctx, "GAME_BOT_STRATEGY_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameBotStrategy, len(result.Items))
	for index := range result.Items {
		items[index] = botStrategyMessage(result.Items[index])
	}
	return &domainv1.ListGameBotStrategiesResponse{HttpStatusCode: 200, Body: &domainv1.GameBotStrategyPage{
		Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize,
	}}, nil
}

// CreateGameBotStrategy 创建此前不存在稳定 Code 的第一个不可变 Bot 版本。
func (service *KratosService) CreateGameBotStrategy(
	ctx context.Context,
	request *domainv1.CreateGameBotStrategyRequest,
) (*domainv1.CreateGameBotStrategyResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	definition, err := botStrategyDefinition(body.GetDefinitionJson())
	if err != nil {
		return nil, err
	}
	created, err := service.services.BotStrategies.Create(ctx, battle.CreateBotStrategyCommand{
		GameDataWriteContext: writeContext, Code: body.GetCode(), Definition: definition,
	})
	if err != nil {
		return nil, service.botStrategyError(ctx, "GAME_BOT_STRATEGY_CREATE_FAILED", err)
	}
	return &domainv1.CreateGameBotStrategyResponse{HttpStatusCode: 201, Body: botStrategyMessage(created)}, nil
}

// GetGameBotStrategy 按稳定 Code 与版本读取一个不可变 Bot 资料快照。
func (service *KratosService) GetGameBotStrategy(
	ctx context.Context,
	request *domainv1.GetGameBotStrategyRequest,
) (*domainv1.GetGameBotStrategyResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	if request.GetVersion() < 1 {
		return nil, kratoserrors.BadRequest("INVALID_BOT_STRATEGY_VERSION", "Bot 策略版本无效")
	}
	value, err := service.services.BotStrategies.Get(ctx, request.GetCode(), uint32(request.GetVersion()))
	if err != nil {
		return nil, service.botStrategyError(ctx, "GAME_BOT_STRATEGY_QUERY_FAILED", err)
	}
	return &domainv1.GetGameBotStrategyResponse{HttpStatusCode: 200, Body: botStrategyMessage(value)}, nil
}

// PublishGameBotStrategy 为已有稳定 Code 发布下一条不可变版本并自动停用旧启用版本。
func (service *KratosService) PublishGameBotStrategy(
	ctx context.Context,
	request *domainv1.PublishGameBotStrategyRequest,
) (*domainv1.PublishGameBotStrategyResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	definition, err := botStrategyDefinition(body.GetDefinitionJson())
	if err != nil {
		return nil, err
	}
	published, err := service.services.BotStrategies.PublishNext(ctx, battle.PublishNextBotStrategyCommand{
		GameDataWriteContext: writeContext, Code: request.GetCode(), Definition: definition,
	})
	if err != nil {
		return nil, service.botStrategyError(ctx, "GAME_BOT_STRATEGY_PUBLISH_FAILED", err)
	}
	return &domainv1.PublishGameBotStrategyResponse{HttpStatusCode: 201, Body: botStrategyMessage(published)}, nil
}

// DisableGameBotStrategy 停用指定 Bot 版本；此前已经创建的 Training Battle 仍使用各自冻结的定义。
func (service *KratosService) DisableGameBotStrategy(
	ctx context.Context,
	request *domainv1.DisableGameBotStrategyRequest,
) (*domainv1.DisableGameBotStrategyResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil || request.GetVersion() < 1 {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	err = service.services.BotStrategies.Disable(ctx, battle.DisableBotStrategyCommand{
		GameDataWriteContext: writeContext, Code: request.GetCode(), Version: uint32(request.GetVersion()),
	})
	if err != nil {
		return nil, service.botStrategyError(ctx, "GAME_BOT_STRATEGY_DISABLE_FAILED", err)
	}
	return &domainv1.DisableGameBotStrategyResponse{HttpStatusCode: 204}, nil
}

// botStrategyDefinition 在 HTTP 边界拒绝不能作为 JSON 解析的载荷，详细 schema 校验由领域服务完成。
func botStrategyDefinition(raw string) (json.RawMessage, error) {
	definition := json.RawMessage(raw)
	if len(definition) == 0 || !json.Valid(definition) {
		return nil, kratoserrors.BadRequest("INVALID_BOT_STRATEGY_DEFINITION", "Bot 策略定义不是合法 JSON")
	}
	return definition, nil
}

// botStrategyMessage 将领域的不可变版本事实映射到对外管理契约。
func botStrategyMessage(value battle.ManagedBotStrategy) *domainv1.GameBotStrategy {
	return &domainv1.GameBotStrategy{
		Code: value.Code, Version: int32(value.Version), Enabled: value.Enabled,
		DefinitionJson: string(value.Definition), CreatedAt: value.CreatedAt.UTC().Format(time.RFC3339Nano),
	}
}

// botStrategyError 将 Bot 版本管理的领域错误稳定映射为外部 HTTP 语义。
func (service *KratosService) botStrategyError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, battle.ErrBotDefinitionInvalid):
		return kratoserrors.BadRequest("INVALID_BOT_STRATEGY", "Bot 策略字段或定义无效")
	case errors.Is(err, battle.ErrBotStrategyNotFound):
		return kratoserrors.NotFound("BOT_STRATEGY_NOT_FOUND", "Bot 策略版本不存在")
	case errors.Is(err, battle.ErrBotStrategyCodeConflict), errors.Is(err, battle.ErrBotStrategyVersionConflict),
		errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("BOT_STRATEGY_CONFLICT", "Bot 策略状态或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "Bot 策略 Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
