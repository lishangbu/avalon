package api

import (
	"context"
	"encoding/json"
	"errors"
	"strconv"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/effect"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// ListBattleClauses 分页读取实时整场条款。
func (service *KratosService) ListBattleClauses(ctx context.Context, request *domainv1.ListBattleClausesRequest) (*domainv1.ListBattleClausesResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	result, err := service.services.BattleRules.ListClauses(ctx, battleformat.ClauseListQuery{Page: page, PageSize: pageSize, Q: request.GetQ(), Enabled: request.Enabled})
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_CLAUSE_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameBattleClause, len(result.Items))
	for i, value := range result.Items {
		items[i] = battleClauseMessage(value)
	}
	return &domainv1.ListBattleClausesResponse{HttpStatusCode: 200, Body: &domainv1.GameBattleClausePage{Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize}}, nil
}

// CreateBattleClause 在维护窗口内创建整场条款。
func (service *KratosService) CreateBattleClause(ctx context.Context, request *domainv1.CreateBattleClauseRequest) (*domainv1.CreateBattleClauseResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil || body.GetDefinition() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	definition, err := clauseDefinition(body.GetDefinition())
	if err != nil {
		return nil, err
	}
	value, err := service.services.BattleRules.CreateClause(ctx, battleformat.CreateClauseCommand{GameDataWriteContext: writeContext, Code: body.GetCode(), Name: body.GetName(), Description: body.GetDescription(), Definition: definition, Enabled: body.GetEnabled()})
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_CLAUSE_CREATE_FAILED", err)
	}
	return &domainv1.CreateBattleClauseResponse{HttpStatusCode: 201, Body: battleClauseMessage(value)}, nil
}

// GetBattleClause 按稳定 Identifier 读取实时整场条款。
func (service *KratosService) GetBattleClause(ctx context.Context, request *domainv1.GetBattleClauseRequest) (*domainv1.GetBattleClauseResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	id, err := gameDataIdentifier(request.GetComponentId(), "INVALID_BATTLE_CLAUSE_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.BattleRules.GetClause(ctx, id)
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_CLAUSE_QUERY_FAILED", err)
	}
	return &domainv1.GetBattleClauseResponse{HttpStatusCode: 200, Body: battleClauseMessage(value)}, nil
}

// UpdateBattleClause 使用资源版本和全局修订双重乐观锁更新整场条款。
func (service *KratosService) UpdateBattleClause(ctx context.Context, request *domainv1.UpdateBattleClauseRequest) (*domainv1.UpdateBattleClauseResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil || body.GetDefinition() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	id, err := gameDataIdentifier(request.GetComponentId(), "INVALID_BATTLE_CLAUSE_ID")
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
	definition, err := clauseDefinition(body.GetDefinition())
	if err != nil {
		return nil, err
	}
	value, err := service.services.BattleRules.UpdateClause(ctx, battleformat.UpdateClauseCommand{CreateClauseCommand: battleformat.CreateClauseCommand{GameDataWriteContext: writeContext, Code: body.GetCode(), Name: body.GetName(), Description: body.GetDescription(), Definition: definition, Enabled: body.GetEnabled()}, ClauseID: id, ExpectedVersion: version})
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_CLAUSE_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateBattleClauseResponse{HttpStatusCode: 200, Body: battleClauseMessage(value)}, nil
}

// ListBattleRestrictions 分页读取实时资料限制。
func (service *KratosService) ListBattleRestrictions(ctx context.Context, request *domainv1.ListBattleRestrictionsRequest) (*domainv1.ListBattleRestrictionsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	result, err := service.services.BattleRules.ListRestrictions(ctx, battleformat.RestrictionListQuery{Page: page, PageSize: pageSize, Q: request.GetQ(), Enabled: request.Enabled})
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_RESTRICTION_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameBattleRestriction, len(result.Items))
	for i, value := range result.Items {
		items[i] = battleRestrictionMessage(value)
	}
	return &domainv1.ListBattleRestrictionsResponse{HttpStatusCode: 200, Body: &domainv1.GameBattleRestrictionPage{Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize}}, nil
}

