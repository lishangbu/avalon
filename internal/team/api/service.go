package api

import (
	"context"
	"encoding/json"
	"errors"
	"log/slog"
	"strconv"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/platform/httpapi"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/security/authentication"
	"github.com/lishangbu/avalon/internal/team"
)

// KratosService 直接实现生成的 TeamService HTTP 契约。
//
// 所有身份、路径、请求体和幂等信息都在此传输边界显式映射为 Team 应用命令。它不再构造其他传输适配层，
// 也不会通过进程内 HTTP 路由二次分派，只调用明确的领域用例。
type KratosService struct {
	// lifecycle 创建、更新和删除 PlayerCharacter 拥有的 Team。
	lifecycle LifecycleService
	// query 提供账号与 PlayerCharacter 范围内的 Team 查询。
	query QueryService
	// shares 创建、撤销和导入不可变 Team 分享快照。
	shares ShareService
	// logger 记录无法安全映射到公开错误的内部失败。
	logger *slog.Logger
}

// NewKratosService 使用 Team 生命周期、查询和分享用例创建原生 Kratos 服务。
func NewKratosService(
	lifecycle LifecycleService,
	query QueryService,
	shares ShareService,
	logger *slog.Logger,
) *KratosService {
	if logger == nil {
		logger = slog.Default()
	}
	return &KratosService{lifecycle: lifecycle, query: query, shares: shares, logger: logger}
}

// ListOwnedTeams 查询当前账号指定角色拥有的全部 Team。
func (service *KratosService) ListOwnedTeams(
	ctx context.Context,
	request *domainv1.ListOwnedTeamsRequest,
) (*domainv1.ListOwnedTeamsResponse, error) {
	principal, characterID, err := teamPrincipalAndCharacter(ctx, request.GetPlayerCharacterId())
	if err != nil {
		return nil, err
	}
	values, queryErr := service.query.ListOwned(ctx, principal.AccountID, characterID)
	if queryErr != nil {
		return nil, service.teamError(ctx, "TEAM_LIST_FAILED", queryErr)
	}
	items := make([]*domainv1.Team, len(values))
	for index := range values {
		items[index] = teamMessage(values[index])
	}
	return &domainv1.ListOwnedTeamsResponse{HttpStatusCode: 200, Body: items}, nil
}

// CreateTeam 创建引用当前实时资料的完整 Team。
func (service *KratosService) CreateTeam(
	ctx context.Context,
	request *domainv1.CreateTeamRequest,
) (*domainv1.CreateTeamResponse, error) {
	principal, characterID, err := teamPrincipalAndCharacter(ctx, request.GetPlayerCharacterId())
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	members, err := teamMemberInputs(request.GetBody().GetMembers())
	if err != nil {
		return nil, err
	}
	created, createErr := service.lifecycle.Create(ctx, team.CreateCommand{
		AccountID: principal.AccountID, PlayerCharacterID: characterID, Name: request.GetBody().GetName(), Members: members,
		IdempotencyKey: request.GetHeaderIdempotencyKey(), RequestID: httpapi.RequestIDFromContext(ctx),
	})
	if createErr != nil {
		return nil, service.teamError(ctx, "TEAM_CREATE_FAILED", createErr)
	}
	// Kratos 生成 HTTP 路由以成功响应主体直接编码，创建 Team 的稳定 HTTP 契约使用 200。
	return &domainv1.CreateTeamResponse{HttpStatusCode: 200, Body: teamMessage(created)}, nil
}

// GetOwnedTeam 查询当前账号角色拥有的指定 Team。
func (service *KratosService) GetOwnedTeam(
	ctx context.Context,
	request *domainv1.GetOwnedTeamRequest,
) (*domainv1.GetOwnedTeamResponse, error) {
	principal, characterID, teamID, err := teamPrincipalAndIDs(ctx, request.GetPlayerCharacterId(), request.GetTeamId())
	if err != nil {
		return nil, err
	}
	value, queryErr := service.query.GetOwned(ctx, principal.AccountID, characterID, teamID)
	if queryErr != nil {
		return nil, service.teamError(ctx, "TEAM_QUERY_FAILED", queryErr)
	}
	return &domainv1.GetOwnedTeamResponse{HttpStatusCode: 200, Body: teamMessage(value)}, nil
}

