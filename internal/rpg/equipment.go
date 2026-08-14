package rpg

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"io"
	"math"
	"sort"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

var (
	// ErrEquipmentNotFound 表示装备资料或实例不存在于当前命令边界。
	ErrEquipmentNotFound = errors.New("Equipment 不存在")
	// ErrEquipmentNotOwned 表示实例不属于活动 PlayerCharacter。
	ErrEquipmentNotOwned = errors.New("Equipment Instance 不属于当前角色")
	// ErrEquipmentInBattle 表示 Battle Reservation 阻止角色换装。
	ErrEquipmentInBattle = errors.New("PlayerCharacter 正在 Battle 中")
	// ErrInvalidEquipmentCursor 表示装备列表游标损坏、类型不匹配或不属于当前筛选条件。
	ErrInvalidEquipmentCursor = errors.New("Equipment 列表游标无效")
	// ErrInvalidEquipmentFilter 表示管理诊断使用了来源或动作闭集之外的筛选值。
	ErrInvalidEquipmentFilter = errors.New("Equipment 列表筛选无效")
)

var (
	// ErrEquipmentSlotMismatch 表示资料槽位类型不能放入请求的固定 Loadout 槽位。
	ErrEquipmentSlotMismatch = errors.New("Equipment 槽位不匹配")
	// ErrEquipmentRequirementNotMet 表示 PlayerCharacter 等级或职业不满足穿戴资格。
	ErrEquipmentRequirementNotMet = errors.New("Equipment 穿戴资格未满足")
	// ErrEquipmentLoadoutConflict 表示同一槽位或实例在最终 Loadout 中重复。
	ErrEquipmentLoadoutConflict = errors.New("Equipment Loadout 冲突")
	// ErrEquipmentTwoHandedConflict 表示双手武器与副手实例同时存在。
	ErrEquipmentTwoHandedConflict = errors.New("双手 Equipment 与副手冲突")
	// ErrEquipmentRulesInvalid 表示 Equipment Rules 不是受支持的闭集强类型文档。
	ErrEquipmentRulesInvalid = errors.New("Equipment Rules 无效")
	// ErrEquipmentStatModifierInvalid 表示单项或汇总属性修正超出稳定范围。
	ErrEquipmentStatModifierInvalid = errors.New("Equipment 属性修正无效")
)

// EquipmentSlot 是 PlayerCharacter Equipment Loadout 的固定后端闭集槽位。
type EquipmentSlot string

const (
	EquipmentSlotMainHand   EquipmentSlot = "main_hand"
	EquipmentSlotOffHand    EquipmentSlot = "off_hand"
	EquipmentSlotHead       EquipmentSlot = "head"
	EquipmentSlotBody       EquipmentSlot = "body"
	EquipmentSlotHands      EquipmentSlot = "hands"
	EquipmentSlotFeet       EquipmentSlot = "feet"
	EquipmentSlotAccessory1 EquipmentSlot = "accessory_1"
	EquipmentSlotAccessory2 EquipmentSlot = "accessory_2"
)

// EquipmentSlotType 是 Equipment Catalog Entry 声明的可穿戴资料槽位类型。
type EquipmentSlotType string

const (
	EquipmentSlotTypeMainHand  EquipmentSlotType = "main_hand"
	EquipmentSlotTypeOffHand   EquipmentSlotType = "off_hand"
	EquipmentSlotTypeHead      EquipmentSlotType = "head"
	EquipmentSlotTypeBody      EquipmentSlotType = "body"
	EquipmentSlotTypeHands     EquipmentSlotType = "hands"
	EquipmentSlotTypeFeet      EquipmentSlotType = "feet"
	EquipmentSlotTypeAccessory EquipmentSlotType = "accessory"
)

// EquipmentHandedness 是手部装备占用手位的闭集身份。
type EquipmentHandedness string

const (
	EquipmentHandednessOneHanded EquipmentHandedness = "one_handed"
	EquipmentHandednessTwoHanded EquipmentHandedness = "two_handed"
	EquipmentHandednessOffHand   EquipmentHandedness = "off_hand"
)