// CreateBattleRestriction 在维护窗口内创建资料限制。
func (service *KratosService) CreateBattleRestriction(ctx context.Context, request *domainv1.CreateBattleRestrictionRequest) (*domainv1.CreateBattleRestrictionResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil || body.GetDefinition() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	definition, err := restrictionDefinition(body.GetDefinition())
	if err != nil {
		return nil, err
	}
	value, err := service.services.BattleRules.CreateRestriction(ctx, battleformat.CreateRestrictionCommand{GameDataWriteContext: writeContext, Code: body.GetCode(), Name: body.GetName(), Description: body.GetDescription(), Definition: definition, Enabled: body.GetEnabled()})
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_RESTRICTION_CREATE_FAILED", err)
	}
	return &domainv1.CreateBattleRestrictionResponse{HttpStatusCode: 201, Body: battleRestrictionMessage(value)}, nil
}

// GetBattleRestriction 按稳定 Identifier 读取实时资料限制。
func (service *KratosService) GetBattleRestriction(ctx context.Context, request *domainv1.GetBattleRestrictionRequest) (*domainv1.GetBattleRestrictionResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	id, err := gameDataIdentifier(request.GetComponentId(), "INVALID_BATTLE_RESTRICTION_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.BattleRules.GetRestriction(ctx, id)
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_RESTRICTION_QUERY_FAILED", err)
	}
	return &domainv1.GetBattleRestrictionResponse{HttpStatusCode: 200, Body: battleRestrictionMessage(value)}, nil
}

// UpdateBattleRestriction 使用双重乐观锁更新资料限制。
func (service *KratosService) UpdateBattleRestriction(ctx context.Context, request *domainv1.UpdateBattleRestrictionRequest) (*domainv1.UpdateBattleRestrictionResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil || body.GetDefinition() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	id, err := gameDataIdentifier(request.GetComponentId(), "INVALID_BATTLE_RESTRICTION_ID")
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
	definition, err := restrictionDefinition(body.GetDefinition())
	if err != nil {
		return nil, err
	}
	value, err := service.services.BattleRules.UpdateRestriction(ctx, battleformat.UpdateRestrictionCommand{CreateRestrictionCommand: battleformat.CreateRestrictionCommand{GameDataWriteContext: writeContext, Code: body.GetCode(), Name: body.GetName(), Description: body.GetDescription(), Definition: definition, Enabled: body.GetEnabled()}, RestrictionID: id, ExpectedVersion: version})
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_RESTRICTION_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateBattleRestrictionResponse{HttpStatusCode: 200, Body: battleRestrictionMessage(value)}, nil
}

// ListBattleMechanics 分页读取实时特殊机制。
func (service *KratosService) ListBattleMechanics(ctx context.Context, request *domainv1.ListBattleMechanicsRequest) (*domainv1.ListBattleMechanicsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	result, err := service.services.BattleRules.ListMechanics(ctx, battleformat.MechanicListQuery{Page: page, PageSize: pageSize, Q: request.GetQ(), Enabled: request.Enabled})
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_MECHANIC_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameBattleMechanic, len(result.Items))
	for i, value := range result.Items {
		items[i] = battleMechanicMessage(value)
	}
	return &domainv1.ListBattleMechanicsResponse{HttpStatusCode: 200, Body: &domainv1.GameBattleMechanicPage{Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize}}, nil
}

