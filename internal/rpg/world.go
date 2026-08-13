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

// AdminWriteContext 保存一次管理员 RPG 资料写入的审计与幂等身份。
type AdminWriteContext struct {
	// ActorAccountID 是执行写入的管理员账号。
	ActorAccountID snowflake.ID
	// IdempotencyKey 在管理员与操作范围内唯一标识本次命令。
	IdempotencyKey string
	// RequestID 把管理审计关联到入口请求。
	RequestID string
}

// SaveRegionCommand 是 Region 创建或完整更新命令。
type SaveRegionCommand struct {
	// Write 保存认证、幂等与审计上下文。
	Write AdminWriteContext
	// Region 是待创建或完整替换的区域资料。
	Region AdminRegion
	// ExpectedVersion 是更新命令要求的当前版本；创建时为零。
	ExpectedVersion int64
}

// AdminLocation 是管理员完整 Location 只读视图。
type AdminLocation struct {
	ID, RegionID, ParentID   snowflake.ID
	Code, Name, LocationType string
	Description              string
	Enabled, DefaultSpawn    bool
	Version                  int64
}

// SaveLocationCommand 是 Location 创建或完整更新命令。
type SaveLocationCommand struct {
	// Write 保存认证、幂等与审计上下文。
	Write AdminWriteContext
	// Location 是待创建或完整替换的地点资料。
	Location AdminLocation
	// ExpectedVersion 是更新命令要求的当前版本；创建时为零。
	ExpectedVersion int64
}

// AdminExit 是管理员完整有向出口只读视图。
type AdminExit struct {
	ID, SourceLocationID, TargetLocationID snowflake.ID
	Code, Name, Description                string
	ConditionJSON, EffectJSON              string
	SortOrder                              int32
	Enabled                                bool
	Version                                int64
}

// SaveExitCommand 是 Location Exit 创建或完整更新命令。
type SaveExitCommand struct {
	// Write 保存认证、幂等与审计上下文。
	Write AdminWriteContext
	// Exit 是待创建或完整替换的出口资料。
	Exit AdminExit
	// ExpectedVersion 是更新命令要求的当前版本；创建时为零。
	ExpectedVersion int64
}

// AdminCheckpoint 是管理员可见的完整恢复点资料。
type AdminCheckpoint struct {
	ID, LocationID                                                   snowflake.ID
	Code, Name, Description, SetConditionJSON, RecoveryConditionJSON string
	Enabled                                                          bool
	Version                                                          int64
}

// SaveCheckpointCommand 是恢复点创建或完整更新命令。
type SaveCheckpointCommand struct {
	Write           AdminWriteContext
	Checkpoint      AdminCheckpoint
	ExpectedVersion int64
}

// AdminEncounterEntry 是遭遇表内的加权候选。
type AdminEncounterEntry struct {
	ID, CreatureID, FormID     snowflake.ID
	MinimumLevel, MaximumLevel int16
	Weight                     int32
	Enabled                    bool
}

// AdminEncounterTable 是包含候选关系的完整遭遇聚合。
type AdminEncounterTable struct {
	ID, LocationID        snowflake.ID
	Code, Name            string
	TriggerProbabilityBPS int32
	CooldownMoves         int64
	MaximumUses           *int32
	Enabled               bool
	Version               int64
	Entries               []AdminEncounterEntry
}

// SaveEncounterTableCommand 是遭遇表创建或完整更新命令。
type SaveEncounterTableCommand struct {
	Write           AdminWriteContext
	Table           AdminEncounterTable
	ExpectedVersion int64
}

// AdminMapProjectionLocation 是投影内一个地点的纯展示坐标和资产引用。
type AdminMapProjectionLocation struct {
	ID, LocationID, IconAssetID, BackgroundAssetID snowflake.ID
	X, Y, Z                                        int32
}

