// Package api 将 RPG 世界用例适配为生成的 Protobuf/gRPC 契约。
package api

import (
	"context"
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	rpgv1 "github.com/lishangbu/avalon/api/gen/go/avalon/rpg/v1"
	"github.com/lishangbu/avalon/internal/rpg"
	"github.com/lishangbu/avalon/internal/security/authentication"
	"google.golang.org/protobuf/types/known/timestamppb"
)

// WorldService 是玩家 RPC 所需的 RPG 世界用例集合。
type WorldService interface {
	GetMap(context.Context, snowflake.ID) (rpg.WorldMap, error)
	Traverse(context.Context, rpg.TraversalCommand) (rpg.TraversalResult, error)
	GetPendingEncounter(context.Context, snowflake.ID, time.Time) (*rpg.PendingEncounter, error)
	ResolvePendingEncounter(context.Context, rpg.ResolveEncounterCommand) (rpg.PendingEncounter, error)
	GetCheckpoint(context.Context, snowflake.ID) (*rpg.Checkpoint, error)
	SetCheckpoint(context.Context, rpg.SetCheckpointCommand) (rpg.Checkpoint, error)
	GetParty(context.Context, snowflake.ID) (rpg.Party, error)
	ReplaceParty(context.Context, rpg.ReplacePartyCommand) (rpg.Party, error)
}

// PlayerService 实现玩家 RpgWorldService 契约。
type PlayerService struct {
	world WorldService
	now   func() time.Time
}

// NewPlayerService 创建 RPG 玩家 RPC 适配器。
func NewPlayerService(world WorldService, now func() time.Time) *PlayerService {
	if now == nil {
		now = time.Now
	}
	return &PlayerService{world: world, now: now}
}

// GetMap 返回发现子图；游标首期由有界结果集保留为空。
func (service *PlayerService) GetMap(ctx context.Context, _ *rpgv1.GetMapRequest) (*rpgv1.GetMapResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	value, err := service.world.GetMap(ctx, accountID)
	if err != nil {
		return nil, publicError(err)
	}
	response := &rpgv1.GetMapResponse{Locations: make([]*rpgv1.MapLocation, 0, len(value.Locations)), Exits: make([]*rpgv1.MapExit, 0, len(value.Exits)), Position: positionMessage(value.Position)}
	for _, location := range value.Locations {
		parent := ""
		if location.ParentID != snowflake.ID(0) {
			parent = location.ParentID.String()
		}
		response.Locations = append(response.Locations, &rpgv1.MapLocation{Id: location.ID.String(), RegionId: location.RegionID.String(), ParentId: parent, Code: location.Code, Name: location.Name, LocationType: location.LocationType, MapX: location.X, MapY: location.Y, MapZ: location.Z})
	}
	for _, exit := range value.Exits {
		response.Exits = append(response.Exits, &rpgv1.MapExit{Id: exit.ID.String(), SourceLocationId: exit.SourceLocationID.String(), TargetLocationId: exit.TargetLocationID.String(), Code: exit.Code, Name: exit.Name, SortOrder: exit.SortOrder})
	}
	return response, nil
}