// UpdateTeam 使用乐观版本完整替换 Team 名称和阵容。
func (service *KratosService) UpdateTeam(
	ctx context.Context,
	request *domainv1.UpdateTeamRequest,
) (*domainv1.UpdateTeamResponse, error) {
	principal, characterID, teamID, err := teamPrincipalAndIDs(ctx, request.GetPlayerCharacterId(), request.GetTeamId())
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	version, err := positiveTeamVersion(request.GetBody().GetExpectedVersion())
	if err != nil {
		return nil, err
	}
	members, err := teamMemberInputs(request.GetBody().GetMembers())
	if err != nil {
		return nil, err
	}
	value, updateErr := service.lifecycle.Update(ctx, team.UpdateCommand{
		AccountID: principal.AccountID, PlayerCharacterID: characterID, TeamID: teamID, ExpectedVersion: version,
		Name: request.GetBody().GetName(), Members: members, IdempotencyKey: request.GetHeaderIdempotencyKey(),
		RequestID: httpapi.RequestIDFromContext(ctx),
	})
	if updateErr != nil {
		return nil, service.teamError(ctx, "TEAM_UPDATE_FAILED", updateErr)
	}
	return &domainv1.UpdateTeamResponse{HttpStatusCode: 200, Body: teamMessage(value)}, nil
}

// DeleteTeam 使用乐观版本永久删除可变 Team。
func (service *KratosService) DeleteTeam(
	ctx context.Context,
	request *domainv1.DeleteTeamRequest,
) (*domainv1.DeleteTeamResponse, error) {
	principal, characterID, teamID, err := teamPrincipalAndIDs(ctx, request.GetPlayerCharacterId(), request.GetTeamId())
	if err != nil {
		return nil, err
	}
	version, err := positiveTeamVersion(request.GetExpectedVersion())
	if err != nil {
		return nil, err
	}
	_, deleteErr := service.lifecycle.Delete(ctx, team.DeleteCommand{
		AccountID: principal.AccountID, PlayerCharacterID: characterID, TeamID: teamID, ExpectedVersion: version,
		IdempotencyKey: request.GetHeaderIdempotencyKey(), RequestID: httpapi.RequestIDFromContext(ctx),
	})
	if deleteErr != nil {
		return nil, service.teamError(ctx, "TEAM_DELETE_FAILED", deleteErr)
	}
	return &domainv1.DeleteTeamResponse{Body: &domainv1.TeamDeleted{Deleted: true}}, nil
}

// GetActiveTeam 查询角色当前默认 Team 绑定。
func (service *KratosService) GetActiveTeam(
	ctx context.Context,
	request *domainv1.GetActiveTeamRequest,
) (*domainv1.GetActiveTeamResponse, error) {
	principal, characterID, err := teamPrincipalAndCharacter(ctx, request.GetPlayerCharacterId())
	if err != nil {
		return nil, err
	}
	binding, queryErr := service.query.GetActive(ctx, principal.AccountID, characterID)
	if queryErr != nil {
		return nil, service.teamError(ctx, "ACTIVE_TEAM_QUERY_FAILED", queryErr)
	}
	return &domainv1.GetActiveTeamResponse{HttpStatusCode: 200, Body: activeTeamMessage(binding)}, nil
}