// CreateBattleMechanic 在维护窗口内创建特殊机制。
func (service *KratosService) CreateBattleMechanic(ctx context.Context, request *domainv1.CreateBattleMechanicRequest) (*domainv1.CreateBattleMechanicResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil || body.GetDefinition() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	writeContext, err := gameDataWriteContext(ctx, principal.AccountID, request.GetHeaderIdempotencyKey())
	if err != nil {
		return nil, err
	}
	definition, err := mechanicDefinition(body.GetDefinition())
	if err != nil {
		return nil, err
	}
	value, err := service.services.BattleRules.CreateMechanic(ctx, battleformat.CreateMechanicCommand{GameDataWriteContext: writeContext, Code: body.GetCode(), Name: body.GetName(), Description: body.GetDescription(), Definition: definition, Enabled: body.GetEnabled()})
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_MECHANIC_CREATE_FAILED", err)
	}
	return &domainv1.CreateBattleMechanicResponse{HttpStatusCode: 201, Body: battleMechanicMessage(value)}, nil
}

// GetBattleMechanic 按稳定 Identifier 读取实时特殊机制。
func (service *KratosService) GetBattleMechanic(ctx context.Context, request *domainv1.GetBattleMechanicRequest) (*domainv1.GetBattleMechanicResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	id, err := gameDataIdentifier(request.GetComponentId(), "INVALID_BATTLE_MECHANIC_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.BattleRules.GetMechanic(ctx, id)
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_MECHANIC_QUERY_FAILED", err)
	}
	return &domainv1.GetBattleMechanicResponse{HttpStatusCode: 200, Body: battleMechanicMessage(value)}, nil
}

// UpdateBattleMechanic 使用双重乐观锁更新特殊机制。
func (service *KratosService) UpdateBattleMechanic(ctx context.Context, request *domainv1.UpdateBattleMechanicRequest) (*domainv1.UpdateBattleMechanicResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil || body.GetDefinition() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	id, err := gameDataIdentifier(request.GetComponentId(), "INVALID_BATTLE_MECHANIC_ID")
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
	definition, err := mechanicDefinition(body.GetDefinition())
	if err != nil {
		return nil, err
	}
	value, err := service.services.BattleRules.UpdateMechanic(ctx, battleformat.UpdateMechanicCommand{CreateMechanicCommand: battleformat.CreateMechanicCommand{GameDataWriteContext: writeContext, Code: body.GetCode(), Name: body.GetName(), Description: body.GetDescription(), Definition: definition, Enabled: body.GetEnabled()}, MechanicID: id, ExpectedVersion: version})
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_MECHANIC_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateBattleMechanicResponse{HttpStatusCode: 200, Body: battleMechanicMessage(value)}, nil
}

// ListBattleFormats 分页读取实时赛制及其组成引用。
func (service *KratosService) ListBattleFormats(ctx context.Context, request *domainv1.ListBattleFormatsRequest) (*domainv1.ListBattleFormatsResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	page, pageSize := gameDataPage(request.GetPage(), request.GetPageSize())
	result, err := service.services.BattleRules.ListFormats(ctx, battleformat.FormatListQuery{Page: page, PageSize: pageSize, Q: request.GetQ(), Mode: battleformat.Mode(request.GetMode()), Enabled: request.Enabled, Challenge: request.Challenge, Training: request.Training, Encounter: request.Encounter, AdminPreview: request.AdminPreview})
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_FORMAT_LIST_FAILED", err)
	}
	items := make([]*domainv1.GameBattleFormat, len(result.Items))
	for i, value := range result.Items {
		items[i] = battleFormatMessage(value)
	}
	return &domainv1.ListBattleFormatsResponse{HttpStatusCode: 200, Body: &domainv1.GameBattleFormatPage{Items: items, Total: strconv.FormatInt(result.Total, 10), Page: result.Page, PageSize: result.PageSize}}, nil
}

