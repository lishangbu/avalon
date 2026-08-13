package api

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	rpgv1 "github.com/lishangbu/avalon/api/gen/go/avalon/rpg/v1"
	"github.com/lishangbu/avalon/internal/platform/httpapi"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/rpg"
	"github.com/lishangbu/avalon/internal/security/authentication"
	"google.golang.org/protobuf/types/known/timestamppb"
)

// AdminWorldService 是管理端只读地图 RPC 适配器。
type AdminWorldService struct{ store rpg.AdminWorldStore }

// NewAdminWorldService 创建管理端只读地图服务。
func NewAdminWorldService(store rpg.AdminWorldStore) *AdminWorldService {
	return &AdminWorldService{store: store}
}

// ListRegions 分页返回完整 Region。
func (service *AdminWorldService) ListRegions(ctx context.Context, request *rpgv1.ListRegionsRequest) (*rpgv1.ListRegionsResponse, error) {
	rows, err := service.store.ListRegions(ctx, int(request.GetPageSize()))
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListRegionsResponse{Regions: make([]*rpgv1.Region, 0, len(rows))}
	for _, row := range rows {
		response.Regions = append(response.Regions, &rpgv1.Region{Id: row.ID.String(), Code: row.Code, Name: row.Name, Description: row.Description, Enabled: row.Enabled, Version: row.Version})
	}
	return response, nil
}

// CreateRegion 在维护窗口创建 Region。
func (service *AdminWorldService) CreateRegion(ctx context.Context, request *rpgv1.CreateRegionRequest) (*rpgv1.CreateRegionResponse, error) {
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	value, err := service.store.CreateRegion(ctx, rpg.SaveRegionCommand{Write: write, Region: rpg.AdminRegion{Code: request.GetCode(), Name: request.GetName(), Description: request.GetDescription(), Enabled: request.GetEnabled()}})
	if err != nil {
		return nil, adminError(err)
	}
	return &rpgv1.CreateRegionResponse{Region: regionMessage(value)}, nil
}

// UpdateRegion 使用乐观版本完整更新 Region。
func (service *AdminWorldService) UpdateRegion(ctx context.Context, request *rpgv1.UpdateRegionRequest) (*rpgv1.UpdateRegionResponse, error) {
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	id, err := snowflake.Parse(request.GetRegionId())
	if err != nil {
		return nil, kratoserrors.BadRequest("INVALID_REGION_ID", "Region 标识无效")
	}
	value, err := service.store.UpdateRegion(ctx, rpg.SaveRegionCommand{Write: write, Region: rpg.AdminRegion{ID: id, Code: request.GetCode(), Name: request.GetName(), Description: request.GetDescription(), Enabled: request.GetEnabled()}, ExpectedVersion: request.GetExpectedVersion()})
	if err != nil {
		return nil, adminError(err)
	}
	return &rpgv1.UpdateRegionResponse{Region: regionMessage(value)}, nil
}