// SwitchActiveTeam 使用绑定版本切换角色默认 Team。
func (service *KratosService) SwitchActiveTeam(
	ctx context.Context,
	request *domainv1.SwitchActiveTeamRequest,
) (*domainv1.SwitchActiveTeamResponse, error) {
	principal, characterID, err := teamPrincipalAndCharacter(ctx, request.GetPlayerCharacterId())
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	teamID, err := parseTeamIdentifier(request.GetBody().GetTeamId(), "INVALID_TEAM_ID")
	if err != nil {
		return nil, err
	}
	version, err := positiveTeamVersion(request.GetBody().GetExpectedVersion())
	if err != nil {
		return nil, err
	}
	binding, switchErr := service.lifecycle.SwitchActive(ctx, team.SwitchActiveCommand{
		AccountID: principal.AccountID, PlayerCharacterID: characterID, TeamID: teamID, ExpectedVersion: version,
		IdempotencyKey: request.GetHeaderIdempotencyKey(), RequestID: httpapi.RequestIDFromContext(ctx),
	})
	if switchErr != nil {
		return nil, service.teamError(ctx, "ACTIVE_TEAM_SWITCH_FAILED", switchErr)
	}
	return &domainv1.SwitchActiveTeamResponse{HttpStatusCode: 200, Body: activeTeamMessage(binding)}, nil
}

// CreateTeamShare 冻结精确 Team 版本并创建限时分享。
func (service *KratosService) CreateTeamShare(
	ctx context.Context,
	request *domainv1.CreateTeamShareRequest,
) (*domainv1.CreateTeamShareResponse, error) {
	principal, characterID, teamID, err := teamPrincipalAndIDs(ctx, request.GetPlayerCharacterId(), request.GetTeamId())
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	version, err := positiveTeamVersion(request.GetBody().GetExpectedVersion())
	if err != nil {
		return nil, err
	}
	expiresAt, err := optionalTeamTime(request.GetBody().GetExpiresAt())
	if err != nil {
		return nil, err
	}
	result, createErr := service.shares.Create(ctx, team.CreateShareCommand{
		AccountID: principal.AccountID, PlayerCharacterID: characterID, TeamID: teamID,
		ExpectedVersion: version, ExpiresAt: expiresAt, IdempotencyKey: request.GetHeaderIdempotencyKey(),
		RequestID: httpapi.RequestIDFromContext(ctx),
	})
	if createErr != nil {
		return nil, service.teamError(ctx, "TEAM_SHARE_CREATE_FAILED", createErr)
	}
	// Kratos 生成 HTTP 路由以成功响应主体直接编码，创建分享的稳定 HTTP 契约使用 200。
	return &domainv1.CreateTeamShareResponse{HttpStatusCode: 200, Body: &domainv1.TeamShareCreated{
		Share: teamShareMessage(result.Share), Code: result.Code,
	}}, nil
}

// ResolveTeamShare 读取仍有效的不可变 Team 分享快照。
func (service *KratosService) ResolveTeamShare(
	ctx context.Context,
	request *domainv1.ResolveTeamShareRequest,
) (*domainv1.ResolveTeamShareResponse, error) {
	if _, err := teamPrincipal(ctx); err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	value, resolveErr := service.shares.Resolve(ctx, request.GetBody().GetCode())
	if resolveErr != nil {
		return nil, service.teamError(ctx, "TEAM_SHARE_RESOLVE_FAILED", resolveErr)
	}
	return &domainv1.ResolveTeamShareResponse{HttpStatusCode: 200, Body: teamShareSnapshotMessage(value)}, nil
}

// RevokeTeamShare 永久撤销角色拥有的 Team 分享。
func (service *KratosService) RevokeTeamShare(
	ctx context.Context,
	request *domainv1.RevokeTeamShareRequest,
) (*domainv1.RevokeTeamShareResponse, error) {
	principal, characterID, err := teamPrincipalAndCharacter(ctx, request.GetPlayerCharacterId())
	if err != nil {
		return nil, err
	}
	shareID, err := parseTeamIdentifier(request.GetShareId(), "INVALID_TEAM_SHARE_ID")
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	version, err := positiveTeamVersion(request.GetBody().GetExpectedVersion())
	if err != nil {
		return nil, err
	}
	value, revokeErr := service.shares.Revoke(ctx, team.RevokeShareCommand{
		AccountID: principal.AccountID, PlayerCharacterID: characterID, ShareID: shareID,
		ExpectedVersion: version, IdempotencyKey: request.GetHeaderIdempotencyKey(),
		RequestID: httpapi.RequestIDFromContext(ctx),
	})
	if revokeErr != nil {
		return nil, service.teamError(ctx, "TEAM_SHARE_REVOKE_FAILED", revokeErr)
	}
	return &domainv1.RevokeTeamShareResponse{HttpStatusCode: 200, Body: teamShareMessage(value)}, nil
}