// CreateBattleFormat 在维护窗口内创建完整赛制。
func (service *KratosService) CreateBattleFormat(ctx context.Context, request *domainv1.CreateBattleFormatRequest) (*domainv1.CreateBattleFormatResponse, error) {
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
	command, err := battleFormatCommand(body.GetCode(), body.GetName(), body.GetDescription(), body.GetMode(), body.GetRosterCount(), body.GetSelectCount(), body.GetActiveParticipantsPerSide(), body.GetLevelRule(), body.GetDeadlines(), body.GetAvailability(), body.GetClauseIds(), body.GetRestrictionIds(), body.GetMechanicIds(), body.GetDefault(), body.GetEnabled())
	if err != nil {
		return nil, err
	}
	command.GameDataWriteContext = writeContext
	value, err := service.services.BattleRules.CreateFormat(ctx, command)
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_FORMAT_CREATE_FAILED", err)
	}
	return &domainv1.CreateBattleFormatResponse{HttpStatusCode: 201, Body: battleFormatMessage(value)}, nil
}

// GetBattleFormat 按稳定 Identifier 读取实时赛制。
func (service *KratosService) GetBattleFormat(ctx context.Context, request *domainv1.GetBattleFormatRequest) (*domainv1.GetBattleFormatResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	id, err := gameDataIdentifier(request.GetFormatId(), "INVALID_BATTLE_FORMAT_ID")
	if err != nil {
		return nil, err
	}
	value, err := service.services.BattleRules.GetFormat(ctx, id)
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_FORMAT_QUERY_FAILED", err)
	}
	return &domainv1.GetBattleFormatResponse{HttpStatusCode: 200, Body: battleFormatMessage(value)}, nil
}

// UpdateBattleFormat 使用双重乐观锁完整替换实时赛制。
func (service *KratosService) UpdateBattleFormat(ctx context.Context, request *domainv1.UpdateBattleFormatRequest) (*domainv1.UpdateBattleFormatResponse, error) {
	principal, err := gameDataPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	if body == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	id, err := gameDataIdentifier(request.GetFormatId(), "INVALID_BATTLE_FORMAT_ID")
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
	command, err := battleFormatCommand(body.GetCode(), body.GetName(), body.GetDescription(), body.GetMode(), body.GetRosterCount(), body.GetSelectCount(), body.GetActiveParticipantsPerSide(), body.GetLevelRule(), body.GetDeadlines(), body.GetAvailability(), body.GetClauseIds(), body.GetRestrictionIds(), body.GetMechanicIds(), body.GetDefault(), body.GetEnabled())
	if err != nil {
		return nil, err
	}
	command.GameDataWriteContext = writeContext
	value, err := service.services.BattleRules.UpdateFormat(ctx, battleformat.UpdateFormatCommand{CreateFormatCommand: command, FormatID: id, ExpectedVersion: version})
	if err != nil {
		return nil, service.battleError(ctx, "GAME_BATTLE_FORMAT_UPDATE_FAILED", err)
	}
	return &domainv1.UpdateBattleFormatResponse{HttpStatusCode: 200, Body: battleFormatMessage(value)}, nil
}