// EquipmentLoadoutCandidate 是完成数据库读取后的单个最终槽位校验输入。
type EquipmentLoadoutCandidate struct {
	// Slot 与 InstanceID 标识最终 Loadout 关系。
	Slot       EquipmentSlot
	InstanceID snowflake.ID
	// SlotType 与 Handedness 来自实例引用的 Equipment Catalog Entry。
	SlotType   EquipmentSlotType
	Handedness EquipmentHandedness
	// MinimumLevel 与 ProfessionIDs 是该资料的全部首期资格条件。
	MinimumLevel  int32
	ProfessionIDs []snowflake.ID
}

// EquipmentStatModifier 是一种 Game Stat 上的平加与万分比修正。
type EquipmentStatModifier struct {
	// StatID 是当前修正所作用的 Game Stat 稳定身份。
	StatID snowflake.ID `json:"statId"`
	// FlatValue 是应用百分比前累加的有符号整数修正。
	FlatValue int64 `json:"flatValue"`
	// PercentageBPS 是全部平加后累加应用的万分比修正。
	PercentageBPS int32 `json:"percentageBps"`
}

// EquipmentBattleSnapshotEntry 是人物 Battle 创建时冻结的一件已穿戴装备及其可执行资料。
//
// Snapshot 不回查实时 Equipment Catalog Entry；资料随后停用或修改不会改变已经创建的 Battle。
type EquipmentBattleSnapshotEntry struct {
	// InstanceID 与 EquipmentID 分别冻结玩家资产身份和装备资料身份。
	InstanceID  snowflake.ID
	EquipmentID snowflake.ID
	// ItemID 是名称、图标和来源追踪使用的 Item Catalog Entry 身份。
	ItemID snowflake.ID
	// Slot 是创建 Battle 时该实例实际占用的固定 Loadout 槽位。
	Slot EquipmentSlot
	// CatalogVersion 是创建 Battle 时 Equipment Catalog Entry 的乐观版本。
	CatalogVersion int64
	// StatModifiers 是按 StatID 保存的完整属性修正深拷贝。
	StatModifiers []EquipmentStatModifier
	// CompiledRules 是 Equipment Rules 编译器产生的规范 JSON 深拷贝。
	CompiledRules json.RawMessage
}

// EquipmentBattleSnapshot 是未来人物 PvE/PvP 执行器唯一可读取的装备输入。
type EquipmentBattleSnapshot struct {
	// LoadoutVersion 是创建 Battle 时整套 Equipment Loadout 的版本。
	LoadoutVersion int64
	// Entries 按固定槽位排序，避免数据库返回顺序影响快照编码和重放。
	Entries []EquipmentBattleSnapshotEntry
}

// FreezeEquipmentBattleSnapshot 校验并深拷贝人物 Battle 所需的完整装备来源事实。
func FreezeEquipmentBattleSnapshot(loadoutVersion int64, entries []EquipmentBattleSnapshotEntry) (EquipmentBattleSnapshot, error) {
	if loadoutVersion <= 0 {
		return EquipmentBattleSnapshot{}, ErrEquipmentLoadoutConflict
	}
	result := EquipmentBattleSnapshot{LoadoutVersion: loadoutVersion, Entries: make([]EquipmentBattleSnapshotEntry, len(entries))}
	seenSlots := make(map[EquipmentSlot]struct{}, len(entries))
	seenInstances := make(map[snowflake.ID]struct{}, len(entries))
	for index, entry := range entries {
		if !entry.InstanceID.IsValid() || !entry.EquipmentID.IsValid() || !entry.ItemID.IsValid() || entry.CatalogVersion <= 0 || !validEquipmentSlot(entry.Slot) || !json.Valid(entry.CompiledRules) {
			return EquipmentBattleSnapshot{}, ErrEquipmentRulesInvalid
		}
		if _, exists := seenSlots[entry.Slot]; exists {
			return EquipmentBattleSnapshot{}, ErrEquipmentLoadoutConflict
		}
		if _, exists := seenInstances[entry.InstanceID]; exists {
			return EquipmentBattleSnapshot{}, ErrEquipmentLoadoutConflict
		}
		seenSlots[entry.Slot], seenInstances[entry.InstanceID] = struct{}{}, struct{}{}
		compiled, err := CompileEquipmentRules(entry.CompiledRules)
		if err != nil {
			return EquipmentBattleSnapshot{}, err
		}
		result.Entries[index] = entry
		result.Entries[index].CompiledRules = append(json.RawMessage(nil), compiled...)
		result.Entries[index].StatModifiers = append([]EquipmentStatModifier(nil), entry.StatModifiers...)
	}
	sort.Slice(result.Entries, func(left, right int) bool { return result.Entries[left].Slot < result.Entries[right].Slot })
	return result, nil
}

