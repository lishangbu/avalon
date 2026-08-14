package rpg

import (
	"context"
	"encoding/json"
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
	// ErrHeldItemUnavailable 表示目标道具不存在、停用、用途错误、没有可执行规则或背包数量不足。
	ErrHeldItemUnavailable = errors.New("Held Item 当前不可用")
	// ErrCreatureInBattle 表示活动角色已经被 Battle Reservation 占用，禁止变更 Creature 携带物。
	ErrCreatureInBattle = errors.New("Owned Creature 正在 Battle 中")
	// ErrOwnedCreatureConflict 表示 Owned Creature 不属于活动角色或乐观版本已经变化。
	ErrOwnedCreatureConflict = errors.New("Owned Creature 归属或版本冲突")
	// ErrInvalidAdminWorld 表示 RPG 管理写入字段无效。
	ErrInvalidAdminWorld = errors.New("RPG 管理资料字段无效")
	// ErrAdminWorldNotFound 表示 RPG 管理资料不存在。
	ErrAdminWorldNotFound = errors.New("RPG 管理资料不存在")
	// ErrAdminWorldConflict 表示稳定编码或乐观版本冲突。
	ErrAdminWorldConflict = errors.New("RPG 管理资料冲突")
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

// InventoryItem 是 PlayerCharacter 背包中一种可堆叠道具的当前读取事实。
type InventoryItem struct {
	// ItemID、ItemName 与 UsageType 来自当前 Item Catalog Entry。
	ItemID              snowflake.ID
	ItemName, UsageType string
	// Quantity 与 Version 是聚合 Inventory Stack 的当前数量和乐观版本。
	Quantity, Version int64
}

// OwnedCreatureHeldItem 是 Owned Creature 当前携带物的玩家安全读取事实。
type OwnedCreatureHeldItem struct {
	// PlayerCharacterCreatureID 与 CreatureID 分别标识资产实例和实时资料。
	PlayerCharacterCreatureID, CreatureID snowflake.ID
	// HeldItemID 为零表示未携带道具。
	HeldItemID snowflake.ID
	// Nickname 与 HeldItemName 是当前可选显示文案。
	Nickname, HeldItemName string
	// Version 是变更携带物所需的 Owned Creature 乐观版本。
	Version int64
}

// Inventory 聚合活动角色的非零背包和 Owned Creature 携带物读取视图。
type Inventory struct {
	// Items 是当前数量大于零的可堆叠 Inventory Stack 列表。
	Items []InventoryItem
	// OwnedCreatures 是当前角色全部 Owned Creature 及其可选携带物摘要。
	OwnedCreatures []OwnedCreatureHeldItem
}

// ReplaceHeldItemCommand 是原子扣还背包并替换 Owned Creature 携带物的幂等命令。
type ReplaceHeldItemCommand struct {
	// AccountID 与 OwnedCreatureID 确定活动角色及其 Owned Creature。
	AccountID, OwnedCreatureID snowflake.ID
	// ItemID 为空表示卸下，非空表示换成指定 Held Item。
	ItemID *snowflake.ID
	// ExpectedCreatureVersion 防止覆盖并发成长或携带物变化。
	ExpectedCreatureVersion int64
	// IdempotencyKey 确保网络重试不会重复扣还背包。
	IdempotencyKey string
	// Now 是事务事实统一使用的 UTC 提交时间。
	Now time.Time
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
	ID, CreatureID, FormID, LootTableID snowflake.ID
	MinimumLevel, MaximumLevel          int16
	Weight                              int32
	Enabled                             bool
	// NewRelation 表示本次聚合保存需要为该候选建立新的关系身份，不进入审计 JSON。
	NewRelation bool `json:"-"`
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

type adminEncounterTableJSON struct {
	ID                    string                    `json:"id"`
	LocationID            string                    `json:"location_id"`
	Code                  string                    `json:"code"`
	Name                  string                    `json:"name"`
	TriggerProbabilityBPS int32                     `json:"trigger_probability_bps"`
	CooldownMoves         int64                     `json:"cooldown_moves"`
	MaximumUses           *int32                    `json:"maximum_uses,omitempty"`
	Enabled               bool                      `json:"enabled"`
	Version               int64                     `json:"version"`
	Entries               []adminEncounterEntryJSON `json:"entries"`
}

type adminEncounterEntryJSON struct {
	ID           string `json:"id"`
	CreatureID   string `json:"creature_id"`
	FormID       string `json:"form_id,omitempty"`
	LootTableID  string `json:"loot_table_id,omitempty"`
	MinimumLevel int16  `json:"minimum_level"`
	MaximumLevel int16  `json:"maximum_level"`
	Weight       int32  `json:"weight"`
	Enabled      bool   `json:"enabled"`
}

// MarshalJSON 把遭遇聚合中的 Identifier 编码为十进制字符串，并以空字符串表达可选形态与掉落表缺失。
func (value AdminEncounterTable) MarshalJSON() ([]byte, error) {
	wire := adminEncounterTableJSON{ID: value.ID.String(), LocationID: value.LocationID.String(), Code: value.Code, Name: value.Name, TriggerProbabilityBPS: value.TriggerProbabilityBPS, CooldownMoves: value.CooldownMoves, MaximumUses: value.MaximumUses, Enabled: value.Enabled, Version: value.Version, Entries: make([]adminEncounterEntryJSON, 0, len(value.Entries))}
	for _, entry := range value.Entries {
		wire.Entries = append(wire.Entries, adminEncounterEntryJSON{ID: entry.ID.String(), CreatureID: entry.CreatureID.String(), FormID: optionalIDString(entry.FormID), LootTableID: optionalIDString(entry.LootTableID), MinimumLevel: entry.MinimumLevel, MaximumLevel: entry.MaximumLevel, Weight: entry.Weight, Enabled: entry.Enabled})
	}
	return json.Marshal(wire)
}

// UnmarshalJSON 从幂等响应恢复遭遇聚合，并严格校验必填与非空可选 Identifier。
func (value *AdminEncounterTable) UnmarshalJSON(raw []byte) error {
	var wire adminEncounterTableJSON
	if err := json.Unmarshal(raw, &wire); err != nil {
		return err
	}
	id, err := snowflake.Parse(wire.ID)
	if err != nil {
		return err
	}
	locationID, err := snowflake.Parse(wire.LocationID)
	if err != nil {
		return err
	}
	result := AdminEncounterTable{ID: id, LocationID: locationID, Code: wire.Code, Name: wire.Name, TriggerProbabilityBPS: wire.TriggerProbabilityBPS, CooldownMoves: wire.CooldownMoves, MaximumUses: wire.MaximumUses, Enabled: wire.Enabled, Version: wire.Version, Entries: make([]AdminEncounterEntry, 0, len(wire.Entries))}
	for _, entry := range wire.Entries {
		entryID, parseErr := snowflake.Parse(entry.ID)
		if parseErr != nil {
			return parseErr
		}
		creatureID, parseErr := snowflake.Parse(entry.CreatureID)
		if parseErr != nil {
			return parseErr
		}
		formID, parseErr := parseOptionalIDString(entry.FormID)
		if parseErr != nil {
			return parseErr
		}
		lootTableID, parseErr := parseOptionalIDString(entry.LootTableID)
		if parseErr != nil {
			return parseErr
		}
		result.Entries = append(result.Entries, AdminEncounterEntry{ID: entryID, CreatureID: creatureID, FormID: formID, LootTableID: lootTableID, MinimumLevel: entry.MinimumLevel, MaximumLevel: entry.MaximumLevel, Weight: entry.Weight, Enabled: entry.Enabled})
	}
	*value = result
	return nil
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
	// NewRelation 表示本次聚合保存需要为该掉落项建立新的关系身份，不进入审计 JSON。
	NewRelation bool `json:"-"`
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
	Enabled                bool
	// NewRelation 表示本次聚合保存需要为该商品建立新的关系身份，不进入审计 JSON。
	NewRelation bool `json:"-"`
}

// AdminShop 是包含全部商品的商店聚合。
type AdminShop struct {
	ID, NPCID, LocationID snowflake.ID
	Code, Name            string
	Enabled               bool
	Version               int64
	Items                 []AdminShopItem
}

type adminShopJSON struct {
	ID         string              `json:"id"`
	NPCID      string              `json:"npc_id,omitempty"`
	LocationID string              `json:"location_id"`
	Code       string              `json:"code"`
	Name       string              `json:"name"`
	Enabled    bool                `json:"enabled"`
	Version    int64               `json:"version"`
	Items      []adminShopItemJSON `json:"items"`
}

type adminShopItemJSON struct {
	ID         string `json:"id"`
	ItemID     string `json:"item_id"`
	CurrencyID string `json:"currency_id"`
	BuyPrice   int64  `json:"buy_price"`
	SellPrice  *int64 `json:"sell_price,omitempty"`
	Enabled    bool   `json:"enabled"`
}

// MarshalJSON 把商店聚合中的 Identifier 编码为前端安全的十进制字符串，并把可选 NPC 的缺失值编码为空字符串。
func (value AdminShop) MarshalJSON() ([]byte, error) {
	npcID := ""
	if value.NPCID.IsValid() {
		npcID = value.NPCID.String()
	}
	wire := adminShopJSON{ID: value.ID.String(), NPCID: npcID, LocationID: value.LocationID.String(), Code: value.Code, Name: value.Name, Enabled: value.Enabled, Version: value.Version, Items: make([]adminShopItemJSON, 0, len(value.Items))}
	for _, item := range value.Items {
		wire.Items = append(wire.Items, adminShopItemJSON{ID: item.ID.String(), ItemID: item.ItemID.String(), CurrencyID: item.CurrencyID.String(), BuyPrice: item.BuyPrice, SellPrice: item.SellPrice, Enabled: item.Enabled})
	}
	return json.Marshal(wire)
}

// UnmarshalJSON 从幂等响应恢复商店聚合，并对所有非空 Identifier 重新执行严格 Snowflake 校验。
func (value *AdminShop) UnmarshalJSON(raw []byte) error {
	var wire adminShopJSON
	if err := json.Unmarshal(raw, &wire); err != nil {
		return err
	}
	id, err := snowflake.Parse(wire.ID)
	if err != nil {
		return err
	}
	locationID, err := snowflake.Parse(wire.LocationID)
	if err != nil {
		return err
	}
	npcID := snowflake.ID(0)
	if wire.NPCID != "" {
		npcID, err = snowflake.Parse(wire.NPCID)
		if err != nil {
			return err
		}
	}
	items := make([]AdminShopItem, 0, len(wire.Items))
	for _, item := range wire.Items {
		shopItemID, parseErr := snowflake.Parse(item.ID)
		if parseErr != nil {
			return parseErr
		}
		itemID, parseErr := snowflake.Parse(item.ItemID)
		if parseErr != nil {
			return parseErr
		}
		currencyID, parseErr := snowflake.Parse(item.CurrencyID)
		if parseErr != nil {
			return parseErr
		}
		items = append(items, AdminShopItem{ID: shopItemID, ItemID: itemID, CurrencyID: currencyID, BuyPrice: item.BuyPrice, SellPrice: item.SellPrice, Enabled: item.Enabled})
	}
	*value = AdminShop{ID: id, NPCID: npcID, LocationID: locationID, Code: wire.Code, Name: wire.Name, Enabled: wire.Enabled, Version: wire.Version, Items: items}
	return nil
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
	// NewRelation 表示本次聚合保存需要为该目标建立新的关系身份，不进入审计 JSON。
	NewRelation bool `json:"-"`
}

// AdminQuestReward 是任务的一种互斥奖励关系。
type AdminQuestReward struct {
	ID, ItemID, CurrencyID snowflake.ID
	Quantity               int64
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

type adminQuestJSON struct {
	ID                  string                    `json:"id"`
	StartNPCID          string                    `json:"start_npc_id,omitempty"`
	TurnInNPCID         string                    `json:"turn_in_npc_id,omitempty"`
	PrerequisiteQuestID string                    `json:"prerequisite_quest_id,omitempty"`
	Code                string                    `json:"code"`
	Name                string                    `json:"name"`
	QuestType           string                    `json:"quest_type"`
	Description         string                    `json:"description"`
	Repeatable          bool                      `json:"repeatable"`
	Enabled             bool                      `json:"enabled"`
	Version             int64                     `json:"version"`
	Objectives          []adminQuestObjectiveJSON `json:"objectives"`
	Rewards             []adminQuestRewardJSON    `json:"rewards"`
}

type adminQuestObjectiveJSON struct {
	ID               string `json:"id"`
	Code             string `json:"code"`
	ObjectiveType    string `json:"objective_type"`
	Description      string `json:"description"`
	TargetCreatureID string `json:"target_creature_id,omitempty"`
	TargetItemID     string `json:"target_item_id,omitempty"`
	TargetLocationID string `json:"target_location_id,omitempty"`
	TargetNPCID      string `json:"target_npc_id,omitempty"`
	Position         int16  `json:"position"`
	RequiredCount    int32  `json:"required_count"`
}

type adminQuestRewardJSON struct {
	ID         string `json:"id"`
	ItemID     string `json:"item_id,omitempty"`
	CurrencyID string `json:"currency_id,omitempty"`
	Quantity   int64  `json:"quantity"`
}

// MarshalJSON 把任务聚合中的 Identifier 编码为前端安全的十进制字符串，并以空字符串表达可选引用缺失。
func (value AdminQuest) MarshalJSON() ([]byte, error) {
	wire := adminQuestJSON{ID: value.ID.String(), StartNPCID: optionalIDString(value.StartNPCID), TurnInNPCID: optionalIDString(value.TurnInNPCID), PrerequisiteQuestID: optionalIDString(value.PrerequisiteQuestID), Code: value.Code, Name: value.Name, QuestType: value.QuestType, Description: value.Description, Repeatable: value.Repeatable, Enabled: value.Enabled, Version: value.Version, Objectives: make([]adminQuestObjectiveJSON, 0, len(value.Objectives)), Rewards: make([]adminQuestRewardJSON, 0, len(value.Rewards))}
	for _, objective := range value.Objectives {
		wire.Objectives = append(wire.Objectives, adminQuestObjectiveJSON{ID: objective.ID.String(), Code: objective.Code, Position: objective.Position, ObjectiveType: objective.ObjectiveType, TargetCreatureID: optionalIDString(objective.TargetCreatureID), TargetItemID: optionalIDString(objective.TargetItemID), TargetLocationID: optionalIDString(objective.TargetLocationID), TargetNPCID: optionalIDString(objective.TargetNPCID), RequiredCount: objective.RequiredCount, Description: objective.Description})
	}
	for _, reward := range value.Rewards {
		wire.Rewards = append(wire.Rewards, adminQuestRewardJSON{ID: reward.ID.String(), ItemID: optionalIDString(reward.ItemID), CurrencyID: optionalIDString(reward.CurrencyID), Quantity: reward.Quantity})
	}
	return json.Marshal(wire)
}

// UnmarshalJSON 从幂等响应恢复任务聚合，并对全部必填与非空可选 Identifier 执行严格 Snowflake 校验。
func (value *AdminQuest) UnmarshalJSON(raw []byte) error {
	var wire adminQuestJSON
	if err := json.Unmarshal(raw, &wire); err != nil {
		return err
	}
	id, err := snowflake.Parse(wire.ID)
	if err != nil {
		return err
	}
	startNPCID, err := parseOptionalIDString(wire.StartNPCID)
	if err != nil {
		return err
	}
	turnInNPCID, err := parseOptionalIDString(wire.TurnInNPCID)
	if err != nil {
		return err
	}
	prerequisiteQuestID, err := parseOptionalIDString(wire.PrerequisiteQuestID)
	if err != nil {
		return err
	}
	result := AdminQuest{ID: id, StartNPCID: startNPCID, TurnInNPCID: turnInNPCID, PrerequisiteQuestID: prerequisiteQuestID, Code: wire.Code, Name: wire.Name, QuestType: wire.QuestType, Description: wire.Description, Repeatable: wire.Repeatable, Enabled: wire.Enabled, Version: wire.Version, Objectives: make([]AdminQuestObjective, 0, len(wire.Objectives)), Rewards: make([]AdminQuestReward, 0, len(wire.Rewards))}
	for _, objective := range wire.Objectives {
		objectiveID, parseErr := snowflake.Parse(objective.ID)
		if parseErr != nil {
			return parseErr
		}
		targetCreatureID, parseErr := parseOptionalIDString(objective.TargetCreatureID)
		if parseErr != nil {
			return parseErr
		}
		targetItemID, parseErr := parseOptionalIDString(objective.TargetItemID)
		if parseErr != nil {
			return parseErr
		}
		targetLocationID, parseErr := parseOptionalIDString(objective.TargetLocationID)
		if parseErr != nil {
			return parseErr
		}
		targetNPCID, parseErr := parseOptionalIDString(objective.TargetNPCID)
		if parseErr != nil {
			return parseErr
		}
		result.Objectives = append(result.Objectives, AdminQuestObjective{ID: objectiveID, Code: objective.Code, Position: objective.Position, ObjectiveType: objective.ObjectiveType, TargetCreatureID: targetCreatureID, TargetItemID: targetItemID, TargetLocationID: targetLocationID, TargetNPCID: targetNPCID, RequiredCount: objective.RequiredCount, Description: objective.Description})
	}
	for _, reward := range wire.Rewards {
		rewardID, parseErr := snowflake.Parse(reward.ID)
		if parseErr != nil {
			return parseErr
		}
		itemID, parseErr := parseOptionalIDString(reward.ItemID)
		if parseErr != nil {
			return parseErr
		}
		currencyID, parseErr := parseOptionalIDString(reward.CurrencyID)
		if parseErr != nil {
			return parseErr
		}
		result.Rewards = append(result.Rewards, AdminQuestReward{ID: rewardID, ItemID: itemID, CurrencyID: currencyID, Quantity: reward.Quantity})
	}
	*value = result
	return nil
}

func optionalIDString(value snowflake.ID) string {
	if !value.IsValid() {
		return ""
	}
	return value.String()
}

func parseOptionalIDString(value string) (snowflake.ID, error) {
	if value == "" {
		return 0, nil
	}
	return snowflake.Parse(value)
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

// AdminWorldQuery 是管理端 RPG 分页列表、选项和诊断投影的只读端口。
type AdminWorldQuery interface {
	ListRegions(context.Context, int) ([]AdminRegion, error)
	ListLocations(context.Context, int) ([]AdminLocation, error)
	ListExits(context.Context, int) ([]AdminExit, error)
	ListIntegrityReports(context.Context, int) ([]AdminIntegrityReport, error)
	ListCheckpoints(context.Context, int) ([]AdminCheckpoint, error)
	ListEncounterTables(context.Context, int) ([]AdminEncounterTable, error)
	ListMapProjections(context.Context, int) ([]AdminMapProjection, error)
	ListNPCs(context.Context, int) ([]AdminNPC, error)
	ListDialogues(context.Context, int) ([]AdminDialogue, error)
	ListLootTables(context.Context, int) ([]AdminLootTable, error)
	ListShops(context.Context, int) ([]AdminShop, error)
	ListQuests(context.Context, int) ([]AdminQuest, error)
	ListRecipes(context.Context, int) ([]AdminRecipe, error)
	ListProfessions(context.Context, int) ([]AdminProfession, error)
	ListEquipments(context.Context, int, string) (AdminEquipmentPage, error)
	ListEquipmentOptions(context.Context) ([]EquipmentOption, error)
	ListAdminEquipmentInstances(context.Context, AdminEquipmentInstanceQuery) (AdminEquipmentInstancePage, error)
	ListEquipmentTransactions(context.Context, EquipmentTransactionQuery) (AdminEquipmentTransactionPage, error)
}

// AdminWorldRepository 是管理端 RPG 聚合写入的关系型持久化端口。
type AdminWorldRepository interface {
	CreateRegion(context.Context, SaveRegionCommand) (AdminRegion, error)
	UpdateRegion(context.Context, SaveRegionCommand) (AdminRegion, error)
	CreateLocation(context.Context, SaveLocationCommand) (AdminLocation, error)
	UpdateLocation(context.Context, SaveLocationCommand) (AdminLocation, error)
	CreateExit(context.Context, SaveExitCommand) (AdminExit, error)
	UpdateExit(context.Context, SaveExitCommand) (AdminExit, error)
	CreateCheckpoint(context.Context, SaveCheckpointCommand) (AdminCheckpoint, error)
	UpdateCheckpoint(context.Context, SaveCheckpointCommand) (AdminCheckpoint, error)
	CreateEncounterTable(context.Context, SaveEncounterTableCommand) (AdminEncounterTable, error)
	UpdateEncounterTable(context.Context, SaveEncounterTableCommand) (AdminEncounterTable, error)
	CreateMapProjection(context.Context, SaveMapProjectionCommand) (AdminMapProjection, error)
	UpdateMapProjection(context.Context, SaveMapProjectionCommand) (AdminMapProjection, error)
	SaveNPC(context.Context, SaveNPCCommand) (AdminNPC, error)
	SaveDialogue(context.Context, SaveDialogueCommand) (AdminDialogue, error)
	SaveLootTable(context.Context, SaveLootTableCommand) (AdminLootTable, error)
	SaveShop(context.Context, SaveShopCommand) (AdminShop, error)
	SaveQuest(context.Context, SaveQuestCommand) (AdminQuest, error)
	SaveRecipe(context.Context, SaveRecipeCommand) (AdminRecipe, error)
	SaveProfession(context.Context, SaveProfessionCommand) (AdminProfession, error)
	SaveEquipment(context.Context, SaveEquipmentCommand) (AdminEquipment, error)
	GrantEquipment(context.Context, GrantEquipmentCommand) (GrantEquipmentResult, error)
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

// WorldRepository 是 RPG 世界聚合查询和原子 Traversal 的关系型持久化端口。
type WorldRepository interface {
	GetMap(context.Context, snowflake.ID) (WorldMap, error)
	Traverse(context.Context, TraversalCommand) (TraversalResult, error)
	GetPendingEncounter(context.Context, snowflake.ID, time.Time) (*PendingEncounter, error)
	ResolvePendingEncounter(context.Context, ResolveEncounterCommand) (PendingEncounter, error)
	GetCheckpoint(context.Context, snowflake.ID) (*Checkpoint, error)
	SetCheckpoint(context.Context, SetCheckpointCommand) (Checkpoint, error)
	GetParty(context.Context, snowflake.ID) (Party, error)
	ReplaceParty(context.Context, ReplacePartyCommand) (Party, error)
	GetInventory(context.Context, snowflake.ID) (Inventory, error)
	ReplaceHeldItem(context.Context, ReplaceHeldItemCommand) (OwnedCreatureHeldItem, error)
	PurchaseShopItem(context.Context, PurchaseShopItemCommand) (ItemAcquisitionResult, error)
	ListAvailableQuests(context.Context, snowflake.ID) ([]AvailableQuest, error)
	ListQuestProgress(context.Context, snowflake.ID) ([]QuestProgress, error)
	StartQuest(context.Context, StartQuestCommand) (QuestProgress, error)
	CompleteQuest(context.Context, CompleteQuestCommand) (QuestProgress, error)
	ClaimQuestRewards(context.Context, ClaimQuestRewardsCommand) (RewardAcquisitionResult, error)
	ClaimLootSettlement(context.Context, ClaimLootSettlementCommand) (RewardAcquisitionResult, error)
	EquipmentRepository
	ProfessionRepository
}

// GetPendingEncounter 返回当前仍可处理的遭遇。
func (service *WorldService) GetPendingEncounter(ctx context.Context, accountID snowflake.ID, now time.Time) (*PendingEncounter, error) {
	return service.repository.GetPendingEncounter(ctx, accountID, now)
}

// ResolvePendingEncounter 接受或取消一次待处理遭遇。
func (service *WorldService) ResolvePendingEncounter(ctx context.Context, command ResolveEncounterCommand) (PendingEncounter, error) {
	return service.repository.ResolvePendingEncounter(ctx, command)
}

// GetCheckpoint 返回当前恢复点。
func (service *WorldService) GetCheckpoint(ctx context.Context, accountID snowflake.ID) (*Checkpoint, error) {
	return service.repository.GetCheckpoint(ctx, accountID)
}

// SetCheckpoint 更新当前恢复点。
func (service *WorldService) SetCheckpoint(ctx context.Context, command SetCheckpointCommand) (Checkpoint, error) {
	return service.repository.SetCheckpoint(ctx, command)
}

// GetParty 返回 RPG Party。
func (service *WorldService) GetParty(ctx context.Context, accountID snowflake.ID) (Party, error) {
	return service.repository.GetParty(ctx, accountID)
}

// ReplaceParty 全量替换 RPG Party。
func (service *WorldService) ReplaceParty(ctx context.Context, command ReplacePartyCommand) (Party, error) {
	return service.repository.ReplaceParty(ctx, command)
}

// GetInventory 返回活动角色的聚合背包与 Owned Creature 携带物。
func (service *WorldService) GetInventory(ctx context.Context, accountID snowflake.ID) (Inventory, error) {
	return service.repository.GetInventory(ctx, accountID)
}

// ReplaceHeldItem 校验命令后委托持久层执行单一原子事务。
func (service *WorldService) ReplaceHeldItem(ctx context.Context, command ReplaceHeldItemCommand) (OwnedCreatureHeldItem, error) {
	if !command.AccountID.IsValid() || !command.OwnedCreatureID.IsValid() || command.ExpectedCreatureVersion <= 0 || command.IdempotencyKey == "" {
		return OwnedCreatureHeldItem{}, ErrOwnedCreatureConflict
	}
	if command.ItemID != nil && !command.ItemID.IsValid() {
		return OwnedCreatureHeldItem{}, ErrHeldItemUnavailable
	}
	if command.Now.IsZero() {
		command.Now = time.Now().UTC()
	}
	return service.repository.ReplaceHeldItem(ctx, command)
}

// ListEquipmentInstances 返回活动角色拥有的装备实例。
func (service *WorldService) ListEquipmentInstances(ctx context.Context, accountID snowflake.ID, size int, cursor string) (EquipmentInstancePage, error) {
	return service.repository.ListEquipmentInstances(ctx, accountID, size, cursor)
}

// GetEquipmentInstance 返回活动角色拥有的一个装备实例。
func (service *WorldService) GetEquipmentInstance(ctx context.Context, accountID, instanceID snowflake.ID) (EquipmentInstance, error) {
	return service.repository.GetEquipmentInstance(ctx, accountID, instanceID)
}

// GetEquipmentLoadout 返回活动角色当前整套装备。
func (service *WorldService) GetEquipmentLoadout(ctx context.Context, accountID snowflake.ID) (EquipmentLoadout, error) {
	return service.repository.GetEquipmentLoadout(ctx, accountID)
}

// ReplaceEquipmentLoadout 原子替换活动角色整套装备。
func (service *WorldService) ReplaceEquipmentLoadout(ctx context.Context, command ReplaceEquipmentLoadoutCommand) (EquipmentLoadout, error) {
	return service.repository.ReplaceEquipmentLoadout(ctx, command)
}

// SellEquipmentInstance 出售一个未穿戴装备实例。
func (service *WorldService) SellEquipmentInstance(ctx context.Context, command SellEquipmentCommand) (SellEquipmentResult, error) {
	return service.repository.SellEquipmentInstance(ctx, command)
}

// PurchaseShopItem 原子支付并按 Item 资料交付普通道具或独立 Equipment Instance。
func (service *WorldService) PurchaseShopItem(ctx context.Context, command PurchaseShopItemCommand) (ItemAcquisitionResult, error) {
	return service.repository.PurchaseShopItem(ctx, command)
}

// ListAvailableQuests 返回当前角色在当前位置可以开始的任务定义。
func (service *WorldService) ListAvailableQuests(ctx context.Context, accountID snowflake.ID) ([]AvailableQuest, error) {
	return service.repository.ListAvailableQuests(ctx, accountID)
}

// ListQuestProgress 返回当前角色全部任务生命周期与目标进度。
func (service *WorldService) ListQuestProgress(ctx context.Context, accountID snowflake.ID) ([]QuestProgress, error) {
	return service.repository.ListQuestProgress(ctx, accountID)
}

// StartQuest 原子开始首轮或下一轮可重复任务。
func (service *WorldService) StartQuest(ctx context.Context, command StartQuestCommand) (QuestProgress, error) {
	return service.repository.StartQuest(ctx, command)
}

// CompleteQuest 在权威交付地点完成目标已经达成的任务轮次。
func (service *WorldService) CompleteQuest(ctx context.Context, command CompleteQuestCommand) (QuestProgress, error) {
	return service.repository.CompleteQuest(ctx, command)
}

// ClaimQuestRewards 领取当前完成轮次的全部任务奖励。
func (service *WorldService) ClaimQuestRewards(ctx context.Context, command ClaimQuestRewardsCommand) (RewardAcquisitionResult, error) {
	return service.repository.ClaimQuestRewards(ctx, command)
}

// ClaimLootSettlement 领取服务端预先建立的权威掉落结算。
func (service *WorldService) ClaimLootSettlement(ctx context.Context, command ClaimLootSettlementCommand) (RewardAcquisitionResult, error) {
	return service.repository.ClaimLootSettlement(ctx, command)
}

// GetActiveProfessions 返回当前参与装备资格判定的职业成长集合。
func (service *WorldService) GetActiveProfessions(ctx context.Context, accountID snowflake.ID) ([]ActiveProfession, error) {
	return service.repository.GetActiveProfessions(ctx, accountID)
}

// ReplaceActiveProfessions 在当前 Loadout 仍合法时原子替换激活职业集合。
func (service *WorldService) ReplaceActiveProfessions(ctx context.Context, command ReplaceActiveProfessionsCommand) ([]ActiveProfession, error) {
	return service.repository.ReplaceActiveProfessions(ctx, command)
}

// WorldService 编排 RPG 世界用例，事务细节完全封装在 WorldRepository。
type WorldService struct{ repository WorldRepository }

// NewWorldService 创建 RPG 世界用例。
func NewWorldService(repository WorldRepository) *WorldService {
	return &WorldService{repository: repository}
}

// GetMap 返回当前账号活动角色的发现子图。
func (service *WorldService) GetMap(ctx context.Context, accountID snowflake.ID) (WorldMap, error) {
	return service.repository.GetMap(ctx, accountID)
}

// Traverse 执行一次服务端权威原子移动。
func (service *WorldService) Traverse(ctx context.Context, command TraversalCommand) (TraversalResult, error) {
	if command.AccountID == snowflake.ID(0) || command.ExitID == snowflake.ID(0) || command.ExpectedPositionVersion <= 0 || command.IdempotencyKey == "" {
		return TraversalResult{}, ErrExitUnavailable
	}
	if command.Now.IsZero() {
		command.Now = time.Now().UTC()
	}
	return service.repository.Traverse(ctx, command)
}