func adminWriteContext(ctx context.Context, key string) (rpg.AdminWriteContext, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok || !principal.AccountID.IsValid() {
		return rpg.AdminWriteContext{}, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	return rpg.AdminWriteContext{ActorAccountID: principal.AccountID, IdempotencyKey: key, RequestID: httpapi.RequestIDFromContext(ctx)}, nil
}

func regionMessage(row rpg.AdminRegion) *rpgv1.Region {
	return &rpgv1.Region{Id: row.ID.String(), Code: row.Code, Name: row.Name, Description: row.Description, Enabled: row.Enabled, Version: row.Version}
}

// ListLocations 分页返回完整 Location。
func (service *AdminWorldService) ListLocations(ctx context.Context, request *rpgv1.ListLocationsRequest) (*rpgv1.ListLocationsResponse, error) {
	rows, err := service.store.ListLocations(ctx, int(request.GetPageSize()))
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListLocationsResponse{Locations: make([]*rpgv1.AdminLocation, 0, len(rows))}
	for _, row := range rows {
		response.Locations = append(response.Locations, locationMessage(row))
	}
	return response, nil
}

// CreateLocation 在维护窗口创建 Location。
func (service *AdminWorldService) CreateLocation(ctx context.Context, request *rpgv1.CreateLocationRequest) (*rpgv1.CreateLocationResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	location, err := locationFromBody(request.GetBody())
	if err != nil {
		return nil, err
	}
	value, err := service.store.CreateLocation(ctx, rpg.SaveLocationCommand{Write: write, Location: location})
	if err != nil {
		return nil, adminError(err)
	}
	return &rpgv1.CreateLocationResponse{Location: locationMessage(value)}, nil
}

// UpdateLocation 使用乐观版本完整更新 Location。
func (service *AdminWorldService) UpdateLocation(ctx context.Context, request *rpgv1.UpdateLocationRequest) (*rpgv1.UpdateLocationResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	location, err := locationFromBody(request.GetBody())
	if err != nil {
		return nil, err
	}
	location.ID, err = snowflake.Parse(request.GetLocationId())
	if err != nil {
		return nil, kratoserrors.BadRequest("INVALID_LOCATION_ID", "Location 标识无效")
	}
	value, err := service.store.UpdateLocation(ctx, rpg.SaveLocationCommand{Write: write, Location: location, ExpectedVersion: request.GetExpectedVersion()})
	if err != nil {
		return nil, adminError(err)
	}
	return &rpgv1.UpdateLocationResponse{Location: locationMessage(value)}, nil
}

func locationFromBody(body *rpgv1.SaveLocationBody) (rpg.AdminLocation, error) {
	regionID, err := snowflake.Parse(body.GetRegionId())
	if err != nil {
		return rpg.AdminLocation{}, kratoserrors.BadRequest("INVALID_REGION_ID", "Region 标识无效")
	}
	parentID := snowflake.ID(0)
	if body.GetParentId() != "" {
		parentID, err = snowflake.Parse(body.GetParentId())
		if err != nil {
			return rpg.AdminLocation{}, kratoserrors.BadRequest("INVALID_PARENT_LOCATION_ID", "父级 Location 标识无效")
		}
	}
	return rpg.AdminLocation{RegionID: regionID, ParentID: parentID, Code: body.GetCode(), Name: body.GetName(), LocationType: body.GetLocationType(), Description: body.GetDescription(), DefaultSpawn: body.GetDefaultSpawn(), Enabled: body.GetEnabled()}, nil
}
func locationMessage(row rpg.AdminLocation) *rpgv1.AdminLocation {
	parent := ""
	if row.ParentID.IsValid() {
		parent = row.ParentID.String()
	}
	return &rpgv1.AdminLocation{Id: row.ID.String(), RegionId: row.RegionID.String(), ParentId: parent, Code: row.Code, Name: row.Name, LocationType: row.LocationType, Description: row.Description, Enabled: row.Enabled, Version: row.Version, DefaultSpawn: row.DefaultSpawn}
}

// ListLocationExits 分页返回完整有向出口。
func (service *AdminWorldService) ListLocationExits(ctx context.Context, request *rpgv1.ListLocationExitsRequest) (*rpgv1.ListLocationExitsResponse, error) {
	rows, err := service.store.ListExits(ctx, int(request.GetPageSize()))
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListLocationExitsResponse{Exits: make([]*rpgv1.AdminLocationExit, 0, len(rows))}
	for _, row := range rows {
		condition, conditionErr := conditionMessage([]byte(row.ConditionJSON))
		if conditionErr != nil {
			return nil, adminError(conditionErr)
		}
		effect, effectErr := effectMessage([]byte(row.EffectJSON))
		if effectErr != nil {
			return nil, adminError(effectErr)
		}
		response.Exits = append(response.Exits, exitMessage(row, condition, effect))
	}
	return response, nil
}

// CreateLocationExit 在维护窗口创建有向出口。
func (service *AdminWorldService) CreateLocationExit(ctx context.Context, request *rpgv1.CreateLocationExitRequest) (*rpgv1.CreateLocationExitResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	exit, err := exitFromBody(request.GetBody())
	if err != nil {
		return nil, err
	}
	value, err := service.store.CreateExit(ctx, rpg.SaveExitCommand{Write: write, Exit: exit})
	if err != nil {
		return nil, adminError(err)
	}
	return &rpgv1.CreateLocationExitResponse{LocationExit: exitMessage(value, request.GetBody().GetCondition(), request.GetBody().GetEffect())}, nil
}

// UpdateLocationExit 使用乐观版本完整更新有向出口。
func (service *AdminWorldService) UpdateLocationExit(ctx context.Context, request *rpgv1.UpdateLocationExitRequest) (*rpgv1.UpdateLocationExitResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	exit, err := exitFromBody(request.GetBody())
	if err != nil {
		return nil, err
	}
	exit.ID, err = snowflake.Parse(request.GetLocationExitId())
	if err != nil {
		return nil, kratoserrors.BadRequest("INVALID_LOCATION_EXIT_ID", "Location Exit 标识无效")
	}
	value, err := service.store.UpdateExit(ctx, rpg.SaveExitCommand{Write: write, Exit: exit, ExpectedVersion: request.GetExpectedVersion()})
	if err != nil {
		return nil, adminError(err)
	}
	return &rpgv1.UpdateLocationExitResponse{LocationExit: exitMessage(value, request.GetBody().GetCondition(), request.GetBody().GetEffect())}, nil
}

func exitFromBody(body *rpgv1.SaveLocationExitBody) (rpg.AdminExit, error) {
	sourceID, err := snowflake.Parse(body.GetSourceLocationId())
	if err != nil {
		return rpg.AdminExit{}, kratoserrors.BadRequest("INVALID_SOURCE_LOCATION_ID", "来源 Location 标识无效")
	}
	targetID, err := snowflake.Parse(body.GetTargetLocationId())
	if err != nil {
		return rpg.AdminExit{}, kratoserrors.BadRequest("INVALID_TARGET_LOCATION_ID", "目标 Location 标识无效")
	}
	condition, err := conditionJSONFromMessage(body.GetCondition())
	if err != nil {
		return rpg.AdminExit{}, kratoserrors.BadRequest("INVALID_EXIT_CONDITION", "出口通行条件无效")
	}
	if _, err = rpg.CompileCondition(condition); err != nil {
		return rpg.AdminExit{}, kratoserrors.BadRequest("INVALID_EXIT_CONDITION", "出口通行条件无效")
	}
	effect, err := effectJSONFromMessage(body.GetEffect())
	if err != nil {
		return rpg.AdminExit{}, kratoserrors.BadRequest("INVALID_TRAVERSAL_EFFECT", "出口移动副作用无效")
	}
	if _, err = rpg.CompileEffect(effect); err != nil {
		return rpg.AdminExit{}, kratoserrors.BadRequest("INVALID_TRAVERSAL_EFFECT", "出口移动副作用无效")
	}
	return rpg.AdminExit{SourceLocationID: sourceID, TargetLocationID: targetID, Code: body.GetCode(), Name: body.GetName(), Description: body.GetDescription(), SortOrder: body.GetSortOrder(), ConditionJSON: string(condition), EffectJSON: string(effect), Enabled: body.GetEnabled()}, nil
}

func exitMessage(row rpg.AdminExit, condition *rpgv1.ExitCondition, effect *rpgv1.TraversalEffect) *rpgv1.AdminLocationExit {
	return &rpgv1.AdminLocationExit{Id: row.ID.String(), SourceLocationId: row.SourceLocationID.String(), TargetLocationId: row.TargetLocationID.String(), Code: row.Code, Name: row.Name, Description: row.Description, SortOrder: row.SortOrder, Condition: condition, Effect: effect, Enabled: row.Enabled, Version: row.Version}
}

// ListCheckpoints 返回完整恢复点规则资料。
func (service *AdminWorldService) ListCheckpoints(ctx context.Context, request *rpgv1.ListCheckpointsRequest) (*rpgv1.ListCheckpointsResponse, error) {
	rows, err := service.store.ListCheckpoints(ctx, int(request.GetPageSize()))
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListCheckpointsResponse{Checkpoints: make([]*rpgv1.AdminCheckpoint, 0, len(rows))}
	for _, row := range rows {
		value, convertErr := adminCheckpointMessage(row)
		if convertErr != nil {
			return nil, adminError(convertErr)
		}
		response.Checkpoints = append(response.Checkpoints, value)
	}
	return response, nil
}

// CreateCheckpoint 创建恢复点及其结构化条件。
func (service *AdminWorldService) CreateCheckpoint(ctx context.Context, request *rpgv1.CreateCheckpointRequest) (*rpgv1.CreateCheckpointResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	value, err := checkpointFromBody(request.GetBody())
	if err != nil {
		return nil, err
	}
	saved, err := service.store.CreateCheckpoint(ctx, rpg.SaveCheckpointCommand{Write: write, Checkpoint: value})
	if err != nil {
		return nil, adminError(err)
	}
	message, err := adminCheckpointMessage(saved)
	if err != nil {
		return nil, adminError(err)
	}
	return &rpgv1.CreateCheckpointResponse{Checkpoint: message}, nil
}

// UpdateCheckpoint 使用乐观版本完整更新恢复点。
func (service *AdminWorldService) UpdateCheckpoint(ctx context.Context, request *rpgv1.UpdateCheckpointRequest) (*rpgv1.UpdateCheckpointResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	value, err := checkpointFromBody(request.GetBody())
	if err != nil {
		return nil, err
	}
	value.ID, err = snowflake.Parse(request.GetCheckpointId())
	if err != nil {
		return nil, kratoserrors.BadRequest("INVALID_CHECKPOINT_ID", "Checkpoint 标识无效")
	}
	saved, err := service.store.UpdateCheckpoint(ctx, rpg.SaveCheckpointCommand{Write: write, Checkpoint: value, ExpectedVersion: request.GetExpectedVersion()})
	if err != nil {
		return nil, adminError(err)
	}
	message, err := adminCheckpointMessage(saved)
	if err != nil {
		return nil, adminError(err)
	}
	return &rpgv1.UpdateCheckpointResponse{Checkpoint: message}, nil
}

func checkpointFromBody(body *rpgv1.SaveCheckpointBody) (rpg.AdminCheckpoint, error) {
	locationID, err := snowflake.Parse(body.GetLocationId())
	if err != nil {
		return rpg.AdminCheckpoint{}, kratoserrors.BadRequest("INVALID_LOCATION_ID", "Location 标识无效")
	}
	setRaw, err := optionalConditionJSON(body.GetSetCondition())
	if err != nil {
		return rpg.AdminCheckpoint{}, kratoserrors.BadRequest("INVALID_CHECKPOINT_CONDITION", "Checkpoint 设置条件无效")
	}
	recoveryRaw, err := optionalConditionJSON(body.GetRecoveryCondition())
	if err != nil {
		return rpg.AdminCheckpoint{}, kratoserrors.BadRequest("INVALID_CHECKPOINT_CONDITION", "Checkpoint 恢复条件无效")
	}
	return rpg.AdminCheckpoint{LocationID: locationID, Code: body.GetCode(), Name: body.GetName(), Description: body.GetDescription(), SetConditionJSON: string(setRaw), RecoveryConditionJSON: string(recoveryRaw), Enabled: body.GetEnabled()}, nil
}
func optionalConditionJSON(value *rpgv1.ExitCondition) (json.RawMessage, error) {
	if value == nil || value.GetNode() == nil {
		return nil, nil
	}
	raw, err := conditionJSONFromMessage(value)
	if err != nil {
		return nil, err
	}
	if _, err = rpg.CompileCondition(raw); err != nil {
		return nil, err
	}
	return raw, nil
}
func adminCheckpointMessage(row rpg.AdminCheckpoint) (*rpgv1.AdminCheckpoint, error) {
	var setCondition, recoveryCondition *rpgv1.ExitCondition
	var err error
	if row.SetConditionJSON != "" {
		setCondition, err = conditionMessage([]byte(row.SetConditionJSON))
		if err != nil {
			return nil, err
		}
	}
	if row.RecoveryConditionJSON != "" {
		recoveryCondition, err = conditionMessage([]byte(row.RecoveryConditionJSON))
		if err != nil {
			return nil, err
		}
	}
	return &rpgv1.AdminCheckpoint{Id: row.ID.String(), LocationId: row.LocationID.String(), Code: row.Code, Name: row.Name, Description: row.Description, SetCondition: setCondition, RecoveryCondition: recoveryCondition, Enabled: row.Enabled, Version: row.Version}, nil
}

// ListEncounterTables 返回完整遭遇表及候选关系。
func (service *AdminWorldService) ListEncounterTables(ctx context.Context, request *rpgv1.ListEncounterTablesRequest) (*rpgv1.ListEncounterTablesResponse, error) {
	rows, err := service.store.ListEncounterTables(ctx, int(request.GetPageSize()))
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListEncounterTablesResponse{Tables: make([]*rpgv1.AdminEncounterTable, 0, len(rows))}
	for _, row := range rows {
		response.Tables = append(response.Tables, encounterMessage(row))
	}
	return response, nil
}

// CreateEncounterTable 创建遭遇表并原子保存候选关系。
func (service *AdminWorldService) CreateEncounterTable(ctx context.Context, request *rpgv1.CreateEncounterTableRequest) (*rpgv1.CreateEncounterTableResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	value, err := encounterFromBody(request.GetBody())
	if err != nil {
		return nil, err
	}
	saved, err := service.store.CreateEncounterTable(ctx, rpg.SaveEncounterTableCommand{Write: write, Table: value})
	if err != nil {
		return nil, adminError(err)
	}
	return &rpgv1.CreateEncounterTableResponse{Table: encounterMessage(saved)}, nil
}

// UpdateEncounterTable 使用乐观版本同步遭遇表及其当前候选关系。
func (service *AdminWorldService) UpdateEncounterTable(ctx context.Context, request *rpgv1.UpdateEncounterTableRequest) (*rpgv1.UpdateEncounterTableResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	value, err := encounterFromBody(request.GetBody())
	if err != nil {
		return nil, err
	}
	value.ID, err = snowflake.Parse(request.GetEncounterTableId())
	if err != nil {
		return nil, kratoserrors.BadRequest("INVALID_ENCOUNTER_TABLE_ID", "Encounter Table 标识无效")
	}
	saved, err := service.store.UpdateEncounterTable(ctx, rpg.SaveEncounterTableCommand{Write: write, Table: value, ExpectedVersion: request.GetExpectedVersion()})
	if err != nil {
		return nil, adminError(err)
	}
	return &rpgv1.UpdateEncounterTableResponse{Table: encounterMessage(saved)}, nil
}
func encounterFromBody(body *rpgv1.SaveEncounterTableBody) (rpg.AdminEncounterTable, error) {
	locationID, err := snowflake.Parse(body.GetLocationId())
	if err != nil {
		return rpg.AdminEncounterTable{}, kratoserrors.BadRequest("INVALID_LOCATION_ID", "Location 标识无效")
	}
	value := rpg.AdminEncounterTable{LocationID: locationID, Code: body.GetCode(), Name: body.GetName(), TriggerProbabilityBPS: body.GetTriggerProbabilityBps(), CooldownMoves: body.GetCooldownMoves(), MaximumUses: body.MaximumUses, Enabled: body.GetEnabled(), Entries: make([]rpg.AdminEncounterEntry, 0, len(body.GetEntries()))}
	for _, item := range body.GetEntries() {
		entryID, parseErr := optionalID(item.GetEncounterEntryId(), "INVALID_ENCOUNTER_ENTRY_ID", "Encounter Entry 标识无效")
		if parseErr != nil {
			return rpg.AdminEncounterTable{}, parseErr
		}
		creatureID, parseErr := snowflake.Parse(item.GetCreatureId())
		if parseErr != nil {
			return rpg.AdminEncounterTable{}, kratoserrors.BadRequest("INVALID_CREATURE_ID", "Creature 标识无效")
		}
		formID := snowflake.ID(0)
		if item.GetFormId() != "" {
			formID, parseErr = snowflake.Parse(item.GetFormId())
			if parseErr != nil {
				return rpg.AdminEncounterTable{}, kratoserrors.BadRequest("INVALID_FORM_ID", "Form 标识无效")
			}
		}
		lootTableID, parseErr := optionalID(item.GetLootTableId(), "INVALID_LOOT_TABLE_ID", "Loot Table 标识无效")
		if parseErr != nil {
			return rpg.AdminEncounterTable{}, parseErr
		}
		value.Entries = append(value.Entries, rpg.AdminEncounterEntry{ID: entryID, CreatureID: creatureID, FormID: formID, LootTableID: lootTableID, MinimumLevel: int16(item.GetMinimumLevel()), MaximumLevel: int16(item.GetMaximumLevel()), Weight: item.GetWeight(), Enabled: item.GetEnabled()})
	}
	return value, nil
}
func encounterMessage(row rpg.AdminEncounterTable) *rpgv1.AdminEncounterTable {
	value := &rpgv1.AdminEncounterTable{Id: row.ID.String(), LocationId: row.LocationID.String(), Code: row.Code, Name: row.Name, EncounterMethod: "walk", TriggerProbabilityBps: row.TriggerProbabilityBPS, CooldownMoves: row.CooldownMoves, MaximumUses: row.MaximumUses, Enabled: row.Enabled, Version: row.Version, Entries: make([]*rpgv1.AdminEncounterEntry, 0, len(row.Entries))}
	for _, item := range row.Entries {
		formID := ""
		if item.FormID.IsValid() {
			formID = item.FormID.String()
		}
		value.Entries = append(value.Entries, &rpgv1.AdminEncounterEntry{Id: item.ID.String(), CreatureId: item.CreatureID.String(), FormId: formID, LootTableId: idString(item.LootTableID), MinimumLevel: int32(item.MinimumLevel), MaximumLevel: int32(item.MaximumLevel), Weight: item.Weight, Enabled: item.Enabled})
	}
	return value
}

// ListMapProjections 返回地图展示投影聚合。
func (service *AdminWorldService) ListMapProjections(ctx context.Context, request *rpgv1.ListMapProjectionsRequest) (*rpgv1.ListMapProjectionsResponse, error) {
	rows, err := service.store.ListMapProjections(ctx, int(request.GetPageSize()))
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListMapProjectionsResponse{Projections: make([]*rpgv1.AdminMapProjection, 0, len(rows))}
	for _, row := range rows {
		response.Projections = append(response.Projections, projectionMessage(row))
	}
	return response, nil
}

// CreateMapProjection 创建地图展示投影及地点关系。
func (service *AdminWorldService) CreateMapProjection(ctx context.Context, request *rpgv1.CreateMapProjectionRequest) (*rpgv1.CreateMapProjectionResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	value, err := projectionFromBody(request.GetBody())
	if err != nil {
		return nil, err
	}
	saved, err := service.store.CreateMapProjection(ctx, rpg.SaveMapProjectionCommand{Write: write, Projection: value})
	if err != nil {
		return nil, adminError(err)
	}
	return &rpgv1.CreateMapProjectionResponse{Projection: projectionMessage(saved)}, nil
}

// UpdateMapProjection 使用布局版本完整替换地图展示投影。
func (service *AdminWorldService) UpdateMapProjection(ctx context.Context, request *rpgv1.UpdateMapProjectionRequest) (*rpgv1.UpdateMapProjectionResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	value, err := projectionFromBody(request.GetBody())
	if err != nil {
		return nil, err
	}
	value.ID, err = snowflake.Parse(request.GetProjectionId())
	if err != nil {
		return nil, kratoserrors.BadRequest("INVALID_MAP_PROJECTION_ID", "地图投影标识无效")
	}
	saved, err := service.store.UpdateMapProjection(ctx, rpg.SaveMapProjectionCommand{Write: write, Projection: value, ExpectedLayoutVersion: request.GetExpectedLayoutVersion()})
	if err != nil {
		return nil, adminError(err)
	}
	return &rpgv1.UpdateMapProjectionResponse{Projection: projectionMessage(saved)}, nil
}
func projectionFromBody(body *rpgv1.SaveMapProjectionBody) (rpg.AdminMapProjection, error) {
	value := rpg.AdminMapProjection{Code: body.GetCode(), Name: body.GetName(), Enabled: body.GetEnabled(), Locations: make([]rpg.AdminMapProjectionLocation, 0, len(body.GetLocations()))}
	for _, item := range body.GetLocations() {
		locationID, err := snowflake.Parse(item.GetLocationId())
		if err != nil {
			return rpg.AdminMapProjection{}, kratoserrors.BadRequest("INVALID_LOCATION_ID", "Location 标识无效")
		}
		iconID, backgroundID := snowflake.ID(0), snowflake.ID(0)
		if item.GetIconAssetId() != "" {
			iconID, err = snowflake.Parse(item.GetIconAssetId())
			if err != nil {
				return rpg.AdminMapProjection{}, kratoserrors.BadRequest("INVALID_ASSET_ID", "图标 Asset 标识无效")
			}
		}
		if item.GetBackgroundAssetId() != "" {
			backgroundID, err = snowflake.Parse(item.GetBackgroundAssetId())
			if err != nil {
				return rpg.AdminMapProjection{}, kratoserrors.BadRequest("INVALID_ASSET_ID", "背景 Asset 标识无效")
			}
		}
		value.Locations = append(value.Locations, rpg.AdminMapProjectionLocation{LocationID: locationID, IconAssetID: iconID, BackgroundAssetID: backgroundID, X: item.GetX(), Y: item.GetY(), Z: item.GetZ()})
	}
	return value, nil
}
func projectionMessage(row rpg.AdminMapProjection) *rpgv1.AdminMapProjection {
	value := &rpgv1.AdminMapProjection{Id: row.ID.String(), Code: row.Code, Name: row.Name, LayoutVersion: row.LayoutVersion, Enabled: row.Enabled, Locations: make([]*rpgv1.AdminMapProjectionLocation, 0, len(row.Locations))}
	for _, item := range row.Locations {
		iconID, backgroundID := "", ""
		if item.IconAssetID.IsValid() {
			iconID = item.IconAssetID.String()
		}
		if item.BackgroundAssetID.IsValid() {
			backgroundID = item.BackgroundAssetID.String()
		}
		value.Locations = append(value.Locations, &rpgv1.AdminMapProjectionLocation{Id: item.ID.String(), LocationId: item.LocationID.String(), X: item.X, Y: item.Y, Z: item.Z, IconAssetId: iconID, BackgroundAssetId: backgroundID})
	}
	return value
}

// ListIntegrityReports 分页返回拓扑校验报告。
func (service *AdminWorldService) ListIntegrityReports(ctx context.Context, request *rpgv1.ListIntegrityReportsRequest) (*rpgv1.ListIntegrityReportsResponse, error) {
	rows, err := service.store.ListIntegrityReports(ctx, int(request.GetPageSize()))
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListIntegrityReportsResponse{Reports: make([]*rpgv1.IntegrityReport, 0, len(rows))}
	for _, row := range rows {
		report := &rpgv1.IntegrityReport{Id: row.ID.String(), CheckedAt: timestamppb.New(row.CheckedAt), Passed: row.Passed, Issues: make([]*rpgv1.IntegrityIssue, 0, len(row.Issues))}
		for _, issue := range row.Issues {
			report.Issues = append(report.Issues, &rpgv1.IntegrityIssue{ReasonCode: issue.ReasonCode, ResourceId: issue.ResourceID, Message: issue.Message})
		}
		response.Reports = append(response.Reports, report)
	}
	return response, nil
}

func adminError(err error) error {
	switch {
	case errors.Is(err, rpg.ErrInvalidAdminWorld):
		return kratoserrors.BadRequest("INVALID_RPG_ADMIN_DATA", "RPG 管理资料字段无效")
	case errors.Is(err, rpg.ErrAdminWorldNotFound), errors.Is(err, rpg.ErrEquipmentNotFound):
		return kratoserrors.NotFound("RPG_ADMIN_DATA_NOT_FOUND", "RPG 管理资料不存在")
	case errors.Is(err, rpg.ErrEquipmentRulesInvalid), errors.Is(err, rpg.ErrEquipmentStatModifierInvalid):
		return kratoserrors.BadRequest("INVALID_EQUIPMENT_DATA", "装备规则或属性修正无效")
	case errors.Is(err, rpg.ErrInvalidEquipmentCursor):
		return kratoserrors.BadRequest("INVALID_EQUIPMENT_CURSOR", "装备列表游标无效")
	case errors.Is(err, rpg.ErrInvalidEquipmentFilter):
		return kratoserrors.BadRequest("INVALID_EQUIPMENT_FILTER", "装备列表筛选无效")
	case errors.Is(err, rpg.ErrAdminWorldConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("RPG_ADMIN_DATA_CONFLICT", "RPG 管理资料编码、版本或幂等请求冲突")
	default:
		return kratoserrors.InternalServer("RPG_ADMIN_WORLD_FAILED", "地图资料读取失败")
	}
}

type conditionJSON struct {
	Op       string            `json:"op"`
	Children []json.RawMessage `json:"children"`
	Child    json.RawMessage   `json:"child"`
	Key      string            `json:"key"`
	Value    int32             `json:"value"`
}

func conditionMessage(raw []byte) (*rpgv1.ExitCondition, error) {
	var value conditionJSON
	if err := json.Unmarshal(raw, &value); err != nil {
		return nil, err
	}
	message := &rpgv1.ExitCondition{}
	switch value.Op {
	case "all":
		node := &rpgv1.ConditionAll{}
		for _, child := range value.Children {
			item, err := conditionMessage(child)
			if err != nil {
				return nil, err
			}
			node.Children = append(node.Children, item)
		}
		message.Node = &rpgv1.ExitCondition_All{All: node}
	case "any":
		node := &rpgv1.ConditionAny{}
		for _, child := range value.Children {
			item, err := conditionMessage(child)
			if err != nil {
				return nil, err
			}
			node.Children = append(node.Children, item)
		}
		message.Node = &rpgv1.ExitCondition_Any{Any: node}
	case "not":
		child, err := conditionMessage(value.Child)
		if err != nil {
			return nil, err
		}
		message.Node = &rpgv1.ExitCondition_Not{Not: &rpgv1.ConditionNot{Child: child}}
	case "level_gte":
		message.Node = &rpgv1.ExitCondition_LevelAtLeast{LevelAtLeast: &rpgv1.LevelAtLeast{Value: value.Value}}
	case "item_count_gte":
		message.Node = &rpgv1.ExitCondition_ItemCountAtLeast{ItemCountAtLeast: &rpgv1.ItemCountAtLeast{ItemCode: value.Key, Value: value.Value}}
	case "quest_objective_gte":
		message.Node = &rpgv1.ExitCondition_QuestObjectiveAtLeast{QuestObjectiveAtLeast: &rpgv1.QuestObjectiveAtLeast{ObjectiveCode: value.Key, Value: value.Value}}
	case "profession":
		message.Node = &rpgv1.ExitCondition_HasProfession{HasProfession: &rpgv1.HasProfession{ProfessionCode: value.Key}}
	case "world_state":
		var boolean struct {
			Value bool `json:"value"`
		}
		if err := json.Unmarshal(raw, &boolean); err != nil {
			return nil, err
		}
		message.Node = &rpgv1.ExitCondition_WorldStateEquals{WorldStateEquals: &rpgv1.WorldStateEquals{Key: value.Key, Value: boolean.Value}}
	default:
		return nil, fmt.Errorf("未知 Exit Condition 操作 %q", value.Op)
	}
	return message, nil
}

func effectMessage(raw []byte) (*rpgv1.TraversalEffect, error) {
	if len(raw) == 0 || string(raw) == "null" {
		return nil, nil
	}
	var value struct {
		Op    string `json:"op"`
		Key   string `json:"key"`
		Value int32  `json:"value"`
	}
	if err := json.Unmarshal(raw, &value); err != nil {
		return nil, err
	}
	message := &rpgv1.TraversalEffect{}
	switch value.Op {
	case "set_world_state":
		var boolean struct {
			Value bool `json:"value"`
		}
		if err := json.Unmarshal(raw, &boolean); err != nil {
			return nil, err
		}
		message.Effect = &rpgv1.TraversalEffect_SetWorldState{SetWorldState: &rpgv1.SetWorldState{Key: value.Key, Value: boolean.Value}}
	case "increment_quest_objective":
		message.Effect = &rpgv1.TraversalEffect_IncrementQuestObjective{IncrementQuestObjective: &rpgv1.IncrementQuestObjective{ObjectiveCode: value.Key, Value: value.Value}}
	default:
		return nil, fmt.Errorf("未知 Traversal Effect 操作 %q", value.Op)
	}
	return message, nil
}

func conditionJSONFromMessage(message *rpgv1.ExitCondition) (json.RawMessage, error) {
	if message == nil {
		return nil, errors.New("Exit Condition 不能为空")
	}
	var value any
	switch node := message.GetNode().(type) {
	case *rpgv1.ExitCondition_All:
		children, err := conditionChildrenJSON(node.All.GetChildren())
		if err != nil {
			return nil, err
		}
		value = struct {
			Op       string            `json:"op"`
			Children []json.RawMessage `json:"children"`
		}{"all", children}
	case *rpgv1.ExitCondition_Any:
		children, err := conditionChildrenJSON(node.Any.GetChildren())
		if err != nil {
			return nil, err
		}
		value = struct {
			Op       string            `json:"op"`
			Children []json.RawMessage `json:"children"`
		}{"any", children}
	case *rpgv1.ExitCondition_Not:
		child, err := conditionJSONFromMessage(node.Not.GetChild())
		if err != nil {
			return nil, err
		}
		value = struct {
			Op    string          `json:"op"`
			Child json.RawMessage `json:"child"`
		}{"not", child}
	case *rpgv1.ExitCondition_LevelAtLeast:
		value = struct {
			Op    string `json:"op"`
			Value int32  `json:"value"`
		}{"level_gte", node.LevelAtLeast.GetValue()}
	case *rpgv1.ExitCondition_ItemCountAtLeast:
		value = struct {
			Op    string `json:"op"`
			Key   string `json:"key"`
			Value int32  `json:"value"`
		}{"item_count_gte", node.ItemCountAtLeast.GetItemCode(), node.ItemCountAtLeast.GetValue()}
	case *rpgv1.ExitCondition_QuestObjectiveAtLeast:
		value = struct {
			Op    string `json:"op"`
			Key   string `json:"key"`
			Value int32  `json:"value"`
		}{"quest_objective_gte", node.QuestObjectiveAtLeast.GetObjectiveCode(), node.QuestObjectiveAtLeast.GetValue()}
	case *rpgv1.ExitCondition_HasProfession:
		value = struct {
			Op  string `json:"op"`
			Key string `json:"key"`
		}{"profession", node.HasProfession.GetProfessionCode()}
	case *rpgv1.ExitCondition_WorldStateEquals:
		value = struct {
			Op    string `json:"op"`
			Key   string `json:"key"`
			Value bool   `json:"value"`
		}{"world_state", node.WorldStateEquals.GetKey(), node.WorldStateEquals.GetValue()}
	default:
		return nil, errors.New("Exit Condition 节点无效")
	}
	return json.Marshal(value)
}

func conditionChildrenJSON(children []*rpgv1.ExitCondition) ([]json.RawMessage, error) {
	result := make([]json.RawMessage, 0, len(children))
	for _, child := range children {
		raw, err := conditionJSONFromMessage(child)
		if err != nil {
			return nil, err
		}
		result = append(result, raw)
	}
	return result, nil
}

func effectJSONFromMessage(message *rpgv1.TraversalEffect) (json.RawMessage, error) {
	if message == nil || message.GetEffect() == nil {
		return nil, nil
	}
	var value any
	switch effect := message.GetEffect().(type) {
	case *rpgv1.TraversalEffect_SetWorldState:
		value = struct {
			Op    string `json:"op"`
			Key   string `json:"key"`
			Value bool   `json:"value"`
		}{"set_world_state", effect.SetWorldState.GetKey(), effect.SetWorldState.GetValue()}
	case *rpgv1.TraversalEffect_IncrementQuestObjective:
		value = struct {
			Op    string `json:"op"`
			Key   string `json:"key"`
			Value int32  `json:"value"`
		}{"increment_quest_objective", effect.IncrementQuestObjective.GetObjectiveCode(), effect.IncrementQuestObjective.GetValue()}
	default:
		return nil, errors.New("Traversal Effect 节点无效")
	}
	return json.Marshal(value)
}