// Traverse 执行权威移动。
func (service *PlayerService) Traverse(ctx context.Context, request *rpgv1.TraverseRequest) (*rpgv1.TraverseResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	exitID, err := parseIdentifier(request.GetExitId())
	if err != nil {
		return nil, err
	}
	value, err := service.world.Traverse(ctx, rpg.TraversalCommand{AccountID: accountID, ExitID: exitID, ExpectedPositionVersion: request.GetExpectedPositionVersion(), IdempotencyKey: request.GetIdempotencyKey(), Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.TraverseResponse{Position: positionMessage(value.Position), PendingEncounter: pendingMessage(value.PendingEncounter)}, nil
}

// GetPendingEncounter 返回当前待处理遭遇。
func (service *PlayerService) GetPendingEncounter(ctx context.Context, _ *rpgv1.GetPendingEncounterRequest) (*rpgv1.GetPendingEncounterResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	value, err := service.world.GetPendingEncounter(ctx, accountID, service.now().UTC())
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.GetPendingEncounterResponse{PendingEncounter: pendingMessage(value)}, nil
}

// ResolvePendingEncounter 接受或取消待处理遭遇。
func (service *PlayerService) ResolvePendingEncounter(ctx context.Context, request *rpgv1.ResolvePendingEncounterRequest) (*rpgv1.ResolvePendingEncounterResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	encounterID, err := parseIdentifier(request.GetPendingEncounterId())
	if err != nil {
		return nil, err
	}
	resolution := rpg.EncounterResolution("")
	switch request.GetResolution() {
	case rpgv1.PendingEncounterResolution_PENDING_ENCOUNTER_RESOLUTION_ACCEPT:
		resolution = rpg.EncounterResolutionAccept
	case rpgv1.PendingEncounterResolution_PENDING_ENCOUNTER_RESOLUTION_CANCEL:
		resolution = rpg.EncounterResolutionCancel
	default:
		return nil, kratoserrors.BadRequest("INVALID_ENCOUNTER_RESOLUTION", "遭遇操作无效")
	}
	value, err := service.world.ResolvePendingEncounter(ctx, rpg.ResolveEncounterCommand{AccountID: accountID, PendingEncounterID: encounterID, Resolution: resolution, IdempotencyKey: request.GetIdempotencyKey(), Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.ResolvePendingEncounterResponse{PendingEncounter: pendingMessage(&value)}, nil
}

// GetCheckpoint 返回当前恢复点。
func (service *PlayerService) GetCheckpoint(ctx context.Context, _ *rpgv1.GetCheckpointRequest) (*rpgv1.GetCheckpointResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	value, err := service.world.GetCheckpoint(ctx, accountID)
	if err != nil {
		return nil, publicError(err)
	}
	response := &rpgv1.GetCheckpointResponse{}
	if value != nil {
		response.Checkpoint = checkpointMessage(*value)
	}
	return response, nil
}

// SetCheckpoint 更新当前恢复点。
func (service *PlayerService) SetCheckpoint(ctx context.Context, request *rpgv1.SetCheckpointRequest) (*rpgv1.SetCheckpointResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	checkpointID, err := parseIdentifier(request.GetCheckpointId())
	if err != nil {
		return nil, err
	}
	value, err := service.world.SetCheckpoint(ctx, rpg.SetCheckpointCommand{AccountID: accountID, CheckpointID: checkpointID, ExpectedVersion: request.GetExpectedVersion(), IdempotencyKey: request.GetIdempotencyKey(), Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.SetCheckpointResponse{Checkpoint: checkpointMessage(value), Version: value.Version}, nil
}

// GetParty 返回当前 RPG Party。
func (service *PlayerService) GetParty(ctx context.Context, _ *rpgv1.GetPartyRequest) (*rpgv1.GetPartyResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	value, err := service.world.GetParty(ctx, accountID)
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.GetPartyResponse{Party: partyMessage(value)}, nil
}

// ReplaceParty 替换 Party 成员。
func (service *PlayerService) ReplaceParty(ctx context.Context, request *rpgv1.ReplacePartyRequest) (*rpgv1.ReplacePartyResponse, error) {
	accountID, err := playerAccountID(ctx)
	if err != nil {
		return nil, err
	}
	members := make([]rpg.PartyMember, 0, len(request.GetMembers()))
	for _, member := range request.GetMembers() {
		creatureID, parseErr := parseIdentifier(member.GetPlayerCharacterCreatureId())
		if parseErr != nil {
			return nil, parseErr
		}
		members = append(members, rpg.PartyMember{Position: int16(member.GetPosition()), PlayerCharacterCreatureID: creatureID})
	}
	value, err := service.world.ReplaceParty(ctx, rpg.ReplacePartyCommand{AccountID: accountID, ExpectedVersion: request.GetExpectedVersion(), IdempotencyKey: request.GetIdempotencyKey(), Members: members, Now: service.now().UTC()})
	if err != nil {
		return nil, publicError(err)
	}
	return &rpgv1.ReplacePartyResponse{Party: partyMessage(value)}, nil
}

func playerAccountID(ctx context.Context) (snowflake.ID, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok || principal.AccountID == snowflake.ID(0) {
		return snowflake.ID(0), kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	return principal.AccountID, nil
}
func parseIdentifier(raw string) (snowflake.ID, error) {
	value, err := snowflake.Parse(raw)
	if err != nil || value == snowflake.ID(0) {
		return snowflake.ID(0), kratoserrors.BadRequest("INVALID_ID", "标识格式无效")
	}
	return value, nil
}
func positionMessage(value rpg.Position) *rpgv1.Position {
	return &rpgv1.Position{LocationId: value.LocationID.String(), MoveSequence: value.MoveSequence, Version: value.Version, UpdatedAt: timestamppb.New(value.UpdatedAt)}
}
func pendingMessage(value *rpg.PendingEncounter) *rpgv1.PendingEncounter {
	if value == nil {
		return nil
	}
	message := &rpgv1.PendingEncounter{Id: value.ID.String(), EncounterEntryId: value.EncounterEntryID.String(), State: value.State, ExpiresAt: timestamppb.New(value.ExpiresAt)}
	if value.BattleID != snowflake.ID(0) {
		battleID := value.BattleID.String()
		message.BattleId = &battleID
	}
	return message
}
func checkpointMessage(value rpg.Checkpoint) *rpgv1.Checkpoint {
	return &rpgv1.Checkpoint{Id: value.ID.String(), LocationId: value.LocationID.String(), Code: value.Code, Name: value.Name}
}
func partyMessage(value rpg.Party) *rpgv1.Party {
	message := &rpgv1.Party{Id: value.ID.String(), Version: value.Version, Members: make([]*rpgv1.PartyMember, 0, len(value.Members))}
	for _, member := range value.Members {
		message.Members = append(message.Members, &rpgv1.PartyMember{Position: int32(member.Position), PlayerCharacterCreatureId: member.PlayerCharacterCreatureID.String()})
	}
	return message
}
func publicError(err error) error {
	switch {
	case errors.Is(err, rpg.ErrActivePlayerCharacterMissing):
		return kratoserrors.New(412, "ACTIVE_PLAYER_CHARACTER_REQUIRED", "请先选择活动角色")
	case errors.Is(err, rpg.ErrPositionConflict):
		return kratoserrors.Conflict("POSITION_VERSION_CONFLICT", "角色位置已经变化，请刷新后重试")
	case errors.Is(err, rpg.ErrExitUnavailable):
		return kratoserrors.New(412, "EXIT_UNAVAILABLE", "出口当前不可用")
	case errors.Is(err, rpg.ErrExitConditionNotMet):
		return kratoserrors.New(412, "EXIT_CONDITION_NOT_MET", "尚未满足出口条件")
	case errors.Is(err, rpg.ErrPendingEncounterBlocksMovement):
		return kratoserrors.New(412, "PENDING_ENCOUNTER", "请先处理当前遭遇")
	case errors.Is(err, rpg.ErrIdempotencyConflict):
		return kratoserrors.Conflict("IDEMPOTENCY_CONFLICT", "幂等键已用于不同请求")
	default:
		return kratoserrors.InternalServer("RPG_WORLD_FAILED", "服务端无法完成请求")
	}
}
