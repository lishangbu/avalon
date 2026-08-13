package api

import (
	"context"
	"errors"
	"log/slog"
	"strconv"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/platform/httpapi"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/playercharacter"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// KratosService 直接实现生成的 PlayerCharacterService HTTP 契约。
//
// 服务只依赖 PlayerCharacter 应用用例，不持有进程内 HTTP Handler，
// 因而请求绑定、鉴权中间件和错误编码都由 Kratos 公开传输边界统一处理。
type KratosService struct {
	// lifecycle 创建、更新、归档和恢复账号拥有的 PlayerCharacter。
	lifecycle LifecycleService
	// query 提供账号范围内的 PlayerCharacter 查询。
	query QueryService
	// active 管理账号唯一的活动 PlayerCharacter 持久绑定。
	active ActiveService
	// presence 管理活动 PlayerCharacter 的临时在线信号。
	presence PresenceService
	// logger 记录无法安全映射到公开错误的内部失败。
	logger *slog.Logger
}

// NewKratosService 使用显式应用用例创建原生 PlayerCharacter Kratos 服务。
func NewKratosService(
	lifecycle LifecycleService,
	query QueryService,
	active ActiveService,
	presence PresenceService,
	logger *slog.Logger,
) *KratosService {
	if logger == nil {
		logger = slog.Default()
	}
	return &KratosService{lifecycle: lifecycle, query: query, active: active, presence: presence, logger: logger}
}

// ListOwnedPlayerCharacters 查询当前认证账号拥有的角色。
func (service *KratosService) ListOwnedPlayerCharacters(
	ctx context.Context,
	request *domainv1.ListOwnedPlayerCharactersRequest,
) (*domainv1.ListOwnedPlayerCharactersResponse, error) {
	principal, err := playerPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	values, queryErr := service.query.ListOwned(ctx, principal.AccountID, request.GetIncludeArchived())
	if queryErr != nil {
		return nil, service.playerError(ctx, "PLAYER_CHARACTER_LIST_FAILED", queryErr)
	}
	items := make([]*domainv1.PlayerCharacter, len(values))
	for index := range values {
		items[index] = playerCharacterMessage(values[index])
	}
	return &domainv1.ListOwnedPlayerCharactersResponse{HttpStatusCode: 200, Body: items}, nil
}

// CreatePlayerCharacter 创建当前认证账号拥有的角色。
func (service *KratosService) CreatePlayerCharacter(
	ctx context.Context,
	request *domainv1.CreatePlayerCharacterRequest,
) (*domainv1.CreatePlayerCharacterResponse, error) {
	principal, err := playerPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	created, createErr := service.lifecycle.Create(ctx, playercharacter.CreateCommand{
		AccountID: principal.AccountID, DisplayName: request.GetBody().GetDisplayName(),
		IdempotencyKey: request.GetHeaderIdempotencyKey(), RequestID: httpapi.RequestIDFromContext(ctx),
	})
	if createErr != nil {
		return nil, service.playerError(ctx, "PLAYER_CHARACTER_CREATE_FAILED", createErr)
	}
	return &domainv1.CreatePlayerCharacterResponse{HttpStatusCode: 201, Body: playerCharacterMessage(created)}, nil
}

// GetOwnedPlayerCharacter 查询当前认证账号拥有的指定角色。
func (service *KratosService) GetOwnedPlayerCharacter(
	ctx context.Context,
	request *domainv1.GetOwnedPlayerCharacterRequest,
) (*domainv1.GetOwnedPlayerCharacterResponse, error) {
	principal, err := playerPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	characterID, err := parsePlayerIdentifier(request.GetPlayerCharacterId(), "INVALID_PLAYER_CHARACTER_ID")
	if err != nil {
		return nil, err
	}
	value, queryErr := service.query.GetOwned(ctx, principal.AccountID, characterID)
	if queryErr != nil {
		return nil, service.playerError(ctx, "PLAYER_CHARACTER_QUERY_FAILED", queryErr)
	}
	return &domainv1.GetOwnedPlayerCharacterResponse{HttpStatusCode: 200, Body: playerCharacterMessage(value)}, nil
}

// RenamePlayerCharacter 修改角色的全局唯一展示名称。
func (service *KratosService) RenamePlayerCharacter(
	ctx context.Context,
	request *domainv1.RenamePlayerCharacterRequest,
) (*domainv1.RenamePlayerCharacterResponse, error) {
	principal, err := playerPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	characterID, version, err := playerMutationInput(request.GetPlayerCharacterId(), request.GetBody().GetExpectedVersion())
	if request.GetBody() == nil || err != nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求字段无效")
	}
	value, mutationErr := service.lifecycle.Rename(ctx, playercharacter.RenameCommand{
		AccountID: principal.AccountID, PlayerCharacterID: characterID, ExpectedVersion: version,
		DisplayName: request.GetBody().GetDisplayName(), IdempotencyKey: request.GetHeaderIdempotencyKey(),
		RequestID: httpapi.RequestIDFromContext(ctx),
	})
	if mutationErr != nil {
		return nil, service.playerError(ctx, "PLAYER_CHARACTER_RENAME_FAILED", mutationErr)
	}
	return &domainv1.RenamePlayerCharacterResponse{HttpStatusCode: 200, Body: playerCharacterMessage(value)}, nil
}