// EquipmentInstance 是玩家读取的一件独立装备资产和必要资料摘要。
type EquipmentInstance struct {
	// ID 是玩家实际拥有的独立 Equipment Instance 身份。
	ID snowflake.ID
	// EquipmentID 与 ItemID 分别指向装备规则资料和展示资料。
	EquipmentID snowflake.ID
	ItemID      snowflake.ID
	// Name 是 Item Catalog Entry 提供的当前简体中文名称。
	Name string
	// SlotType 与 Handedness 是穿戴校验所需的资料槽位事实。
	SlotType   EquipmentSlotType
	Handedness EquipmentHandedness
	// SourceType 是 shop、quest、loot 或 admin 获取来源。
	SourceType string
	// EquippedSlot 非空时表示该实例当前占用的 Loadout 槽位。
	EquippedSlot string
	// Version 是出售等实例终态命令使用的乐观版本。
	Version int64
	// MinimumLevel 是当前资料声明的最低穿戴等级。
	MinimumLevel int32
	// RuleTimings 是规范 Equipment Rules 中存在的执行时机。
	RuleTimings []string
	// AcquiredAt 是该实例归属当前 PlayerCharacter 的 UTC 时间。
	AcquiredAt time.Time
}

// EquipmentInstancePage 是玩家装备实例的一页稳定 ID 升序结果。
type EquipmentInstancePage struct {
	// Items 是本页尚未出售的独立装备实例。
	Items []EquipmentInstance
	// NextCursor 非空时可读取严格位于本页之后的实例。
	NextCursor string
}

// EquipmentLoadoutEntry 是固定槽位与实例的当前关系。
type EquipmentLoadoutEntry struct {
	// Slot 是角色装备配置中的固定闭集槽位。
	Slot EquipmentSlot
	// InstanceID 是当前占用该槽位的独立装备资产。
	InstanceID snowflake.ID
	// EquippedAt 是该实例进入当前槽位的 UTC 时间。
	EquippedAt time.Time
}

// EquipmentLoadout 是角色整套装备的单一版本化视图。
type EquipmentLoadout struct {
	// Version 是整套 Loadout 原子替换使用的单一乐观版本。
	Version int64
	// Entries 保存当前全部非空槽位关系。
	Entries []EquipmentLoadoutEntry
	// UpdatedAt 是最近一次整套替换的 UTC 时间。
	UpdatedAt time.Time
}

// ReplaceEquipmentLoadoutCommand 是整套原子换装命令。
type ReplaceEquipmentLoadoutCommand struct {
	// AccountID 用于解析本次命令的活动 PlayerCharacter。
	AccountID snowflake.ID
	// Entries 是命令提交后的完整最终非空槽位集合。
	Entries []EquipmentLoadoutEntry
	// ExpectedVersion 是调用方读取到的当前 Loadout 版本。
	ExpectedVersion int64
	// IdempotencyKey 在角色和换装操作范围内唯一标识请求。
	IdempotencyKey string
	// Now 是命令统一提交和流水记录使用的 UTC 时间。
	Now time.Time
}

