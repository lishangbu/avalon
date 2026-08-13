package rpg

import (
	"context"
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

var (
	// ErrActivePlayerCharacterMissing 表示账号尚未选择活动角色。
	ErrActivePlayerCharacterMissing = errors.New("没有活动 PlayerCharacter")
	// ErrPositionConflict 表示客户端位置版本已经过期。
	ErrPositionConflict = errors.New("PlayerCharacter 位置版本冲突")
	// ErrExitUnavailable 表示出口不存在、停用或来源与当前位置不同。
	ErrExitUnavailable = errors.New("Location Exit 当前不可用")
	// ErrExitConditionNotMet 表示服务端出口条件未满足或无法安全求值。
	ErrExitConditionNotMet = errors.New("Location Exit 条件未满足")
	// ErrPendingEncounterBlocksMovement 表示尚有可处理遭遇阻止继续移动。
	ErrPendingEncounterBlocksMovement = errors.New("存在待处理 Encounter")
	// ErrIdempotencyConflict 表示相同幂等键绑定了不同请求。
	ErrIdempotencyConflict = errors.New("幂等键已绑定不同请求")
)

// WorldLocation 是玩家地图读取允许返回的地点安全字段。
type WorldLocation struct {
	ID, RegionID, ParentID   snowflake.ID
	Code, Name, LocationType string
	X, Y, Z                  int32
}

// WorldExit 是玩家地图读取允许返回的已发现有向出口。
type WorldExit struct {
	ID, SourceLocationID, TargetLocationID snowflake.ID
	Code, Name                             string
	SortOrder                              int32
}

// WorldMap 是按 Map Discovery 裁剪后的玩家子图。
type WorldMap struct {
	Locations []WorldLocation
	Exits     []WorldExit
	Position  Position
}

// Position 是当前位置的单调移动和乐观版本事实。
type Position struct {
	LocationID            snowflake.ID
	MoveSequence, Version int64
	UpdatedAt             time.Time
}

// PendingEncounter 是玩家可见的不含 seed 的待处理遭遇。
type PendingEncounter struct {
	ID, EncounterEntryID, BattleID snowflake.ID
	State                          string
	ExpiresAt                      time.Time
}

// EncounterResolution 是待处理遭遇允许的显式操作。
type EncounterResolution string

const (
	// EncounterResolutionAccept 接受遭遇并进入 PvE Battle 创建流程。
	EncounterResolutionAccept EncounterResolution = "accept"
	// EncounterResolutionCancel 取消遭遇并解除移动阻塞。
	EncounterResolutionCancel EncounterResolution = "cancel"
)

// ResolveEncounterCommand 是待处理遭遇幂等终态命令。
type ResolveEncounterCommand struct {
	AccountID, PendingEncounterID snowflake.ID
	Resolution                    EncounterResolution
	IdempotencyKey                string
	Now                           time.Time
}

// Checkpoint 是玩家当前选择的稳定恢复点。
type Checkpoint struct {
	ID, LocationID snowflake.ID
	Code, Name     string
	Version        int64
}

// SetCheckpointCommand 是带乐观版本的恢复点命令。
type SetCheckpointCommand struct {
	AccountID, CheckpointID snowflake.ID
	ExpectedVersion         int64
	IdempotencyKey          string
	Now                     time.Time
}

// PartyMember 是 Party 中从一开始的 Owned Creature 位置。
type PartyMember struct {
	Position                  int16
	PlayerCharacterCreatureID snowflake.ID
}

// Party 是 RPG 世界使用的有序编组。
type Party struct {
	ID      snowflake.ID
	Version int64
	Members []PartyMember
}

// AdminRegion 是管理员完整 Region 只读视图。
type AdminRegion struct {
	ID                      snowflake.ID
	Code, Name, Description string
	Enabled                 bool
	Version                 int64
}

// AdminLocation 是管理员完整 Location 只读视图。
type AdminLocation struct {
	ID, RegionID, ParentID   snowflake.ID
	Code, Name, LocationType string
	Enabled, DefaultSpawn    bool
	Version                  int64
}

// AdminExit 是管理员完整有向出口只读视图。
type AdminExit struct {
	ID, SourceLocationID, TargetLocationID snowflake.ID
	Code, Name, ConditionJSON, EffectJSON  string
	Enabled                                bool
	Version                                int64
}

// AdminIntegrityIssue 是拓扑报告问题只读视图。
type AdminIntegrityIssue struct{ ReasonCode, ResourceID, Message string }

// AdminIntegrityReport 是拓扑报告只读视图。
type AdminIntegrityReport struct {
	ID        snowflake.ID
	CheckedAt time.Time
	Passed    bool
	Issues    []AdminIntegrityIssue
}

// AdminWorldStore 是管理端地图资料只读边界。
type AdminWorldStore interface {
	ListRegions(context.Context, int) ([]AdminRegion, error)
	ListLocations(context.Context, int) ([]AdminLocation, error)
	ListExits(context.Context, int) ([]AdminExit, error)
	ListIntegrityReports(context.Context, int) ([]AdminIntegrityReport, error)
}

// ReplacePartyCommand 是带乐观版本的 Party 全量替换命令。
type ReplacePartyCommand struct {
	AccountID       snowflake.ID
	ExpectedVersion int64
	IdempotencyKey  string
	Members         []PartyMember
	Now             time.Time
}

// TraversalCommand 是一次幂等移动命令。
type TraversalCommand struct {
	AccountID, ExitID       snowflake.ID
	ExpectedPositionVersion int64
	IdempotencyKey          string
	Now                     time.Time
}

// TraversalResult 是同一幂等键必须确定性重放的提交结果。
type TraversalResult struct {
	Position         Position
	PendingEncounter *PendingEncounter
	Replayed         bool
}

// WorldStore 是 RPG 世界查询和原子 Traversal 的深持久层边界。
type WorldStore interface {
	GetMap(context.Context, snowflake.ID) (WorldMap, error)
	Traverse(context.Context, TraversalCommand) (TraversalResult, error)
	GetPendingEncounter(context.Context, snowflake.ID, time.Time) (*PendingEncounter, error)
	ResolvePendingEncounter(context.Context, ResolveEncounterCommand) (PendingEncounter, error)
	GetCheckpoint(context.Context, snowflake.ID) (*Checkpoint, error)
	SetCheckpoint(context.Context, SetCheckpointCommand) (Checkpoint, error)
	GetParty(context.Context, snowflake.ID) (Party, error)
	ReplaceParty(context.Context, ReplacePartyCommand) (Party, error)
}

// GetPendingEncounter 返回当前仍可处理的遭遇。
func (service *WorldService) GetPendingEncounter(ctx context.Context, accountID snowflake.ID, now time.Time) (*PendingEncounter, error) {
	return service.store.GetPendingEncounter(ctx, accountID, now)
}

// ResolvePendingEncounter 接受或取消一次待处理遭遇。
func (service *WorldService) ResolvePendingEncounter(ctx context.Context, command ResolveEncounterCommand) (PendingEncounter, error) {
	return service.store.ResolvePendingEncounter(ctx, command)
}

// GetCheckpoint 返回当前恢复点。
func (service *WorldService) GetCheckpoint(ctx context.Context, accountID snowflake.ID) (*Checkpoint, error) {
	return service.store.GetCheckpoint(ctx, accountID)
}

// SetCheckpoint 更新当前恢复点。
func (service *WorldService) SetCheckpoint(ctx context.Context, command SetCheckpointCommand) (Checkpoint, error) {
	return service.store.SetCheckpoint(ctx, command)
}

// GetParty 返回 RPG Party。
func (service *WorldService) GetParty(ctx context.Context, accountID snowflake.ID) (Party, error) {
	return service.store.GetParty(ctx, accountID)
}

// ReplaceParty 全量替换 RPG Party。
func (service *WorldService) ReplaceParty(ctx context.Context, command ReplacePartyCommand) (Party, error) {
	return service.store.ReplaceParty(ctx, command)
}

// WorldService 编排 RPG 世界用例，事务细节完全封装在 WorldStore。
type WorldService struct{ store WorldStore }

// NewWorldService 创建 RPG 世界用例。
func NewWorldService(store WorldStore) *WorldService { return &WorldService{store: store} }

// GetMap 返回当前账号活动角色的发现子图。
func (service *WorldService) GetMap(ctx context.Context, accountID snowflake.ID) (WorldMap, error) {
	return service.store.GetMap(ctx, accountID)
}

// Traverse 执行一次服务端权威原子移动。
func (service *WorldService) Traverse(ctx context.Context, command TraversalCommand) (TraversalResult, error) {
	if command.AccountID == snowflake.ID(0) || command.ExitID == snowflake.ID(0) || command.ExpectedPositionVersion <= 0 || command.IdempotencyKey == "" {
		return TraversalResult{}, ErrExitUnavailable
	}
	if command.Now.IsZero() {
		command.Now = time.Now().UTC()
	}
	return service.store.Traverse(ctx, command)
}