// AdminMapProjection 是包含地点展示关系的地图投影聚合。
type AdminMapProjection struct {
	ID            snowflake.ID
	Code, Name    string
	LayoutVersion int64
	Enabled       bool
	Locations     []AdminMapProjectionLocation
}

// SaveMapProjectionCommand 是地图投影创建或完整更新命令。
type SaveMapProjectionCommand struct {
	Write                 AdminWriteContext
	Projection            AdminMapProjection
	ExpectedLayoutVersion int64
}

// AdminNPC 是 RPG 非玩家角色维护聚合。
type AdminNPC struct {
	ID, LocationID                   snowflake.ID
	Code, Name, NPCType, Description string
	Enabled                          bool
	Version                          int64
}

// AdminDialogueLine 是对话中的有序文本关系。
type AdminDialogueLine struct {
	ID                   snowflake.ID
	Position             int32
	SpeakerName, Content string
}

// AdminDialogue 是包含全部对话行的对话聚合。
type AdminDialogue struct {
	ID, NPCID  snowflake.ID
	Code, Name string
	Enabled    bool
	Version    int64
	Lines      []AdminDialogueLine
}

// AdminLootEntry 是掉落表中的道具权重关系。
type AdminLootEntry struct {
	ID, ItemID                               snowflake.ID
	MinimumQuantity, MaximumQuantity, Weight int32
}

// AdminLootTable 是包含全部掉落项的掉落聚合。
type AdminLootTable struct {
	ID         snowflake.ID
	Code, Name string
	Enabled    bool
	Version    int64
	Entries    []AdminLootEntry
}

// AdminShopItem 是商店内道具、货币和价格关系。
type AdminShopItem struct {
	ID, ItemID, CurrencyID snowflake.ID
	BuyPrice               int64
	SellPrice              *int64
	StockLimit             *int32
	Enabled                bool
}

// AdminShop 是包含全部商品的商店聚合。
type AdminShop struct {
	ID, NPCID, LocationID snowflake.ID
	Code, Name            string
	Enabled               bool
	Version               int64
	Items                 []AdminShopItem
}

// SaveNPCCommand 是 NPC 创建或更新命令。
type SaveNPCCommand struct {
	Write           AdminWriteContext
	Value           AdminNPC
	ExpectedVersion int64
}

// SaveDialogueCommand 是对话聚合创建或更新命令。
type SaveDialogueCommand struct {
	Write           AdminWriteContext
	Value           AdminDialogue
	ExpectedVersion int64
}

// SaveLootTableCommand 是掉落聚合创建或更新命令。
type SaveLootTableCommand struct {
	Write           AdminWriteContext
	Value           AdminLootTable
	ExpectedVersion int64
}

// SaveShopCommand 是商店聚合创建或更新命令。
type SaveShopCommand struct {
	Write           AdminWriteContext
	Value           AdminShop
	ExpectedVersion int64
}

// AdminQuestObjective 是任务中的有序结构化目标。
type AdminQuestObjective struct {
	ID                                                            snowflake.ID
	Code                                                          string
	Position                                                      int16
	ObjectiveType                                                 string
	TargetCreatureID, TargetItemID, TargetLocationID, TargetNPCID snowflake.ID
	RequiredCount                                                 int32
	Description                                                   string
}

// AdminQuestReward 是任务的一种互斥奖励关系。
type AdminQuestReward struct {
	ID, ItemID, CurrencyID, CreatureID snowflake.ID
	Quantity                           int64
}

// AdminQuest 是包含目标和奖励的任务聚合。
type AdminQuest struct {
	ID, StartNPCID, TurnInNPCID, PrerequisiteQuestID snowflake.ID
	Code, Name, QuestType, Description               string
	Repeatable, Enabled                              bool
	Version                                          int64
	Objectives                                       []AdminQuestObjective
	Rewards                                          []AdminQuestReward
}

// AdminRecipeItem 是配方的一种道具数量关系。
type AdminRecipeItem struct {
	ID, ItemID snowflake.ID
	Quantity   int32
}