// SellEquipmentCommand 是出售一个未穿戴实例的幂等命令。
type SellEquipmentCommand struct {
	// AccountID 用于解析本次命令的活动 PlayerCharacter。
	AccountID snowflake.ID
	// InstanceID 是待进入出售终态的独立装备资产。
	InstanceID snowflake.ID
	// ExpectedVersion 是调用方读取到的实例乐观版本。
	ExpectedVersion int64
	// IdempotencyKey 在角色和出售操作范围内唯一标识请求。
	IdempotencyKey string
	// Now 是实例终态、钱包和流水共享的 UTC 提交时间。
	Now time.Time
}

// SellEquipmentResult 返回出售操作身份与基础价格。
type SellEquipmentResult struct {
	// OperationID 关联本次出售产生的装备、货币和 Outbox 事实。
	OperationID snowflake.ID
	// CurrencyID 是 Equipment Catalog Entry 声明的入账货币。
	CurrencyID snowflake.ID
	// SellPrice 是本次实际计入钱包的非负资料价格。
	SellPrice int64
	// BalanceAfter 是同一事务提交后的对应钱包余额。
	BalanceAfter int64
}

// EquipmentReader 返回玩家装备实例与整套 Loadout 领域对象。
type EquipmentReader interface {
	GetEquipmentInstance(context.Context, snowflake.ID, snowflake.ID) (EquipmentInstance, error)
	GetEquipmentLoadout(context.Context, snowflake.ID) (EquipmentLoadout, error)
}

// EquipmentQuery 返回玩家装备实例分页投影。
type EquipmentQuery interface {
	ListEquipmentInstances(context.Context, snowflake.ID, int, string) (EquipmentInstancePage, error)
}

// EquipmentRepository 是玩家装备资产原子命令的关系型持久化端口。
type EquipmentRepository interface {
	ReplaceEquipmentLoadout(context.Context, ReplaceEquipmentLoadoutCommand) (EquipmentLoadout, error)
	SellEquipmentInstance(context.Context, SellEquipmentCommand) (SellEquipmentResult, error)
}

// AdminEquipment 是管理端原子维护的完整 Equipment Catalog Entry 聚合。
type AdminEquipment struct {
	// ID 是 Equipment Catalog Entry 身份；创建命令中为空。
	ID snowflake.ID
	// ItemID 是装备使用的展示与获取资料身份。
	ItemID snowflake.ID
	// SellCurrencyID 是实例出售时服务端权威入账的货币身份。
	SellCurrencyID snowflake.ID
	// ItemName 是管理列表从 Item Catalog Entry 读取的显示名称。
	ItemName string
	// SlotType 与 Handedness 定义装备可占用的角色槽位。
	SlotType   EquipmentSlotType
	Handedness EquipmentHandedness
	// MinimumLevel 是角色穿戴该资料实例所需的最低等级。
	MinimumLevel int32
	// SellPrice 是出售未穿戴实例时计入钱包的非负金额。
	SellPrice int64
	// Enabled 控制新的获取与穿戴，不强制卸下既有实例。
	Enabled bool
	// Version 是主资料与全部关系原子保存使用的乐观版本。
	Version int64
	// InstanceCount 是管理诊断使用的已创建实例总数。
	InstanceCount int64
	// ProfessionIDs 是允许穿戴的职业白名单；空集合表示通用。
	ProfessionIDs []snowflake.ID
	// StatModifiers 是按 Game Stat 唯一保存的完整修正关系。
	StatModifiers []AdminEquipmentStatModifier
	// RuleTimings 是当前规范规则文档声明的顶层执行时机。
	RuleTimings []string
}

// AdminEquipmentPage 是装备资料按稳定 ID 升序返回的一页管理结果。
type AdminEquipmentPage struct {
	// Items 是本页完整 Equipment Catalog Entry 聚合。
	Items []AdminEquipment
	// NextCursor 非空时可读取严格位于本页之后的装备资料。
	NextCursor string
}

// EquipmentOption 是管理表单选择启用装备时使用的轻量引用投影。
type EquipmentOption struct {
	// ID 是 Equipment Catalog Entry 的稳定身份。
	ID snowflake.ID
	// ItemName 是关联 Item Catalog Entry 的当前简体中文名称。
	ItemName string
}