// ArchivePlayerCharacter 归档当前认证账号拥有的角色。
func (service *KratosService) ArchivePlayerCharacter(
	ctx context.Context,
	request *domainv1.ArchivePlayerCharacterRequest,
) (*domainv1.ArchivePlayerCharacterResponse, error) {
	principal, err := playerPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	characterID, version, err := playerMutationInput(request.GetPlayerCharacterId(), request.GetBody().GetExpectedVersion())
	if request.GetBody() == nil || err != nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求字段无效")
	}
	value, mutationErr := service.lifecycle.Archive(ctx, playercharacter.ArchiveCommand{
		AccountID: principal.AccountID, PlayerCharacterID: characterID, ExpectedVersion: version,
		IdempotencyKey: request.GetHeaderIdempotencyKey(), RequestID: httpapi.RequestIDFromContext(ctx),
	})
	if mutationErr != nil {
		return nil, service.playerError(ctx, "PLAYER_CHARACTER_ARCHIVE_FAILED", mutationErr)
	}
	return &domainv1.ArchivePlayerCharacterResponse{HttpStatusCode: 200, Body: playerCharacterMessage(value)}, nil
}

// RestorePlayerCharacter 恢复仍满足账号上限的已归档角色。
func (service *KratosService) RestorePlayerCharacter(
	ctx context.Context,
	request *domainv1.RestorePlayerCharacterRequest,
) (*domainv1.RestorePlayerCharacterResponse, error) {
	principal, err := playerPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	characterID, version, err := playerMutationInput(request.GetPlayerCharacterId(), request.GetBody().GetExpectedVersion())
	if request.GetBody() == nil || err != nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求字段无效")
	}
	value, mutationErr := service.lifecycle.Restore(ctx, playercharacter.RestoreCommand{
		AccountID: principal.AccountID, PlayerCharacterID: characterID, ExpectedVersion: version,
		IdempotencyKey: request.GetHeaderIdempotencyKey(), RequestID: httpapi.RequestIDFromContext(ctx),
	})
	if mutationErr != nil {
		return nil, service.playerError(ctx, "PLAYER_CHARACTER_RESTORE_FAILED", mutationErr)
	}
	return &domainv1.RestorePlayerCharacterResponse{HttpStatusCode: 200, Body: playerCharacterMessage(value)}, nil
}

// GetActivePlayerCharacter 查询账号跨设备共享的活动角色绑定。
func (service *KratosService) GetActivePlayerCharacter(
	ctx context.Context,
	_ *domainv1.GetActivePlayerCharacterRequest,
) (*domainv1.GetActivePlayerCharacterResponse, error) {
	principal, err := playerPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	binding, queryErr := service.query.GetActive(ctx, principal.AccountID)
	if queryErr != nil {
		return nil, service.playerError(ctx, "ACTIVE_PLAYER_CHARACTER_QUERY_FAILED", queryErr)
	}
	return &domainv1.GetActivePlayerCharacterResponse{HttpStatusCode: 200, Body: activePlayerCharacterMessage(binding)}, nil
}

// SwitchActivePlayerCharacter 原子切换账号跨设备共享的活动角色绑定。
func (service *KratosService) SwitchActivePlayerCharacter(
	ctx context.Context,
	request *domainv1.SwitchActivePlayerCharacterRequest,
) (*domainv1.SwitchActivePlayerCharacterResponse, error) {
	principal, err := playerPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	characterID, parseErr := snowflake.Parse(request.GetBody().GetPlayerCharacterId())
	version, versionErr := strconv.ParseInt(request.GetBody().GetExpectedVersion(), 10, 64)
	if parseErr != nil || versionErr != nil || version < 0 {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求字段无效")
	}
	binding, switchErr := service.active.Switch(ctx, playercharacter.SwitchActiveCommand{
		AccountID: principal.AccountID, PlayerCharacterID: characterID, ExpectedVersion: version,
		IdempotencyKey: request.GetHeaderIdempotencyKey(), RequestID: httpapi.RequestIDFromContext(ctx),
	})
	if switchErr != nil {
		return nil, service.playerError(ctx, "ACTIVE_PLAYER_CHARACTER_SWITCH_FAILED", switchErr)
	}
	return &domainv1.SwitchActivePlayerCharacterResponse{HttpStatusCode: 200, Body: activePlayerCharacterMessage(binding)}, nil
}