func clauseDefinition(value *domainv1.BattleClauseEffectDefinition) (effect.Definition, error) {
	parameters, err := json.Marshal(struct{}{})
	if err != nil {
		return effect.Definition{}, err
	}
	return effect.Definition{Kind: value.GetKind(), SchemaVersion: value.GetSchemaVersion(), Parameters: parameters}, nil
}
func restrictionDefinition(value *domainv1.BattleRestrictionEffectDefinition) (effect.Definition, error) {
	if value.GetParameters() == nil {
		return effect.Definition{}, kratoserrors.BadRequest("INVALID_EFFECT_DEFINITION", "效果参数无效")
	}
	parameters, err := json.Marshal(effect.StableCodeListParameters{Mode: value.GetParameters().GetMode(), ResourceType: value.GetParameters().GetResourceType(), StableCodes: value.GetParameters().GetStableCodes()})
	if err != nil {
		return effect.Definition{}, err
	}
	return effect.Definition{Kind: value.GetKind(), SchemaVersion: value.GetSchemaVersion(), Parameters: parameters}, nil
}
func mechanicDefinition(value *domainv1.BattleMechanicEffectDefinition) (effect.Definition, error) {
	if value.GetParameters() == nil {
		return effect.Definition{}, kratoserrors.BadRequest("INVALID_EFFECT_DEFINITION", "效果参数无效")
	}
	var rawParameters any = effect.LevelNormalizationParameters{Level: value.GetParameters().GetLevel()}
	if value.GetKind() == effect.KindTerastallizationMechanic {
		if value.GetParameters().GetLevel() != 0 {
			return effect.Definition{}, kratoserrors.BadRequest("INVALID_EFFECT_DEFINITION", "太晶化机制不能设置等级参数")
		}
		rawParameters = struct{}{}
	}
	parameters, err := json.Marshal(rawParameters)
	if err != nil {
		return effect.Definition{}, err
	}
	return effect.Definition{Kind: value.GetKind(), SchemaVersion: value.GetSchemaVersion(), Parameters: parameters}, nil
}

func battleClauseMessage(value battleformat.Clause) *domainv1.GameBattleClause {
	return &domainv1.GameBattleClause{Id: value.ID.String(), Code: value.Code, Name: value.Name, Description: value.Description, Definition: &domainv1.BattleClauseEffectDefinition{Kind: value.Definition.Kind, SchemaVersion: value.Definition.SchemaVersion, Parameters: &domainv1.EmptyEffectParameters{}}, Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10)}
}
func battleRestrictionMessage(value battleformat.Restriction) *domainv1.GameBattleRestriction {
	parameters := effect.StableCodeListParameters{}
	_ = json.Unmarshal(value.Definition.Parameters, &parameters)
	return &domainv1.GameBattleRestriction{Id: value.ID.String(), Code: value.Code, Name: value.Name, Description: value.Description, Definition: &domainv1.BattleRestrictionEffectDefinition{Kind: value.Definition.Kind, SchemaVersion: value.Definition.SchemaVersion, Parameters: &domainv1.StableCodeListParameters{Mode: parameters.Mode, ResourceType: parameters.ResourceType, StableCodes: parameters.StableCodes}}, Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10)}
}
func battleMechanicMessage(value battleformat.Mechanic) *domainv1.GameBattleMechanic {
	parameters := effect.LevelNormalizationParameters{}
	if value.Definition.Kind == effect.KindLevelNormalizationMechanic {
		_ = json.Unmarshal(value.Definition.Parameters, &parameters)
	}
	return &domainv1.GameBattleMechanic{Id: value.ID.String(), Code: value.Code, Name: value.Name, Description: value.Description, Definition: &domainv1.BattleMechanicEffectDefinition{Kind: value.Definition.Kind, SchemaVersion: value.Definition.SchemaVersion, Parameters: &domainv1.LevelNormalizationParameters{Level: parameters.Level}}, Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10)}
}