// AdminEquipmentStatModifier 是管理端维护的一条 Stat 修正关系。
type AdminEquipmentStatModifier struct {
	// ID 是已持久化关系身份；保存请求中为空。
	ID snowflake.ID
	// StatID 是被修正的 Game Stat 身份。
	StatID snowflake.ID
	// FlatValue 是百分比计算前累加的有符号整数修正。
	FlatValue int64
	// PercentageBPS 是全部平加后应用的万分比修正。
	PercentageBPS int32
}

// SaveEquipmentCommand 原子保存装备主资料和全部关系。
type SaveEquipmentCommand struct {
	// Write 保存管理员、请求和幂等身份。
	Write AdminWriteContext
	// Value 是待创建或完整替换的装备资料聚合。
	Value AdminEquipment
	// ExpectedVersion 是更新要求的当前版本；创建时为零。
	ExpectedVersion int64
}

// AdminEquipmentInstance 是管理端装备实例诊断视图。
type AdminEquipmentInstance struct {
	// ID 是独立 Equipment Instance 身份。
	ID snowflake.ID
	// PlayerCharacterID 是资产当前归属角色。
	PlayerCharacterID snowflake.ID
	// EquipmentID 是实例引用的 Equipment Catalog Entry。
	EquipmentID snowflake.ID
	// SourceReferenceID 是可选支付、奖励、掉落或管理操作身份。
	SourceReferenceID snowflake.ID
	// ItemName 是实例装备资料关联的当前道具名称。
	ItemName string
	// SourceType 是 shop、quest、loot 或 admin 获取来源。
	SourceType string
	// EquippedSlot 非空时是实例当前占用的角色槽位。
	EquippedSlot string
	// Version 是实例出售终态使用的乐观版本。
	Version int64
	// AcquiredAt 是实例归属当前角色的 UTC 时间。
	AcquiredAt time.Time
	// SoldAt 非零时表示实例已经进入不可逆的出售终态。
	SoldAt time.Time
}

// AdminEquipmentInstanceQuery 保存管理实例诊断的筛选与 keyset 分页输入。
type AdminEquipmentInstanceQuery struct {
	// PageSize 是一至一百的请求页大小。
	PageSize int
	// Cursor 是上一页返回的不可解释游标；首页为空。
	Cursor string
	// PlayerCharacterID 与 EquipmentID 是可选精确身份筛选。
	PlayerCharacterID, EquipmentID snowflake.ID
	// Equipped 可选地区分已穿戴和未穿戴实例。
	Equipped *bool
	// SourceType 是 shop、quest、loot 或 admin 的可选获取来源筛选。
	SourceType string
}

// AdminEquipmentInstancePage 是管理实例诊断按获得时间和 ID 倒序返回的一页结果。
type AdminEquipmentInstancePage struct {
	// Items 是本页装备实例诊断事实。
	Items []AdminEquipmentInstance
	// NextCursor 非空时可读取时间更早的下一页实例。
	NextCursor string
}

// AdminEquipmentTransaction 是管理端不可变资产流水视图。
type AdminEquipmentTransaction struct {
	// ID 是不可变资产流水身份。
	ID snowflake.ID
	// OperationID 关联同一获取、换装或出售命令产生的流水。
	OperationID snowflake.ID
	// PlayerCharacterID 与 InstanceID 确定流水涉及的角色资产。
	PlayerCharacterID snowflake.ID
	InstanceID        snowflake.ID
	// Action 是 acquire、equip、unequip 或 sell 闭集动作。
	Action string
	// SourceType 仅在 acquire 动作保存获取来源。
	SourceType string
	// Slot 仅在 equip 或 unequip 动作保存涉及槽位。
	Slot string
	// CreatedAt 是同一操作统一的 UTC 提交时间。
	CreatedAt time.Time
}

// EquipmentTransactionQuery 保存不可变装备流水的筛选与 keyset 分页输入。
type EquipmentTransactionQuery struct {
	// PageSize 是一至一百的请求页大小。
	PageSize int
	// Cursor 是上一页返回的不可解释游标；首页为空。
	Cursor string
	// PlayerCharacterID 与 EquipmentInstanceID 是可选精确身份筛选。
	PlayerCharacterID, EquipmentInstanceID snowflake.ID
	// Action 是 acquire、equip、unequip 或 sell 的可选动作筛选。
	Action string
}