// HeartbeatPlayerCharacterPresence 使用认证会话作为连接标识刷新临时在线信号。
func (service *KratosService) HeartbeatPlayerCharacterPresence(
	ctx context.Context,
	_ *domainv1.HeartbeatPlayerCharacterPresenceRequest,
) (*domainv1.HeartbeatPlayerCharacterPresenceResponse, error) {
	principal, err := playerPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if principal.SessionID == snowflake.ID(0) {
		return nil, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	binding, heartbeatErr := service.presence.Heartbeat(ctx, principal.AccountID, principal.SessionID)
	if heartbeatErr != nil {
		return nil, service.playerError(ctx, "PLAYER_CHARACTER_PRESENCE_FAILED", heartbeatErr)
	}
	return &domainv1.HeartbeatPlayerCharacterPresenceResponse{HttpStatusCode: 200, Body: activePlayerCharacterMessage(binding)}, nil
}

// FindPublicPlayerCharacter 按完整展示名称返回最小公开角色投影。
func (service *KratosService) FindPublicPlayerCharacter(
	ctx context.Context,
	request *domainv1.FindPublicPlayerCharacterRequest,
) (*domainv1.FindPublicPlayerCharacterResponse, error) {
	principal, err := playerPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	value, queryErr := service.query.FindPublicByDisplayName(ctx, principal.AccountID, request.GetDisplayName())
	if queryErr != nil {
		return nil, service.playerError(ctx, "PUBLIC_PLAYER_CHARACTER_QUERY_FAILED", queryErr)
	}
	return &domainv1.FindPublicPlayerCharacterResponse{HttpStatusCode: 200, Body: &domainv1.PublicPlayerCharacter{
		DisplayName: value.DisplayName, Online: value.Online, Challengeable: value.Challengeable,
	}}, nil
}

func playerPrincipal(ctx context.Context) (authentication.Principal, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok || principal.AccountID == snowflake.ID(0) {
		return authentication.Principal{}, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	return principal, nil
}

func parsePlayerIdentifier(raw, reason string) (snowflake.ID, error) {
	value, err := snowflake.Parse(raw)
	if err != nil || value == snowflake.ID(0) {
		return snowflake.ID(0), kratoserrors.BadRequest(reason, "标识格式无效")
	}
	return value, nil
}

func playerMutationInput(rawID, rawVersion string) (snowflake.ID, int64, error) {
	id, err := snowflake.Parse(rawID)
	if err != nil || id == snowflake.ID(0) {
		return snowflake.ID(0), 0, errors.New("角色标识无效")
	}
	version, err := strconv.ParseInt(rawVersion, 10, 64)
	if err != nil || version < 1 {
		return snowflake.ID(0), 0, errors.New("角色版本无效")
	}
	return id, version, nil
}

func playerCharacterMessage(value playercharacter.PlayerCharacter) *domainv1.PlayerCharacter {
	archivedAt := ""
	if value.ArchivedAt != nil {
		archivedAt = value.ArchivedAt.UTC().Format(time.RFC3339Nano)
	}
	return &domainv1.PlayerCharacter{
		Id: value.ID.String(), DisplayName: value.DisplayName, Version: strconv.FormatInt(value.Version, 10),
		ArchivedAt: archivedAt, CreatedAt: value.CreatedAt.UTC().Format(time.RFC3339Nano),
		UpdatedAt: value.UpdatedAt.UTC().Format(time.RFC3339Nano),
	}
}

func activePlayerCharacterMessage(value playercharacter.ActiveBinding) *domainv1.ActivePlayerCharacter {
	return &domainv1.ActivePlayerCharacter{
		PlayerCharacterId: value.PlayerCharacterID.String(), Version: strconv.FormatInt(value.Version, 10),
		UpdatedAt: value.UpdatedAt.UTC().Format(time.RFC3339Nano),
	}
}

func (service *KratosService) playerError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, playercharacter.ErrInvalidCommand):
		return kratoserrors.BadRequest("INVALID_REQUEST", "请求字段无效")
	case errors.Is(err, playercharacter.ErrPlayerCharacterNotFound):
		return kratoserrors.NotFound("PLAYER_CHARACTER_NOT_FOUND", "游戏角色不存在")
	case errors.Is(err, playercharacter.ErrActivePlayerCharacterRequired):
		return kratoserrors.Conflict("ACTIVE_PLAYER_CHARACTER_REQUIRED", "需要先选择活动角色")
	case errors.Is(err, playercharacter.ErrActiveLimitExceeded):
		return kratoserrors.Conflict("PLAYER_CHARACTER_LIMIT_EXCEEDED", "游戏角色数量已达上限")
	case errors.Is(err, playercharacter.ErrSensitiveDisplayName):
		return kratoserrors.New(422, "DISPLAY_NAME_REJECTED", "展示名称不可用")
	case errors.Is(err, playercharacter.ErrDisplayNameUnavailable):
		return kratoserrors.Conflict("DISPLAY_NAME_UNAVAILABLE", "展示名称已被占用")
	case errors.Is(err, playercharacter.ErrVersionConflict), errors.Is(err, playercharacter.ErrActiveBindingConflict):
		return kratoserrors.Conflict("PLAYER_CHARACTER_CONFLICT", "游戏角色版本或状态冲突")
	case errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("IDEMPOTENCY_CONFLICT", "幂等键已用于不同请求")
	default:
		service.logger.ErrorContext(ctx, "PlayerCharacter Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