// ImportTeamShare 把有效分享导入指定角色为独立 Team。
func (service *KratosService) ImportTeamShare(
	ctx context.Context,
	request *domainv1.ImportTeamShareRequest,
) (*domainv1.ImportTeamShareResponse, error) {
	principal, characterID, err := teamPrincipalAndCharacter(ctx, request.GetPlayerCharacterId())
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	value, importErr := service.shares.Import(ctx, team.ImportShareCommand{
		AccountID: principal.AccountID, PlayerCharacterID: characterID, Code: request.GetBody().GetCode(),
		Name: request.GetBody().GetName(), IdempotencyKey: request.GetHeaderIdempotencyKey(),
		RequestID: httpapi.RequestIDFromContext(ctx),
	})
	if importErr != nil {
		return nil, service.teamError(ctx, "TEAM_SHARE_IMPORT_FAILED", importErr)
	}
	// Kratos 生成 HTTP 路由与 Protobuf 均将导入分享的成功响应固定为 200；包装响应必须保持同一状态。
	return &domainv1.ImportTeamShareResponse{HttpStatusCode: 200, Body: teamMessage(value)}, nil
}

func teamPrincipal(ctx context.Context) (authentication.Principal, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok || principal.AccountID == snowflake.ID(0) {
		return authentication.Principal{}, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	return principal, nil
}

func teamPrincipalAndCharacter(ctx context.Context, rawCharacterID string) (authentication.Principal, snowflake.ID, error) {
	principal, err := teamPrincipal(ctx)
	if err != nil {
		return authentication.Principal{}, snowflake.ID(0), err
	}
	characterID, err := parseTeamIdentifier(rawCharacterID, "INVALID_PLAYER_CHARACTER_ID")
	return principal, characterID, err
}

func teamPrincipalAndIDs(
	ctx context.Context,
	rawCharacterID string,
	rawTeamID string,
) (authentication.Principal, snowflake.ID, snowflake.ID, error) {
	principal, characterID, err := teamPrincipalAndCharacter(ctx, rawCharacterID)
	if err != nil {
		return authentication.Principal{}, snowflake.ID(0), snowflake.ID(0), err
	}
	teamID, err := parseTeamIdentifier(rawTeamID, "INVALID_TEAM_ID")
	return principal, characterID, teamID, err
}

func parseTeamIdentifier(raw, reason string) (snowflake.ID, error) {
	value, err := snowflake.Parse(raw)
	if err != nil || value == snowflake.ID(0) {
		return snowflake.ID(0), kratoserrors.BadRequest(reason, "标识格式无效")
	}
	return value, nil
}

func positiveTeamVersion(raw string) (int64, error) {
	value, err := strconv.ParseInt(raw, 10, 64)
	if err != nil || value < 1 {
		return 0, kratoserrors.BadRequest("INVALID_VERSION", "版本格式无效")
	}
	return value, nil
}

func optionalTeamTime(raw string) (*time.Time, error) {
	if raw == "" {
		return nil, nil
	}
	value, err := time.Parse(time.RFC3339, raw)
	if err != nil {
		return nil, kratoserrors.BadRequest("INVALID_EXPIRES_AT", "到期时间格式无效")
	}
	value = value.UTC()
	return &value, nil
}

func teamMemberInputs(values []*domainv1.TeamMemberInput) ([]team.MemberInput, error) {
	result := make([]team.MemberInput, len(values))
	for index, value := range values {
		if value == nil {
			return nil, kratoserrors.BadRequest("INVALID_TEAM_MEMBER", "队伍成员格式无效")
		}
		creatureID, err := parseTeamIdentifier(value.GetCreatureId(), "INVALID_CREATURE_ID")
		if err != nil {
			return nil, err
		}
		abilityID, err := parseTeamIdentifier(value.GetAbilityId(), "INVALID_ABILITY_ID")
		if err != nil {
			return nil, err
		}
		teraID, err := parseTeamIdentifier(value.GetTeraElementId(), "INVALID_TERA_ELEMENT_ID")
		if err != nil {
			return nil, err
		}
		natureID, err := parseTeamIdentifier(value.GetNatureId(), "INVALID_NATURE_ID")
		if err != nil {
			return nil, err
		}
		skills := make([]snowflake.ID, len(value.GetSkillIds()))
		for skillIndex, raw := range value.GetSkillIds() {
			skills[skillIndex], err = parseTeamIdentifier(raw, "INVALID_SKILL_ID")
			if err != nil {
				return nil, err
			}
		}
		stats := make([]team.MemberStatInput, len(value.GetStats()))
		for statIndex, stat := range value.GetStats() {
			if stat == nil {
				return nil, kratoserrors.BadRequest("INVALID_TEAM_STAT", "培养值格式无效")
			}
			statID, statErr := parseTeamIdentifier(stat.GetStatId(), "INVALID_STAT_ID")
			if statErr != nil {
				return nil, statErr
			}
			stats[statIndex] = team.MemberStatInput{
				StatID: statID, IndividualValue: stat.GetIndividualValue(), EffortValue: stat.GetEffortValue(),
			}
		}
		formID, err := optionalTeamIdentifier(value.GetFormId(), "INVALID_FORM_ID")
		if err != nil {
			return nil, err
		}
		genderID, err := optionalTeamIdentifier(value.GetGenderId(), "INVALID_GENDER_ID")
		if err != nil {
			return nil, err
		}
		skinID, err := optionalTeamIdentifier(value.GetSkinId(), "INVALID_SKIN_ID")
		if err != nil {
			return nil, err
		}
		itemID, err := optionalTeamIdentifier(value.GetItemId(), "INVALID_ITEM_ID")
		if err != nil {
			return nil, err
		}
		result[index] = team.MemberInput{
			CreatureID: creatureID, FormID: formID, GenderID: genderID, SkinID: skinID, AbilityID: abilityID,
			ItemID: itemID, TeraElementID: teraID, NatureID: natureID, Level: value.GetLevel(), SkillIDs: skills, Stats: stats,
		}
	}
	return result, nil
}

func optionalTeamIdentifier(raw, reason string) (*snowflake.ID, error) {
	if raw == "" {
		return nil, nil
	}
	value, err := parseTeamIdentifier(raw, reason)
	if err != nil {
		return nil, err
	}
	return &value, nil
}

func teamMessage(value team.Team) *domainv1.Team {
	members := make([]*domainv1.TeamMember, len(value.Members))
	for index := range value.Members {
		members[index] = teamMemberMessage(value.Members[index])
	}
	return &domainv1.Team{
		Id: value.ID.String(), PlayerCharacterId: value.PlayerCharacterID.String(),
		Name: value.Name, Active: value.Active, Version: strconv.FormatInt(value.Version, 10), Members: members,
		CreatedAt: value.CreatedAt.UTC().Format(time.RFC3339Nano), UpdatedAt: value.UpdatedAt.UTC().Format(time.RFC3339Nano),
	}
}

func teamMemberMessage(value team.Member) *domainv1.TeamMember {
	skills := make([]*domainv1.TeamMemberSkill, len(value.Skills))
	for index, skill := range value.Skills {
		skills[index] = &domainv1.TeamMemberSkill{Position: skill.Position, SkillId: skill.SkillID.String()}
	}
	stats := make([]*domainv1.TeamMemberStat, len(value.Stats))
	for index, stat := range value.Stats {
		stats[index] = &domainv1.TeamMemberStat{
			StatId: stat.StatID.String(), IndividualValue: stat.IndividualValue, EffortValue: stat.EffortValue,
		}
	}
	return &domainv1.TeamMember{
		Level:    value.Level,
		Position: value.Position, CreatureId: value.CreatureID.String(), FormId: teamIdentifierString(value.FormID),
		GenderId: teamIdentifierString(value.GenderID), SkinId: teamIdentifierString(value.SkinID), AbilityId: value.AbilityID.String(),
		ItemId: teamIdentifierString(value.ItemID), TeraElementId: value.TeraElementID.String(), NatureId: value.NatureID.String(), Skills: skills, Stats: stats,
	}
}

func teamIdentifierString(value *snowflake.ID) string {
	if value == nil {
		return ""
	}
	return value.String()
}

func activeTeamMessage(value team.ActiveBinding) *domainv1.ActiveTeam {
	return &domainv1.ActiveTeam{
		PlayerCharacterId: value.PlayerCharacterID.String(), TeamId: value.TeamID.String(),
		Version: strconv.FormatInt(value.Version, 10), UpdatedAt: value.UpdatedAt.UTC().Format(time.RFC3339Nano),
	}
}

func teamShareMessage(value team.Share) *domainv1.TeamShare {
	revokedAt := ""
	if value.RevokedAt != nil {
		revokedAt = value.RevokedAt.UTC().Format(time.RFC3339Nano)
	}
	return &domainv1.TeamShare{
		Id: value.ID.String(), SourceTeamId: value.SourceTeamID.String(),
		OwnerPlayerCharacterId: value.OwnerPlayerCharacterID.String(),
		SourceTeamVersion:      strconv.FormatInt(value.SourceTeamVersion, 10), SchemaVersion: int32(value.SchemaVersion),
		Version: strconv.FormatInt(value.Version, 10), ExpiresAt: value.ExpiresAt.UTC().Format(time.RFC3339Nano),
		RevokedAt: revokedAt, CreatedAt: value.CreatedAt.UTC().Format(time.RFC3339Nano),
		UpdatedAt: value.UpdatedAt.UTC().Format(time.RFC3339Nano),
	}
}

func teamShareSnapshotMessage(value team.ShareSnapshot) *domainv1.TeamShareSnapshot {
	members := make([]*domainv1.TeamMember, len(value.Members))
	for index := range value.Members {
		members[index] = teamMemberMessage(value.Members[index])
	}
	return &domainv1.TeamShareSnapshot{
		SchemaVersion: int32(value.SchemaVersion), Name: value.Name, Members: members,
	}
}

func (service *KratosService) teamError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, team.ErrInvalidTeam):
		return kratoserrors.BadRequest("INVALID_TEAM", "Team 字段或资料引用无效")
	case errors.Is(err, team.ErrTeamReferenceInvalid):
		return service.teamReferenceInvalidError(ctx, err)
	case errors.Is(err, team.ErrTeamNotFound), errors.Is(err, team.ErrPlayerCharacterUnavailable):
		return kratoserrors.NotFound("TEAM_NOT_FOUND", "Team 或游戏角色不存在")
	case errors.Is(err, team.ErrTeamShareNotFound):
		return kratoserrors.NotFound("TEAM_SHARE_NOT_FOUND", "Team 分享不存在")
	case errors.Is(err, team.ErrTeamConflict), errors.Is(err, team.ErrTeamLimitExceeded),
		errors.Is(err, team.ErrTeamCatalogUnavailable), errors.Is(err, team.ErrTeamShareConflict),
		errors.Is(err, team.ErrTeamShareCodeCollision), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("TEAM_CONFLICT", "Team 状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "Team Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}

// teamReferenceInvalidError 将 Current Game Data 的结构化 Team 兼容问题映射为稳定的 RPC 错误。
//
// Kratos metadata 只能保存字符串，因此 issues 的值是一个 JSON 数组文本；客户端必须解码该字段，不能
// 依赖错误消息解析。WithCause 保留领域错误链，使服务内的调用方仍可通过 errors.Is 识别资料引用无效。
func (service *KratosService) teamReferenceInvalidError(ctx context.Context, err error) error {
	response := kratoserrors.BadRequest("INVALID_TEAM", "Team 字段或资料引用无效").WithCause(err)
	var compatibility *team.CompatibilityError
	if !errors.As(err, &compatibility) {
		return response
	}
	issues, marshalErr := json.Marshal(compatibility.Issues())
	if marshalErr != nil {
		service.logger.ErrorContext(ctx, "编码 Team 当前资料兼容问题失败", "error", marshalErr)
		return response
	}
	return response.WithMetadata(map[string]string{"issues": string(issues)})
}