// AdminEquipmentTransactionPage 是装备资产流水按提交时间和 ID 倒序返回的一页结果。
type AdminEquipmentTransactionPage struct {
	// Items 是本页不可变资产流水。
	Items []AdminEquipmentTransaction
	// NextCursor 非空时可读取时间更早的下一页流水。
	NextCursor string
}

// GrantEquipmentCommand 是管理授予一种装备的幂等命令。
type GrantEquipmentCommand struct {
	// Write 保存管理员、请求和幂等身份。
	Write AdminWriteContext
	// PlayerCharacterID 是接收装备实例的角色。
	PlayerCharacterID snowflake.ID
	// EquipmentID 是待实例化的已启用装备资料。
	EquipmentID snowflake.ID
	// Quantity 是本次创建的独立实例数量，范围为一至一百。
	Quantity int32
	// Reason 只进入管理审计，不进入公开 Outbox。
	Reason string
	// Now 是全部实例、流水、审计和 Outbox 共享的 UTC 时间。
	Now time.Time
}

// GrantEquipmentResult 返回共同操作身份及稳定实例身份。
type GrantEquipmentResult struct {
	// OperationID 关联本次批量授予的实例、流水、审计和 Outbox。
	OperationID snowflake.ID
	// InstanceIDs 按创建顺序返回本次产生的稳定资产身份。
	InstanceIDs []snowflake.ID
}

// ValidateEquipmentLoadout 只校验命令提交后的完整最终状态，因此交换槽位不依赖中间写入顺序。
func ValidateEquipmentLoadout(level int32, professions []snowflake.ID, entries []EquipmentLoadoutCandidate) error {
	professionSet := make(map[snowflake.ID]struct{}, len(professions))
	for _, id := range professions {
		professionSet[id] = struct{}{}
	}
	seenSlots := make(map[EquipmentSlot]struct{}, len(entries))
	seenInstances := make(map[snowflake.ID]struct{}, len(entries))
	twoHanded := false
	offHandPresent := false
	for _, entry := range entries {
		if !entry.InstanceID.IsValid() || !EquipmentSlotMatches(entry.Slot, entry.SlotType) {
			return ErrEquipmentSlotMismatch
		}
		if _, exists := seenSlots[entry.Slot]; exists {
			return ErrEquipmentLoadoutConflict
		}
		if _, exists := seenInstances[entry.InstanceID]; exists {
			return ErrEquipmentLoadoutConflict
		}
		seenSlots[entry.Slot], seenInstances[entry.InstanceID] = struct{}{}, struct{}{}
		if level < entry.MinimumLevel || !matchesProfession(professionSet, entry.ProfessionIDs) {
			return ErrEquipmentRequirementNotMet
		}
		if entry.Slot == EquipmentSlotMainHand && entry.Handedness == EquipmentHandednessTwoHanded {
			twoHanded = true
		}
		if entry.Slot == EquipmentSlotOffHand {
			offHandPresent = true
		}
	}
	if twoHanded && offHandPresent {
		return ErrEquipmentTwoHandedConflict
	}
	return nil
}

// ApplyEquipmentStatModifiers 按“全部平加后汇总百分比”的稳定公式计算非负最终值。
func ApplyEquipmentStatModifiers(baseValue, minimumValue int64, modifiers []EquipmentStatModifier) (int64, error) {
	flatTotal, bpsTotal := int64(0), int64(0)
	for _, modifier := range modifiers {
		if modifier.PercentageBPS < -10000 || modifier.PercentageBPS > 100000 {
			return 0, ErrEquipmentStatModifierInvalid
		}
		flatTotal += modifier.FlatValue
		bpsTotal += int64(modifier.PercentageBPS)
	}
	if bpsTotal < -10000 || bpsTotal > 100000 {
		return 0, ErrEquipmentStatModifierInvalid
	}
	value := int64(math.Floor(float64(baseValue+flatTotal) * float64(10000+bpsTotal) / 10000))
	if value < minimumValue {
		value = minimumValue
	}
	if value < 0 {
		value = 0
	}
	return value, nil
}