// AdminRecipe 是包含材料和产物的制作配方聚合。
type AdminRecipe struct {
	ID                                 snowflake.ID
	Code, Name, RequiredProfessionCode string
	RequiredProfessionLevel            *int32
	Enabled                            bool
	Version                            int64
	Ingredients, Outputs               []AdminRecipeItem
}

// AdminProfessionSkill 是职业内可解锁技能关系。
type AdminProfessionSkill struct {
	ID                      snowflake.ID
	Code, Name, Description string
	RequiredLevel           int32
	Enabled                 bool
}

// AdminProfession 是包含技能的职业聚合。
type AdminProfession struct {
	ID                      snowflake.ID
	Code, Name, Description string
	MaximumLevel            int32
	Enabled                 bool
	Version                 int64
	Skills                  []AdminProfessionSkill
}

// SaveQuestCommand 是任务聚合创建或更新命令。
type SaveQuestCommand struct {
	Write           AdminWriteContext
	Value           AdminQuest
	ExpectedVersion int64
}

// SaveRecipeCommand 是配方聚合创建或更新命令。
type SaveRecipeCommand struct {
	Write           AdminWriteContext
	Value           AdminRecipe
	ExpectedVersion int64
}

// SaveProfessionCommand 是职业聚合创建或更新命令。
type SaveProfessionCommand struct {
	Write           AdminWriteContext
	Value           AdminProfession
	ExpectedVersion int64
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
	CreateRegion(context.Context, SaveRegionCommand) (AdminRegion, error)
	UpdateRegion(context.Context, SaveRegionCommand) (AdminRegion, error)
	CreateLocation(context.Context, SaveLocationCommand) (AdminLocation, error)
	UpdateLocation(context.Context, SaveLocationCommand) (AdminLocation, error)
	CreateExit(context.Context, SaveExitCommand) (AdminExit, error)
	UpdateExit(context.Context, SaveExitCommand) (AdminExit, error)
	ListCheckpoints(context.Context, int) ([]AdminCheckpoint, error)
	CreateCheckpoint(context.Context, SaveCheckpointCommand) (AdminCheckpoint, error)
	UpdateCheckpoint(context.Context, SaveCheckpointCommand) (AdminCheckpoint, error)
	ListEncounterTables(context.Context, int) ([]AdminEncounterTable, error)
	CreateEncounterTable(context.Context, SaveEncounterTableCommand) (AdminEncounterTable, error)
	UpdateEncounterTable(context.Context, SaveEncounterTableCommand) (AdminEncounterTable, error)
	ListMapProjections(context.Context, int) ([]AdminMapProjection, error)
	CreateMapProjection(context.Context, SaveMapProjectionCommand) (AdminMapProjection, error)
	UpdateMapProjection(context.Context, SaveMapProjectionCommand) (AdminMapProjection, error)
	ListNPCs(context.Context, int) ([]AdminNPC, error)
	SaveNPC(context.Context, SaveNPCCommand) (AdminNPC, error)
	ListDialogues(context.Context, int) ([]AdminDialogue, error)
	SaveDialogue(context.Context, SaveDialogueCommand) (AdminDialogue, error)
	ListLootTables(context.Context, int) ([]AdminLootTable, error)
	SaveLootTable(context.Context, SaveLootTableCommand) (AdminLootTable, error)
	ListShops(context.Context, int) ([]AdminShop, error)
	SaveShop(context.Context, SaveShopCommand) (AdminShop, error)
	ListQuests(context.Context, int) ([]AdminQuest, error)
	SaveQuest(context.Context, SaveQuestCommand) (AdminQuest, error)
	ListRecipes(context.Context, int) ([]AdminRecipe, error)
	SaveRecipe(context.Context, SaveRecipeCommand) (AdminRecipe, error)
	ListProfessions(context.Context, int) ([]AdminProfession, error)
	SaveProfession(context.Context, SaveProfessionCommand) (AdminProfession, error)
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