func battleFormatCommand(code, name, description, mode string, rosterCount, selectCount, activeCount int32, levelRule *domainv1.BattleLevelRule, deadlines *domainv1.BattleDeadlines, availability *domainv1.BattleFormatAvailability, clauseIDs, restrictionIDs, mechanicIDs []string, isDefault, enabled bool) (battleformat.CreateFormatCommand, error) {
	if levelRule == nil || deadlines == nil || availability == nil {
		return battleformat.CreateFormatCommand{}, kratoserrors.BadRequest("INVALID_BATTLE_FORMAT", "赛制字段无效")
	}
	clauses, err := gameDataIdentifiers(clauseIDs, "INVALID_BATTLE_CLAUSE_ID")
	if err != nil {
		return battleformat.CreateFormatCommand{}, err
	}
	restrictions, err := gameDataIdentifiers(restrictionIDs, "INVALID_BATTLE_RESTRICTION_ID")
	if err != nil {
		return battleformat.CreateFormatCommand{}, err
	}
	mechanics, err := gameDataIdentifiers(mechanicIDs, "INVALID_BATTLE_MECHANIC_ID")
	if err != nil {
		return battleformat.CreateFormatCommand{}, err
	}
	var level *int32
	if levelRule.GetLevel() != 0 {
		value := levelRule.GetLevel()
		level = &value
	}
	return battleformat.CreateFormatCommand{Code: code, Name: name, Description: description, Mode: battleformat.Mode(mode), RosterCount: rosterCount, SelectCount: selectCount, ActiveParticipantsPerSide: activeCount, LevelRule: battleformat.LevelRule{Mode: battleformat.LevelRuleMode(levelRule.GetMode()), Level: level}, Deadlines: battleformat.Deadlines{PreviewSeconds: deadlines.GetPreviewSeconds(), TurnSeconds: deadlines.GetTurnSeconds(), BattleSeconds: deadlines.GetBattleSeconds()}, Availability: battleformat.Availability{Challenge: availability.GetChallenge(), Training: availability.GetTraining(), Encounter: availability.GetEncounter(), AdminPreview: availability.GetAdminPreview()}, ClauseIDs: clauses, RestrictionIDs: restrictions, MechanicIDs: mechanics, Default: isDefault, Enabled: enabled}, nil
}
func battleFormatMessage(value battleformat.Format) *domainv1.GameBattleFormat {
	level := int32(0)
	if value.LevelRule.Level != nil {
		level = *value.LevelRule.Level
	}
	return &domainv1.GameBattleFormat{Id: value.ID.String(), Code: value.Code, Name: value.Name, Description: value.Description, Mode: string(value.Mode), RosterCount: value.RosterCount, SelectCount: value.SelectCount, ActiveParticipantsPerSide: value.ActiveParticipantsPerSide, LevelRule: &domainv1.BattleLevelRule{Mode: string(value.LevelRule.Mode), Level: &level}, Deadlines: &domainv1.BattleDeadlines{PreviewSeconds: value.Deadlines.PreviewSeconds, TurnSeconds: value.Deadlines.TurnSeconds, BattleSeconds: value.Deadlines.BattleSeconds}, Availability: &domainv1.BattleFormatAvailability{Challenge: value.Availability.Challenge, Training: value.Availability.Training, Encounter: value.Availability.Encounter, AdminPreview: value.Availability.AdminPreview}, ClauseIds: identifierStrings(value.ClauseIDs), RestrictionIds: identifierStrings(value.RestrictionIDs), MechanicIds: identifierStrings(value.MechanicIDs), Default: value.Default, Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10)}
}

func (service *KratosService) battleError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, battleformat.ErrInvalidClause), errors.Is(err, battleformat.ErrInvalidRestriction), errors.Is(err, battleformat.ErrInvalidMechanic), errors.Is(err, battleformat.ErrInvalidFormat):
		return kratoserrors.BadRequest("INVALID_GAME_BATTLE_DATA", "对战资料字段或引用无效")
	case errors.Is(err, battleformat.ErrClauseNotFound), errors.Is(err, battleformat.ErrRestrictionNotFound), errors.Is(err, battleformat.ErrMechanicNotFound), errors.Is(err, battleformat.ErrFormatNotFound), errors.Is(err, battleformat.ErrFormatNotFound):
		return kratoserrors.NotFound("GAME_BATTLE_DATA_NOT_FOUND", "对战资料或维护窗口不存在")
	case errors.Is(err, battleformat.ErrClauseConflict), errors.Is(err, battleformat.ErrRestrictionConflict), errors.Is(err, battleformat.ErrMechanicConflict), errors.Is(err, battleformat.ErrFormatConflict), errors.Is(err, battleformat.ErrFormatConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("GAME_BATTLE_DATA_CONFLICT", "对战资料版本、引用或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "对战资料 Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