// CompileEquipmentRules 校验顶层执行时机闭集并输出键顺序稳定、无未知字段的规范 JSON。
// 当前人物 Battle 执行器尚未接入具体原语，因此每个时机只允许空数组；非空规则资料不能启用。
func CompileEquipmentRules(source []byte) (json.RawMessage, error) {
	decoder := json.NewDecoder(bytes.NewReader(source))
	decoder.DisallowUnknownFields()
	var document struct {
		OnBattleStart []json.RawMessage `json:"onBattleStart,omitempty"`
		BeforeDamage  []json.RawMessage `json:"beforeDamage,omitempty"`
		AfterDamage   []json.RawMessage `json:"afterDamage,omitempty"`
		OnTurnEnd     []json.RawMessage `json:"onTurnEnd,omitempty"`
		OnDefeat      []json.RawMessage `json:"onDefeat,omitempty"`
	}
	if err := decoder.Decode(&document); err != nil {
		return nil, ErrEquipmentRulesInvalid
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return nil, ErrEquipmentRulesInvalid
	}
	if len(document.OnBattleStart)+len(document.BeforeDamage)+len(document.AfterDamage)+len(document.OnTurnEnd)+len(document.OnDefeat) > 0 {
		return nil, ErrEquipmentRulesInvalid
	}
	var raw map[string]json.RawMessage
	if err := json.Unmarshal(source, &raw); err != nil || raw == nil {
		return nil, ErrEquipmentRulesInvalid
	}
	allowed := map[string]struct{}{"onBattleStart": {}, "beforeDamage": {}, "afterDamage": {}, "onTurnEnd": {}, "onDefeat": {}}
	keys := make([]string, 0, len(raw))
	for key := range raw {
		if _, ok := allowed[key]; !ok {
			return nil, ErrEquipmentRulesInvalid
		}
		var values []json.RawMessage
		if err := json.Unmarshal(raw[key], &values); err != nil || values == nil {
			return nil, ErrEquipmentRulesInvalid
		}
		keys = append(keys, key)
	}
	sort.Strings(keys)
	buffer := bytes.NewBufferString("{")
	for index, key := range keys {
		if index > 0 {
			buffer.WriteByte(',')
		}
		encodedKey, _ := json.Marshal(key)
		buffer.Write(encodedKey)
		buffer.WriteByte(':')
		buffer.Write(raw[key])
	}
	buffer.WriteByte('}')
	return json.RawMessage(buffer.Bytes()), nil
}

// EquipmentSlotMatches 判断资料槽位类型是否可放入指定 Loadout 槽位。
func EquipmentSlotMatches(slot EquipmentSlot, slotType EquipmentSlotType) bool {
	switch slot {
	case EquipmentSlotMainHand:
		return slotType == EquipmentSlotTypeMainHand
	case EquipmentSlotOffHand:
		return slotType == EquipmentSlotTypeOffHand
	case EquipmentSlotHead:
		return slotType == EquipmentSlotTypeHead
	case EquipmentSlotBody:
		return slotType == EquipmentSlotTypeBody
	case EquipmentSlotHands:
		return slotType == EquipmentSlotTypeHands
	case EquipmentSlotFeet:
		return slotType == EquipmentSlotTypeFeet
	case EquipmentSlotAccessory1, EquipmentSlotAccessory2:
		return slotType == EquipmentSlotTypeAccessory
	default:
		return false
	}
}

func validEquipmentSlot(slot EquipmentSlot) bool {
	return EquipmentSlotMatches(slot, EquipmentSlotType(slot)) || slot == EquipmentSlotAccessory1 || slot == EquipmentSlotAccessory2
}

func matchesProfession(owned map[snowflake.ID]struct{}, allowed []snowflake.ID) bool {
	if len(allowed) == 0 {
		return true
	}
	for _, id := range allowed {
		if _, ok := owned[id]; ok {
			return true
		}
	}
	return false
}
