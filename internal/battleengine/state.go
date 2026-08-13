package battleengine

import (
	"errors"
	"fmt"
	"sort"
	"strings"
)

const (
	// MaximumMembersPerSide 是一方带入纯战斗引擎的最大成员数。
	MaximumMembersPerSide = 6
	// MaximumSkillsPerMember 是一名战斗成员最多拥有的技能槽数量。
	MaximumSkillsPerMember = 4
)

// SkillPosition 是技能在一名成员冻结技能列表中的稳定位置，取值为 1 至 4。
type SkillPosition uint8

// Valid 报告技能位置是否位于一名成员可携带的合法技能槽范围内。
func (position SkillPosition) Valid() bool {
	return position >= 1 && position <= MaximumSkillsPerMember
}

var (
	// ErrInvalidInitialState 表示战斗初始快照违反格式、站位或成员数据不变量。
	ErrInvalidInitialState = errors.New("无效的战斗初始状态")
)

// Side 是一场双边战斗中的稳定阵营位置。
//
// Side 只表示 Battle 内的位置，不携带账号、玩家角色或队伍身份。
type Side uint8

const (
	// SideOne 是 Battle 创建时固定的第一方。
	SideOne Side = 1
	// SideTwo 是 Battle 创建时固定的第二方。
	SideTwo Side = 2
)

// Valid 报告阵营位置是否属于当前双边战斗模型。
func (side Side) Valid() bool {
	return side == SideOne || side == SideTwo
}

// MemberPosition 是成员在本场 Battle 队伍快照中的稳定位置，取值为 1 至 6。
//
// 成员换上或换下时该位置不会变化，因此它可以安全进入命令、事件和 Turn Record。
type MemberPosition uint8

// Valid 报告成员位置是否位于一方队伍的合法范围内。
func (position MemberPosition) Valid() bool {
	return position >= 1 && position <= MaximumMembersPerSide
}

// SlotPosition 是一方当前场上槽位的稳定位置，单打仅使用 1，双打使用 1 和 2。
type SlotPosition uint8

// SlotRef 唯一定位一方的场上槽位。
type SlotRef struct {
	// Side 是槽位所属的稳定阵营位置。
	Side Side `json:"side"`
	// Position 是阵营内从 1 开始的场上槽位位置。
	Position SlotPosition `json:"slotPosition"`
}

// DamageClass 是技能采用的伤害结算类别。
type DamageClass string

const (
	// DamageClassPhysical 表示技能使用攻击和防御能力进行伤害结算。
	DamageClassPhysical DamageClass = "physical"
	// DamageClassSpecial 表示技能使用特攻和特防能力进行伤害结算。
	DamageClassSpecial DamageClass = "special"
	// DamageClassStatus 表示技能不进入普通伤害公式。
	DamageClassStatus DamageClass = "status"
)

// Valid 报告伤害类别是否为引擎已知的稳定值。
func (class DamageClass) Valid() bool {
	return class == DamageClassPhysical || class == DamageClassSpecial || class == DamageClassStatus
}

// SkillDamageMode 是技能命中后计算目标生命损失的强类型规则。
//
// Formula 使用普通物理或特殊伤害公式；其余模式均为直接伤害，不读取攻击、防御、属性克制、要害或伤害
// 浮动随机数。运行时资料编译器只接受本枚举的稳定值，禁止把管理端自由文本解释为可执行规则。
type SkillDamageMode string

const (
	// SkillDamageModeFormula 表示使用普通物理或特殊伤害公式。
	SkillDamageModeFormula SkillDamageMode = "formula"
	// SkillDamageModeFixedAmount 表示造成固定的生命损失。
	SkillDamageModeFixedAmount SkillDamageMode = "fixedAmount"
	// SkillDamageModeUserLevel 表示造成等于使用者等级的固定生命损失。
	SkillDamageModeUserLevel SkillDamageMode = "userLevel"
	// SkillDamageModeTargetCurrentHPFraction 表示按目标当前生命值的指定分数造成伤害。
	SkillDamageModeTargetCurrentHPFraction SkillDamageMode = "targetCurrentHPFraction"
	// SkillDamageModeTargetCurrentHPMinusUserCurrentHP 表示造成目标当前生命减使用者当前生命的差值伤害。
	SkillDamageModeTargetCurrentHPMinusUserCurrentHP SkillDamageMode = "targetCurrentHPMinusUserCurrentHP"
	// SkillDamageModeUserCurrentHPAndUserFaints 表示以使用者当前生命作为伤害并使其倒下。
	SkillDamageModeUserCurrentHPAndUserFaints SkillDamageMode = "userCurrentHPAndUserFaints"
	// SkillDamageModeAverageUserAndTargetCurrentHP 表示把使用者与目标当前生命分别重设为双方当前生命的平均值。
	// 它不计作普通伤害、回复或生命交换，且只能由变化技能使用。
	SkillDamageModeAverageUserAndTargetCurrentHP SkillDamageMode = "averageUserAndTargetCurrentHP"
	// SkillDamageModeOneHitKnockOut 表示使用专用等级与命中率规则；命中后直接造成目标当前生命的伤害。
	SkillDamageModeOneHitKnockOut SkillDamageMode = "oneHitKnockOut"
	// SkillDamageModeReceivedDamage 表示读取使用者本回合最后一段合格的已受伤害，并按倍率返还给原攻击者。
	SkillDamageModeReceivedDamage SkillDamageMode = "receivedDamage"
)

// Valid 报告伤害模式是否为纯战斗引擎支持的稳定值。
func (mode SkillDamageMode) Valid() bool {
	return mode == SkillDamageModeFormula || mode == SkillDamageModeFixedAmount ||
		mode == SkillDamageModeUserLevel || mode == SkillDamageModeTargetCurrentHPFraction ||
		mode == SkillDamageModeTargetCurrentHPMinusUserCurrentHP || mode == SkillDamageModeUserCurrentHPAndUserFaints ||
		mode == SkillDamageModeAverageUserAndTargetCurrentHP || mode == SkillDamageModeOneHitKnockOut ||
		mode == SkillDamageModeReceivedDamage
}

// MajorStatus 是一名成员同一时间最多持有一个的主要异常状态。
type MajorStatus string

const (
	// MajorStatusBurn 表示灼伤状态。
	MajorStatusBurn MajorStatus = "burn"
	// MajorStatusPoison 表示每回合造成固定比例伤害的普通中毒状态。
	MajorStatusPoison MajorStatus = "poison"
	// MajorStatusBadPoison 表示伤害随连续在场回合增长的剧毒状态。
	MajorStatusBadPoison MajorStatus = "badPoison"
	// MajorStatusParalysis 表示速度降低且行动前可能无法行动的麻痹状态。
	MajorStatusParalysis MajorStatus = "paralysis"
	// MajorStatusSleep 表示在剩余阻止次数归零前无法使用技能的睡眠状态。
	MajorStatusSleep MajorStatus = "sleep"
	// MajorStatusFreeze 表示行动前可能自然解冻、否则无法使用技能的冰冻状态。
	MajorStatusFreeze MajorStatus = "freeze"
)

// Valid 报告主要异常状态是否为引擎已知的稳定值。
func (status MajorStatus) Valid() bool {
	return status == MajorStatusBurn || status == MajorStatusPoison || status == MajorStatusBadPoison ||
		status == MajorStatusParalysis || status == MajorStatusSleep || status == MajorStatusFreeze
}

// EffectTarget 是技能结构化效果相对于行动者的目标种类。
type EffectTarget string

const (
	// EffectTargetSelected 表示效果作用于玩家选择的目标槽位当前成员。
	EffectTargetSelected EffectTarget = "selectedTarget"
	// EffectTargetUser 表示效果作用于技能使用者自身。
	EffectTargetUser EffectTarget = "user"
)

// Valid 报告效果目标是否为引擎已知的稳定值。
func (target EffectTarget) Valid() bool {
	return target == EffectTargetSelected || target == EffectTargetUser
}

// SkillTargetScope 是一次技能行动在当前场上站位中解析实际目标的范围。
//
// 它与 EffectTarget 的职责不同：SkillTargetScope 决定一项技能本身会尝试影响哪些场上成员，
// EffectTarget 只决定某一项附加效果相对于每个已解析目标落在该目标还是使用者。范围目标始终在
// 行动实际执行时重新读取当前站位，因而不会把先前换人前的成员快照错误地当成目标。
type SkillTargetScope string

const (
	// SkillTargetScopeSelectedTarget 表示技能只影响请求中指定的一个对方场上槽位。
	SkillTargetScopeSelectedTarget SkillTargetScope = "selectedTarget"
	// SkillTargetScopeSelf 表示技能只影响使用者自身。
	SkillTargetScopeSelf SkillTargetScope = "self"
	// SkillTargetScopeUserSideActive 表示技能影响使用者和同侧全部仍在场的成员。
	SkillTargetScopeUserSideActive SkillTargetScope = "userSideActive"
	// SkillTargetScopeAllAdjacentOpponents 表示技能影响对侧全部仍在场的成员。
	SkillTargetScopeAllAdjacentOpponents SkillTargetScope = "allAdjacentOpponents"
	// SkillTargetScopeAllAdjacentParticipants 表示技能影响除使用者外全部仍在场的成员。
	SkillTargetScopeAllAdjacentParticipants SkillTargetScope = "allAdjacentParticipants"
	// SkillTargetScopeRandomAdjacentOpponent 表示技能在对侧仍在场成员中随机选择一个目标。
	SkillTargetScopeRandomAdjacentOpponent SkillTargetScope = "randomAdjacentOpponent"
)

// Valid 报告技能目标范围是否是当前纯战斗引擎能够解释的稳定值。
func (scope SkillTargetScope) Valid() bool {
	return scope == SkillTargetScopeSelectedTarget || scope == SkillTargetScopeSelf ||
		scope == SkillTargetScopeUserSideActive || scope == SkillTargetScopeAllAdjacentOpponents ||
		scope == SkillTargetScopeAllAdjacentParticipants || scope == SkillTargetScopeRandomAdjacentOpponent
}

// MajorStatusApplication 描述技能命中后尝试施加的一项主要异常效果。
type MajorStatusApplication struct {
	// Status 是效果尝试施加的主要异常状态。
	Status MajorStatus `json:"status"`
	// Target 是 selectedTarget 或 user 等相对于行动者的稳定目标种类。
	Target EffectTarget `json:"target"`
	// ChancePercent 是 0 至 100 的成功概率；100 不消费额外随机数。
	ChancePercent uint8 `json:"chancePercent"`
}

// VolatileStatus 是不占用主要异常槽、但会在战斗过程中自行过期的强类型状态。
//
// 该枚举是资料编译器和纯引擎之间的封闭契约。管理员资料只能选择这些稳定代码，不能把技能名称、
// 自由文本效果或脚本送入运行时解释器。
type VolatileStatus string

const (
	// VolatileStatusConfusion 表示成员行动前可能伤害自身的混乱状态。
	VolatileStatusConfusion VolatileStatus = "confusion"
	// VolatileStatusBinding 表示成员被束缚、持续受到回合末伤害且不能主动换人。
	VolatileStatusBinding VolatileStatus = "binding"
	// VolatileStatusTaunt 表示成员暂时不能使用变化技能。
	VolatileStatusTaunt VolatileStatus = "taunt"
	// VolatileStatusCharging 表示成员已经进入蓄力阶段，下一回合必须使用同一技能完成攻击。
	VolatileStatusCharging VolatileStatus = "charging"
	// VolatileStatusLockedMove 表示成员在若干回合内必须重复使用同一技能。
	VolatileStatusLockedMove VolatileStatus = "lockedMove"
	// VolatileStatusDisable 表示成员的一项最近使用技能在若干回合内不可使用。
	VolatileStatusDisable VolatileStatus = "disable"
	// VolatileStatusProtection 表示成员在本回合剩余行动中阻止对方技能直接影响自身的保护。
	VolatileStatusProtection VolatileStatus = "protection"
	// VolatileStatusSubstitute 表示成员已支付本体生命建立的独立替身生命值。
	VolatileStatusSubstitute VolatileStatus = "substitute"
)

// Valid 报告易变状态是否为当前引擎可执行的稳定值。
func (status VolatileStatus) Valid() bool {
	return status == VolatileStatusConfusion || status == VolatileStatusBinding ||
		status == VolatileStatusTaunt || status == VolatileStatusCharging ||
		status == VolatileStatusLockedMove || status == VolatileStatusDisable ||
		status == VolatileStatusProtection || status == VolatileStatusSubstitute
}

// VolatileStatusApplication 描述技能成功执行后尝试写入的一项易变状态。
//
// MinTurns 与 MaxTurns 以完整行动/回合阶段计数：两者相等时不消费持续时间随机数；不同值时引擎从
// 闭区间均匀选择。charging 固定使用一回合准备期，lockedMove 的时长包含首次成功使用该技能的回合。
type VolatileStatusApplication struct {
	// Status 是要写入的封闭易变状态代码。
	Status VolatileStatus `json:"status"`
	// Target 是 selectedTarget 或 user 等相对于技能使用者的稳定落点。
	Target EffectTarget `json:"target"`
	// ChancePercent 是本项状态独立触发的概率；100 表示必定且不消费概率随机数。
	ChancePercent uint8 `json:"chancePercent"`
	// MinTurns 是状态持续时间的下界，必须为正数。
	MinTurns uint8 `json:"minTurns"`
	// MaxTurns 是状态持续时间的上界，必须不小于 MinTurns。
	MaxTurns uint8 `json:"maxTurns"`
	// SubstituteCostNumerator 是建立替身时支付使用者最大生命值的分子；仅 substitute 使用，
	// 其它易变状态必须为 0。
	SubstituteCostNumerator uint8 `json:"substituteCostNumerator"`
	// SubstituteCostDenominator 是建立替身时支付使用者最大生命值的分母；仅 substitute 使用，
	// 必须不小于分子，实际费用按向下取整且至少为 1 点计算。
	SubstituteCostDenominator uint8 `json:"substituteCostDenominator"`
}

// FormatSnapshot 是战斗开始时冻结的最小赛制快照。
type FormatSnapshot struct {
	// Code 是实时资料中 BattleFormat 的稳定代码。
	Code string `json:"code"`
	// ActiveSlotsPerSide 是每方必须同时占用的场上槽位数量。
	ActiveSlotsPerSide SlotPosition `json:"activeSlotsPerSide"`
	// TeamSize 是每方实际带入引擎的成员上限，取值为 1 至 6。
	TeamSize uint8 `json:"teamSize"`
	// MaxTurns 是赛制允许的最大完整回合数；0 表示不设置引擎级回合上限。
	MaxTurns uint32 `json:"maxTurns"`
}

// RuleSnapshot 标识战斗开始时冻结的规则契约版本。
//
// 它只保存引擎已经理解的强类型规则，而不携带管理端的泛型效果 JSON。SchemaVersion 使离线重放可以
// 在读取任何规则字段前拒绝未知结构；每个字段都由 Battle 层在对局开始时从已注册效果编译得到。
type RuleSnapshot struct {
	// SchemaVersion 是规则快照 JSON 结构的版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// ElementIDs 按资料稳定 code 保存主要异常等规则需要识别的属性 Identifier。
	ElementIDs map[string]Identifier `json:"elementIds"`
	// ElementEffectiveness 保存非中性的攻击属性与防守属性克制倍率。
	ElementEffectiveness []ElementEffectiveness `json:"elementEffectiveness"`
	// UniqueHeldItemClause 表示同一方参战 Team 不允许存在重复的非空持有道具。
	UniqueHeldItemClause bool `json:"uniqueHeldItemClause"`
	// UniqueSpeciesClause 表示同一方参战 Team 不允许存在重复的精灵种类。
	UniqueSpeciesClause bool `json:"uniqueSpeciesClause"`
	// StableCodeRestrictions 保存按资料 Stable Code 判定的允许或禁止名单。
	StableCodeRestrictions []StableCodeRestriction `json:"stableCodeRestrictions"`
	// NormalizedLevel 是特殊机制冻结的统一等级；0 表示本场不启用等级规范化机制。
	NormalizedLevel uint8 `json:"normalizedLevel"`
	// TerastallizationEnabled 表示当前赛制已冻结允许太晶化。
	// 它来自 BattleFormat 引用的显式特殊机制，而不是客户端开关、精灵属性或任何展示代码；未启用时所有
	// `UseSkillAction.Terastallize` 请求都必须在命令校验阶段被拒绝。
	TerastallizationEnabled bool `json:"terastallizationEnabled"`
}

// StableCodeRestriction 是战斗引擎能够执行的一条资料 Stable Code 名单限制。
//
// 它来自管理端的 battle.restriction.stable-code-list 效果，但使用明确字段而非任意 JSON，避免引擎
// 在运行时解释资料定义。当前 ResourceType 仅支持 creature、ability、item 和 skill；Mode 仅支持 allow
// 与 deny。
type StableCodeRestriction struct {
	// Mode 是名单的匹配方式：allow 表示仅允许名单内编码，deny 表示禁止名单内编码。
	Mode string `json:"mode"`
	// ResourceType 是需要检查的 Team 成员引用资料类型。
	ResourceType string `json:"resourceType"`
	// StableCodes 是经过排序且不重复的目标资料 Stable Code。
	StableCodes []string `json:"stableCodes"`
}

// ElementEffectiveness 使用整数分数描述一种攻击属性对一种防守属性的倍率。
//
// 未列出的组合按 1/1 中性倍率处理；双属性目标会按列表中两项倍率继续相乘。
type ElementEffectiveness struct {
	// AttackElementID 是发起伤害的技能属性稳定 Identifier。
	AttackElementID Identifier `json:"attackElementId"`
	// DefenseElementID 是承受伤害的目标属性稳定 Identifier。
	DefenseElementID Identifier `json:"defenseElementId"`
	// Numerator 是倍率分子；0 明确表示该属性组合完全免疫。
	Numerator uint16 `json:"numerator"`
	// Denominator 是倍率分母，必须大于 0。
	Denominator uint16 `json:"denominator"`
}

// StatBlock 保存普通伤害公式和行动排序需要的五项最终能力值。
type StatBlock struct {
	// Attack 是成员的物理攻击能力值。
	Attack uint32 `json:"attack"`
	// Defense 是成员的物理防御能力值。
	Defense uint32 `json:"defense"`
	// SpecialAttack 是成员的特殊攻击能力值。
	SpecialAttack uint32 `json:"specialAttack"`
	// SpecialDefense 是成员的特殊防御能力值。
	SpecialDefense uint32 `json:"specialDefense"`
	// Speed 是成员的基础速度能力值。
	Speed uint32 `json:"speed"`
}

// Stat 是战斗中允许产生临时能力阶级变化的稳定能力项。
type Stat string

const (
	// StatAttack 表示物理攻击能力。
	StatAttack Stat = "attack"
	// StatDefense 表示物理防御能力。
	StatDefense Stat = "defense"
	// StatSpecialAttack 表示特殊攻击能力。
	StatSpecialAttack Stat = "specialAttack"
	// StatSpecialDefense 表示特殊防御能力。
	StatSpecialDefense Stat = "specialDefense"
	// StatSpeed 表示行动排序使用的速度能力。
	StatSpeed Stat = "speed"
	// StatAccuracy 表示技能命中判定中的使用者命中能力。
	StatAccuracy Stat = "accuracy"
	// StatEvasion 表示技能命中判定中的目标闪避能力。
	StatEvasion Stat = "evasion"
)

// Valid 报告能力项是否为引擎支持的稳定值。
func (stat Stat) Valid() bool {
	return stat == StatAttack || stat == StatDefense || stat == StatSpecialAttack ||
		stat == StatSpecialDefense || stat == StatSpeed || stat == StatAccuracy || stat == StatEvasion
}

// StatStageEffect 描述技能命中后按概率增减使用者或目标能力阶级的规则片段。
type StatStageEffect struct {
	// Stat 是本项效果修改的能力项。
	Stat Stat `json:"stat"`
	// Target 是效果相对于技能使用者的实际落点。
	Target EffectTarget `json:"target"`
	// StageDelta 是本次尝试叠加的非零阶级变化，取值为 -6 至 6。
	StageDelta int8 `json:"stageDelta"`
	// ChancePercent 是效果独立触发的百分比；0 表示声明但不触发。
	ChancePercent uint8 `json:"chancePercent"`
}

// SkillSnapshot 是一个成员技能槽在战斗开始时冻结的基础执行数据。
type SkillSnapshot struct {
	// Position 是技能在成员技能列表中的稳定位置，取值为 1 至 4。
	Position SkillPosition `json:"skillPosition"`
	// SkillID 是技能在实时资料中的稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Name 是事件和管理调试使用的冻结技能名称。
	Name string `json:"name"`
	// ElementID 是技能的基础属性稳定 Identifier。
	ElementID Identifier `json:"elementId"`
	// WeakenedByGrassyTerrain 表示该技能带有会被青草场地削弱的震动类地面效果标记。引擎只在
	// 青草场地生效、目标接地且技能进入普通伤害公式时读取该冻结事实；不通过技能名称或属性 Identifier 推断。
	WeakenedByGrassyTerrain bool `json:"weakenedByGrassyTerrain"`
	// WeatherAccuracyOverrides 是技能在指定普通天气下替换基础命中率的封闭覆盖集合。0 命中率表示必中；
	// 未匹配天气时才读取 Accuracy，不能由天气名称、技能名称或效果文本隐式推断。
	WeatherAccuracyOverrides []WeatherAccuracyOverride `json:"weatherAccuracyOverrides"`
	// WeatherElementOverrides 是技能在指定普通天气下替换基础属性的封闭覆盖集合。匹配后属性相性、同属性
	// 加成、天气与场地伤害修正及一击必杀同属性规则均使用覆盖值；未匹配天气时回退 ElementID。
	WeatherElementOverrides []WeatherElementOverride `json:"weatherElementOverrides"`
	// WeatherPowerMultipliers 是技能在指定普通天气下调整普通伤害基础威力的独立分数集合。它在动态威力之后、
	// 属性相性和天气火水修正之前生效；未匹配天气时严格使用一倍，不能与属性或命中覆盖共用泛型效果结构。
	WeatherPowerMultipliers []WeatherPowerMultiplier `json:"weatherPowerMultipliers"`
	// ChargeSkippedWeathers 是当前技能可在指定普通天气下跳过首次蓄力等待的封闭天气集合。它只对声明 charging
	// 易变控制规则的技能有效；已经开始的蓄力仍由成员运行态强制在下一次行动完成，不能被后续天气变化取消。
	ChargeSkippedWeathers []WeatherKind `json:"chargeSkippedWeathers"`
	// RechargesAfterUse 表示该技能成功扣除任一目标本体生命后，使用者下一次技能行动前必须休整一次。
	// 未命中、保护、属性免疫和替身承伤都不满足本规则；休整是成员运行态而不是易变状态资料项，因此不能由
	// 技能名称、威力或展示文本推断。
	RechargesAfterUse bool `json:"rechargesAfterUse"`
	// LocksAccuracyOnTarget 表示该技能命中后使使用者在下一回合内锁定当前具体目标的命中。
	// 它只能用于单体目标变化技能；锁定不能绕过保护、一击必杀等级限制或属性免疫等命中前规则。
	LocksAccuracyOnTarget bool `json:"locksAccuracyOnTarget"`
	// MakesContact 表示该技能在没有其它运行时规则改写时属于接触类技能。
	// 这是保护穿透与接触反制共享的冻结资料事实；不能从伤害类别、技能名称或说明文本推断。
	MakesContact bool `json:"makesContact"`
	// PunchBased 表示该技能是否属于拳击类技能，供持有道具和特性在威力或有效接触阶段读取。
	// 它独立于 MakesContact：拳击标签不自动意味着接触，运行时接触仍由有效接触规则统一判定。
	PunchBased bool `json:"punchBased"`
	// SlicingBased 表示该技能是否属于切割类技能，供攻击方特性在普通直接伤害最终倍率阶段读取。
	// 它不隐含接触、击中要害或任何其它技能标签。
	SlicingBased bool `json:"slicingBased"`
	// SoundBased 表示该技能是否属于声音类技能，供攻击和防守特性的最终伤害倍率读取。
	// 声音免疫、替身穿透等其它规则必须继续使用自己的独立事实。
	SoundBased bool `json:"soundBased"`
	// PulseBased 表示该技能是否属于波动类技能，供攻击方特性读取；它不改变技能属性或回复语义。
	PulseBased bool `json:"pulseBased"`
	// BiteBased 表示该技能是否属于啃咬类技能，供攻击方特性读取；它不自动附加畏缩或接触。
	BiteBased bool `json:"biteBased"`
	// PowderBased 表示该技能是否属于粉末或孢子类技能，供持有道具和特性在命中前的免疫 gate 读取。
	// 它是冻结资料标签，不能从技能名称、伤害类别或主要异常效果推断。
	PowderBased bool `json:"powderBased"`
	// DamageClass 决定技能进入物理、特殊或变化结算分支。
	DamageClass DamageClass `json:"damageClass"`
	// TargetScope 是本次技能行动解析实际目标的冻结范围；运行时资料编译器必须写入显式值。
	TargetScope SkillTargetScope `json:"targetScope"`
	// Power 是普通伤害技能的基础威力；0 表示不使用普通威力公式。
	Power uint16 `json:"power"`
	// DynamicPower 是普通公式前按当前使用者和实际目标重新计算基础威力的可选强类型规则。
	// 空规则时引擎读取 Power；非空规则不依赖技能名称或自由文本，且会在创建 State 时完成参数校验。
	DynamicPower DynamicPowerRule `json:"dynamicPower"`
	// DamageMode 是技能命中后采用的伤害模型；规则编译器始终写入显式值。
	DamageMode SkillDamageMode `json:"damageMode"`
	// DamageAmount 是 fixedAmount 模式未经生命上限夹取的正固定伤害值；其它模式必须为 0。
	DamageAmount uint32 `json:"damageAmount"`
	// DamageNumerator 是 targetCurrentHPFraction 模式使用的正分子；其它模式必须为 0。
	DamageNumerator uint16 `json:"damageNumerator"`
	// DamageDenominator 是 targetCurrentHPFraction 模式使用的正分母；其它模式必须为 0。
	DamageDenominator uint16 `json:"damageDenominator"`
	// MinimumDamage 是比例伤害向下取整后仍应保持的最小正伤害；其它模式必须为 0。
	MinimumDamage uint32 `json:"minimumDamage"`
	// OneHitKnockOutBaseAccuracy 是 oneHitKnockOut 模式的基础命中率，取值 1 至 100；其它模式必须为 0。
	// 一击必杀在本值上叠加双方等级差，且不读取普通 Accuracy 字段或命中/闪避能力阶级。
	OneHitKnockOutBaseAccuracy uint8 `json:"oneHitKnockOutBaseAccuracy"`
	// OneHitKnockOutSameElementUserBaseAccuracy 是使用者拥有本次技能属性时替换基础命中率的可选值。
	// 0 表示没有同属性使用者例外；该字段只允许 oneHitKnockOut 模式使用。
	OneHitKnockOutSameElementUserBaseAccuracy uint8 `json:"oneHitKnockOutSameElementUserBaseAccuracy"`
	// OneHitKnockOutBlocksSameElementTarget 表示拥有本次技能属性的目标会在命中随机数之前阻止一击必杀。
	OneHitKnockOutBlocksSameElementTarget bool `json:"oneHitKnockOutBlocksSameElementTarget"`
	// ReceivedDamageNumerator 是 receivedDamage 模式把最近一段合格已受伤害放大的正分子；其它模式必须为 0。
	ReceivedDamageNumerator uint16 `json:"receivedDamageNumerator"`
	// ReceivedDamageDenominator 是 receivedDamage 模式放大倍率的正分母；其它模式必须为 0。
	ReceivedDamageDenominator uint16 `json:"receivedDamageDenominator"`
	// ReceivedDamageAcceptsPhysical 表示伤害记忆可以读取物理伤害来源；至少一个可接受类别必须为 true。
	ReceivedDamageAcceptsPhysical bool `json:"receivedDamageAcceptsPhysical"`
	// ReceivedDamageAcceptsSpecial 表示伤害记忆可以读取特殊伤害来源；至少一个可接受类别必须为 true。
	ReceivedDamageAcceptsSpecial bool `json:"receivedDamageAcceptsSpecial"`
	// ReceivedDamageIgnoreNonImmuneElementEffectiveness 表示伤害记忆返还伤害只保留完全免疫判断，
	// 不重复应用非零属性克制倍率。当前纯引擎尚未接入直接伤害属性倍率，但快照必须保留此规则事实。
	ReceivedDamageIgnoreNonImmuneElementEffectiveness bool `json:"receivedDamageIgnoreNonImmuneElementEffectiveness"`
	// Accuracy 是 1 至 100 的基础命中率；0 表示该技能基础必中。
	Accuracy uint8 `json:"accuracy"`
	// Priority 是技能参与行动排序的基础优先度。
	Priority int8 `json:"priority"`
	// RemainingPP 是当前剩余可使用次数。
	RemainingPP uint8 `json:"remainingPp"`
	// MaxPP 是本场战斗冻结的最大可使用次数。
	MaxPP uint8 `json:"maxPp"`
	// StatusApplications 是技能命中后按声明顺序尝试施加的主要异常效果。
	StatusApplications []MajorStatusApplication `json:"statusApplications"`
	// CuresUserSideMajorStatuses 表示技能成功后清除使用者同侧整支队伍的主要异常状态，包含后备成员。
	// 它与仅清除当前上场成员、仅清除使用者自己的两个字段独立保存，避免资料编译时扩大治疗范围。
	CuresUserSideMajorStatuses bool `json:"curesUserSideMajorStatuses"`
	// CuresUserMajorStatus 表示技能成功后仅清除使用者自己的主要异常状态。
	CuresUserMajorStatus bool `json:"curesUserMajorStatus"`
	// CuresUserSideActiveMajorStatuses 表示技能成功后仅清除使用者同侧当前上场成员的主要异常状态。
	// 后备成员不属于该效果范围；它不能用整队清除或自我清除的语义替代。
	CuresUserSideActiveMajorStatuses bool `json:"curesUserSideActiveMajorStatuses"`
	// StatStageEffects 是技能命中后按声明顺序尝试应用的能力阶级增减效果。
	StatStageEffects []StatStageEffect `json:"statStageEffects"`
	// DrainPercent 是伤害技能按本次实际伤害改变使用者生命值的百分比；正数表示吸取回复，
	// 负数表示反作用伤害，0 表示没有这类后效。
	DrainPercent int8 `json:"drainPercent"`
	// HealingPercent 是技能成功后按使用者最大生命值改变自身生命值的百分比；正数表示回复，
	// 负数表示支付自身生命值代价，0 表示没有固定比例自身生命变化。
	HealingPercent int8 `json:"healingPercent"`
	// TargetHealingNumerator 是变化技能按实际目标最大生命值回复生命的正分子；0 表示没有目标回复规则。
	// 它与使用者自身 HealingPercent 保持独立，避免把作用对象不同的生命效果压缩为同一个百分比。
	TargetHealingNumerator uint16 `json:"targetHealingNumerator"`
	// TargetHealingDenominator 是目标最大生命回复分数的正分母；存在目标回复时必须不小于分子。
	TargetHealingDenominator uint16 `json:"targetHealingDenominator"`
	// MinHits 是伤害技能单次使用时至少结算的连续命中段数；未配置时为 1。
	MinHits uint8 `json:"minHits"`
	// MaxHits 是伤害技能单次使用时至多结算的连续命中段数；未配置时为 1。
	MaxHits uint8 `json:"maxHits"`
	// CriticalHitStage 是技能自身提供的击中要害等级；0 使用普通 1/24 概率，3 及以上必定要害。
	CriticalHitStage uint8 `json:"criticalHitStage"`
	// FlinchChancePercent 是技能命中后使目标在本回合畏缩的概率；100 不消费额外随机数，0 表示不尝试。
	FlinchChancePercent uint8 `json:"flinchChancePercent"`
	// VolatileStatusApplications 是技能成功后按声明顺序尝试施加的易变状态。每项都使用强类型状态、
	// 目标、概率与时长，禁止由 Name、Effect 等展示文本驱动战斗逻辑。
	VolatileStatusApplications []VolatileStatusApplication `json:"volatileStatusApplications"`
	// LeechSeedApplication 是技能成功后尝试种下寄生种子的可选规则。寄生种子会记录来源场上槽位，并在
	// 回合末抽取目标生命后回复该槽位当前成员，不能与普通易变状态或全场环境效果混用。
	LeechSeedApplication *LeechSeedApplication `json:"leechSeedApplication,omitempty"`
	// FieldSpeedOrderApplication 是技能成功后尝试建立的可选全场速度顺序效果。它独立于成员和侧状态，
	// 因而不会伪装为某个 selectedTarget 的附加状态；再次建立同一种效果时会解除既有效果。
	FieldSpeedOrderApplication *FieldSpeedOrderApplication `json:"fieldSpeedOrderApplication,omitempty"`
	// WeatherApplication 是技能成功后尝试建立普通全场天气的可选规则。天气会影响全体成员的伤害和回合末
	// 结算，不能与速度顺序、寄生种子或易变状态共享同一个无语义效果字段。
	WeatherApplication *WeatherApplication `json:"weatherApplication,omitempty"`
	// TerrainApplication 是技能成功后尝试建立普通全场场地的可选规则。场地以接地成员为边界影响伤害、异常和
	// 回合末结算，不能与天气、速度顺序或成员状态共享无约束环境效果字段。
	TerrainApplication *TerrainApplication `json:"terrainApplication,omitempty"`
	// TailwindApplication 是技能成功后尝试在使用者一方建立顺风的可选规则。顺风属于阵营侧状态，成员换下后
	// 仍持续作用于同侧新成员，不能错误建模为全场天气、场地或成员易变状态。
	TailwindApplication *TailwindApplication `json:"tailwindApplication,omitempty"`
	// ReflectApplication 是技能成功后尝试在使用者一方建立反射壁的可选规则。反射壁属于阵营侧物理减伤，
	// 不能伪装为成员防御能力阶级、天气或全场场地。
	ReflectApplication *ReflectApplication `json:"reflectApplication,omitempty"`
	// LightScreenApplication 是技能成功后尝试在使用者一方建立光墙的可选规则。光墙属于阵营侧特殊减伤，
	// 不能与反射壁、极光幕或成员特防能力阶级混为同一种无语义的减伤效果。
	LightScreenApplication *LightScreenApplication `json:"lightScreenApplication,omitempty"`
	// AuroraVeilApplication 是技能成功后尝试在使用者一方建立极光幕的可选规则。极光幕同时减免物理和特殊伤害，
	// 仍必须独立保存，使资料编译和重放能够准确表达其不同于两面单独屏障的规则语义。
	AuroraVeilApplication *AuroraVeilApplication `json:"auroraVeilApplication,omitempty"`
	// SpikesApplication 是技能成功后尝试在被选中对手一方场地增加一层撒菱的可选规则。撒菱是接地成员换入时
	// 按层数造成伤害的入场危害，不能伪装为成员状态、毒菱或隐形岩。
	SpikesApplication *SpikesApplication `json:"spikesApplication,omitempty"`
	// StealthRockApplication 是技能成功后尝试在被选中对手一方场地布置隐形岩的可选规则。隐形岩按岩石属性倍率
	// 影响所有换入成员，不具有层数，也不能与撒菱共享一个无语义危害字段。
	StealthRockApplication *StealthRockApplication `json:"stealthRockApplication,omitempty"`
	// ToxicSpikesApplication 是技能成功后尝试在被选中对手一方场地增加一层毒菱的可选规则。毒菱影响主要异常，
	// 并允许毒属性成员吸收全部层数，必须同撒菱的伤害层数分开。
	ToxicSpikesApplication *ToxicSpikesApplication `json:"toxicSpikesApplication,omitempty"`
	// StickyWebApplication 是技能成功后尝试在被选中对手一方场地布置黏黏网的可选规则。黏黏网影响换入成员的
	// 速度能力阶级，既不伤害也不施加主要异常，因而独立保存。
	StickyWebApplication *StickyWebApplication `json:"stickyWebApplication,omitempty"`
	// RapidSpinApplication 是技能成功造成伤害后清除使用者一方全部入场危害的固定规则。它不能清除屏障或
	// 对方危害，必须同清除浓雾的目标方清除语义保持独立。
	RapidSpinApplication *RapidSpinApplication `json:"rapidSpinApplication,omitempty"`
	// DefogApplication 是技能成功后清除目标一方屏障、入场危害和当前普通场地的固定规则。顺风不在清除范围内，
	// 因而不能用一个通用的侧状态删除标志替代。
	DefogApplication *DefogApplication `json:"defogApplication,omitempty"`
	// ForceTargetSwitch 表示技能在目标完成普通伤害及其它目标向后效后，强制目标由同侧健康后备成员替换。
	// 它只适用于单个被选中对手；替身、目标倒下或不存在健康后备时不会发生替换。多个候选的选择通过战斗随机
	// 轨迹固定，且实际换入必须复用主动换人、倒下补位的完整入场生命周期。
	ForceTargetSwitch bool `json:"forceTargetSwitch"`
}

// MemberSnapshot 是一名成员进入纯战斗引擎时的权威快照。
type MemberSnapshot struct {
	// Position 是成员在本场队伍中的稳定位置，换人不会改变它。
	Position MemberPosition `json:"memberPosition"`
	// CreatureID 是成员种类在实时资料中的稳定 Identifier。
	CreatureID Identifier `json:"creatureId"`
	// NatureID 是 Battle 已应用到最终能力后冻结的 Nature 稳定 Identifier；引擎不会据此读取资料或重复修正。
	NatureID Identifier `json:"natureId,omitempty"`
	// GenderCode 是 Battle 从成员选择的性别资料冻结出的稳定类别代码；空字符串表示无性别。
	// 战斗引擎只比较非空代码是否相同，不读取实时性别字典或本地化名称。
	GenderCode string `json:"genderCode,omitempty"`
	// Level 是伤害公式使用的冻结等级，取值为 1 至 100。
	Level uint8 `json:"level"`
	// Weight 是从精灵资料冻结的当前体重，使用与 Creature Data Projection 相同的整数刻度；0 表示资料未提供体重。
	// 只有显式声明体重动态威力的技能会读取该字段，普通伤害、速度和其他状态都不受其影响。
	Weight uint32 `json:"weight"`
	// MaxHP 是本场战斗中的最大生命值，必须大于 0。
	MaxHP uint32 `json:"maxHp"`
	// CurrentHP 是成员当前生命值，取值不能超过 MaxHP。
	CurrentHP uint32 `json:"currentHp"`
	// MajorStatus 是成员当前主要异常状态；空字符串表示没有主要异常。
	MajorStatus MajorStatus `json:"majorStatus,omitempty"`
	// BadPoisonCounter 是剧毒本次回合末伤害使用的正整数倍率；其它状态固定为 0。
	BadPoisonCounter int32 `json:"badPoisonCounter"`
	// SleepTurnsRemaining 是睡眠还会阻止技能行动的次数；其它状态固定为 0。
	SleepTurnsRemaining int32 `json:"sleepTurnsRemaining"`
	// FlinchedTurn 是本成员被畏缩效果命中的回合编号；等于当前结算回合时，本成员无法使用技能。
	// 0 表示当前没有待结算的畏缩。该瞬时状态在下一回合自然失效，无需独立清理命令。
	FlinchedTurn uint32 `json:"flinchedTurn"`
	// ConfusionTurnsRemaining 是混乱仍会参与行动前判定的次数；0 表示当前未混乱。
	ConfusionTurnsRemaining uint8 `json:"confusionTurnsRemaining"`
	// BindingTurnsRemaining 是束缚仍会在回合末造成伤害并禁止主动换人的次数；0 表示未束缚。
	BindingTurnsRemaining uint8 `json:"bindingTurnsRemaining"`
	// BindingDamageDenominator 是本次束缚按最大生命造成回合末伤害的冻结分母；0 使用默认值 8。
	// 它在束缚建立时写入，之后不再依赖来源成员是否仍持有原道具。
	BindingDamageDenominator uint16 `json:"bindingDamageDenominator"`
	// ProtectionTurnsRemaining 是保护仍会阻止对方技能影响本成员的回合末阶段数；当前保护规则固定为
	// 1，并在写入当回合的所有行动完成后清除。0 表示本成员当前未受保护。
	ProtectionTurnsRemaining uint8 `json:"protectionTurnsRemaining"`
	// ProtectionChain 是本成员连续成功使用保护的次数。首次保护必定成功，之后成功率按 1/3 的
	// 连乘降低；任一回合未成功保护都会在回合末重置为 0。
	ProtectionChain uint8 `json:"protectionChain"`
	// SubstituteHP 是当前替身的独立生命值；大于 0 表示替身存在。对方技能伤害会优先扣除该值，
	// 归零时替身立即破裂，替身不随回合自然衰减且在成员换下时清除。
	SubstituteHP uint32 `json:"substituteHp"`
	// LeechSeedSourceSlot 是种下寄生种子的来源场上槽位；空值表示当前未被寄生。
	//
	// 这里刻意保存 SlotRef 而不是来源 MemberRef：来源成员换下后，同一槽位新上场的成员仍会在回合末获得
	// 回复；被寄生目标自身换下时该字段会清除，后备成员不会继承种子。
	LeechSeedSourceSlot *SlotRef `json:"leechSeedSourceSlot,omitempty"`
	// TauntTurnsRemaining 是禁止使用变化技能的剩余行动次数；0 表示当前未被挑衅。
	TauntTurnsRemaining uint8 `json:"tauntTurnsRemaining"`
	// ChargingSkillPosition 是正在蓄力、下回合必须完成的技能槽；0 表示当前没有蓄力技能。
	ChargingSkillPosition SkillPosition `json:"chargingSkillPosition"`
	// ChargingTurnsRemaining 是完成蓄力前仍需等待的行动次数；与 ChargingSkillPosition 同时为零或正值。
	ChargingTurnsRemaining uint8 `json:"chargingTurnsRemaining"`
	// RechargeTurnsRemaining 是成功使用休整技能后还必须放弃的技能行动次数；0 表示当前无需休整。
	// 该状态在行动前优先于睡眠、冰冻、畏缩、混乱等常规阻止判定消费，不会扣除本次提交技能的 PP；成员离场
	// 时必须清空，防止强制换人或倒下补位把休整错误带给后备成员。
	RechargeTurnsRemaining uint8 `json:"rechargeTurnsRemaining"`
	// AccuracyLockTarget 是本成员当前锁定命中的具体目标成员；nil 表示没有命中锁定。
	// 它使用 MemberRef 而非槽位，确保目标换出后新成员不会继承旧锁定；目标离场时会由换人路径全局清理。
	AccuracyLockTarget *MemberRef `json:"accuracyLockTarget,omitempty"`
	// AccuracyLockTurnsRemaining 是命中锁定还会跨越的回合末次数；建立时为 2，本回合结束后为 1，
	// 下一回合结束后清零。它只决定常规命中骰是否跳过，不修改任何其它命中前门槛。
	AccuracyLockTurnsRemaining uint8 `json:"accuracyLockTurnsRemaining"`
	// LockedSkillPosition 是锁招期间必须重复使用的技能槽；0 表示当前不受锁招约束。
	LockedSkillPosition SkillPosition `json:"lockedSkillPosition"`
	// LockedTurnsRemaining 是锁招还需要强制重复的行动次数；不包含首次写入锁招的回合。
	LockedTurnsRemaining uint8 `json:"lockedTurnsRemaining"`
	// DisabledSkillPosition 是当前被定身、不能使用的最近技能槽；0 表示没有被定身的技能。
	DisabledSkillPosition SkillPosition `json:"disabledSkillPosition"`
	// DisabledTurnsRemaining 是定身仍有效的行动次数；与 DisabledSkillPosition 同时为零或正值。
	DisabledTurnsRemaining uint8 `json:"disabledTurnsRemaining"`
	// LastUsedSkillPosition 是成员最近一次实际宣告并消费 PP 的技能槽，供 disable 明确锁定目标使用。
	LastUsedSkillPosition SkillPosition `json:"lastUsedSkillPosition"`
	// LastSkillActionTurn 是成员最近一次实际宣告技能的回合号，供后手命中道具读取目标是否已行动。
	LastSkillActionTurn uint32 `json:"lastSkillActionTurn"`
	// LastDeclaredSkillID 是成员最近一次实际宣告并产生 SkillUsedEvent 的技能稳定 Identifier。
	// 它与连续次数共同服务节拍器；未命中、保护和免疫仍算宣告，行动前被阻止则不计。
	LastDeclaredSkillID Identifier `json:"lastDeclaredSkillId,omitempty"`
	// ConsecutiveDeclaredSkillUses 是同一技能连续宣告次数；换招宣告后重置为 1。
	ConsecutiveDeclaredSkillUses uint16 `json:"consecutiveDeclaredSkillUses"`
	// StatStages 保存七项可变能力当前的 -6 至 6 阶级；未出现的能力按 0 处理。
	StatStages map[Stat]int8 `json:"statStages"`
	// Stats 保存除生命值外的五项最终能力值。
	Stats StatBlock `json:"stats"`
	// ElementIDs 是成员当前属性的稳定 Identifier 集合，初始状态包含一至两个属性。
	ElementIDs []Identifier `json:"elementIds"`
	// NaturalElementIDs 是成员当前自然形态的属性稳定 Identifier 集合。
	// 太晶化、携带道具属性身份等覆盖当前属性的机制都不能丢失这份基线；形态变化和变身会更新该字段，但已
	// 太晶化的成员仍保持 TeraElementID 作为当前唯一属性。为空时 NewState 会用初始 ElementIDs 建立基线。
	NaturalElementIDs []Identifier `json:"naturalElementIds"`
	// TeraElementID 是 Team 在 Battle 启动时冻结的太晶属性稳定 Identifier；空字符串表示该成员没有太晶化资格。
	TeraElementID Identifier `json:"teraElementId,omitempty"`
	// Terastallized 表示成员已在本场对局中完成太晶化。
	// 太晶化一旦生效不会因换人、形态变化或道具属性身份解除；当前 ElementIDs 必须保持为仅包含
	// TeraElementID 的单元素集合。
	Terastallized bool `json:"terastallized"`
	// FormProfiles 是该成员在本场对局可切换到的完整冻结形态画像。
	//
	// 普通成员可以不携带该集合；一旦声明入场形态或天气形态规则，集合必须完整包含当前、默认和所有目标
	// 形态。该字段不保存资料名称或编码，因而不会把实时资料变更带入已经开始的对局。
	FormProfiles []FormProfile `json:"formProfiles"`
	// Skills 是成员按稳定技能位置排列的一至四个技能槽快照。
	Skills []SkillSnapshot `json:"skills"`
	// AbilityID 是当前特性的稳定 Identifier；空字符串表示没有特性。
	AbilityID Identifier `json:"abilityId,omitempty"`
	// WeatherDamageImmunities 是由当前特性冻结的天气伤害免疫集合。它只参与天气的回合末伤害阶段，不能代替
	// 属性天然免疫或用于推断其它特性效果；Battle 编译器必须从启用特性详情显式写入。
	WeatherDamageImmunities []WeatherKind `json:"weatherDamageImmunities"`
	// WeatherEffectsSuppressed 表示该成员在场且存活时封锁普通天气的可执行效果。
	// 天气本身仍会保留、递减和产生建立/结束事件；此标记只让普通天气的伤害、命中、威力、属性替换和蓄力跳过
	// 在场上暂时失效，不能借此删除环境状态或改变其持续时间。
	WeatherEffectsSuppressed bool `json:"weatherEffectsSuppressed"`
	// ReactiveAbilityRules 是本场开始时冻结的回合末、受伤和倒下触发特性规则。
	// nil 表示当前特性没有这些触发窗口；运行时状态与规则声明分离，复制、变身和离场还原必须深复制本字段。
	ReactiveAbilityRules *ReactiveAbilityRules `json:"reactiveAbilityRules,omitempty"`
	// OncePerBattleFaintBoostActivated 表示本成员整场一次的“造成倒下后强化”已经成功触发。
	// 该状态属于成员在本场对局的权威运行态，换人不会清除，复制特性也不能重置既有消费事实。
	OncePerBattleFaintBoostActivated bool `json:"oncePerBattleFaintBoostActivated"`
	// HalfHPThresholdAbilityActivated 表示本成员本次连续上场周期的半血跨越能力变化已经触发。
	// 离场时会清除，防止同一次连续在场通过回复反复跨越阈值；重新上场后允许按新周期再次触发。
	HalfHPThresholdAbilityActivated bool `json:"halfHpThresholdAbilityActivated"`
	// ChargedElementID 是受伤特性为下一次攻击储存的技能有效属性稳定 Identifier；空字符串表示没有充能。
	ChargedElementID Identifier `json:"chargedElementId,omitempty"`
	// ChargedDamageNumerator 是受伤充能提供的一次性精确伤害倍率分子；无充能时为 1。
	ChargedDamageNumerator uint16 `json:"chargedDamageNumerator"`
	// ChargedDamageDenominator 是受伤充能提供的一次性精确伤害倍率分母；无充能时为 1。
	ChargedDamageDenominator uint16 `json:"chargedDamageDenominator"`
	// BasePowerAtMostDamageBoost 是当前特性对原始基础威力不超过上限技能提供的最终伤害倍率。
	// nil 表示没有该规则；动态威力技能不会用运行时计算值反向满足本条件。
	BasePowerAtMostDamageBoost *BasePowerAtMostDamageBoost `json:"basePowerAtMostDamageBoost,omitempty"`
	// RecoilSkillDamageBoost 是当前特性对按实际伤害产生反作用的技能提供的最终伤害倍率。
	// nil 表示没有该规则；仅支付最大生命代价的技能不属于按实际伤害反作用。
	RecoilSkillDamageBoost *RecoilSkillDamageBoost `json:"recoilSkillDamageBoost,omitempty"`
	// LowHPElementDamageBoost 是当前特性在生命值达到阈值时对指定有效属性提供的最终伤害倍率。
	// nil 表示没有该规则；生命阈值通过整数交叉相乘判断，不使用浮点数。
	LowHPElementDamageBoost *LowHPElementDamageBoost `json:"lowHpElementDamageBoost,omitempty"`
	// WeatherElementDamageBoost 是当前特性在指定有效天气中对一组有效属性提供的最终伤害倍率。
	// nil 表示没有该规则；天气封锁会使普通和强天气均无法满足条件。
	WeatherElementDamageBoost *WeatherElementDamageBoost `json:"weatherElementDamageBoost,omitempty"`
	// ElementSkillDamageBoost 是当前特性对一组技能有效属性提供的最终伤害倍率。
	// nil 表示没有该规则；判定读取天气等规则改写后的属性而不是资料原始属性。
	ElementSkillDamageBoost *ElementSkillDamageBoost `json:"elementSkillDamageBoost,omitempty"`
	// SameElementBonusOverride 是当前特性对属性一致加成使用的替代倍率。
	// nil 表示使用默认三分之二加成；本字段替换默认值而不是在其后额外叠乘。
	SameElementBonusOverride *SameElementBonusOverride `json:"sameElementBonusOverride,omitempty"`
	// ContactBasedSkillDamageBoost 是当前特性对本次仍构成有效接触的技能提供的最终伤害倍率。
	// nil 表示没有该规则；动态接触抑制和相关持有道具会共同影响有效接触事实。
	ContactBasedSkillDamageBoost *ContactBasedSkillDamageBoost `json:"contactBasedSkillDamageBoost,omitempty"`
	// CriticalHitDamageBoost 是当前特性对本次实际击中要害提供的额外最终伤害倍率。
	// nil 表示没有该规则；目标的要害免疫生效后不会触发本倍率。
	CriticalHitDamageBoost *CriticalHitDamageBoost `json:"criticalHitDamageBoost,omitempty"`
	// SuperEffectiveDamageBoost 是当前特性对最终属性相性严格大于一的技能提供的最终伤害倍率。
	// nil 表示没有该规则；强风对飞行弱点的中和会先进入最终相性结果。
	SuperEffectiveDamageBoost *SuperEffectiveDamageBoost `json:"superEffectiveDamageBoost,omitempty"`
	// NotVeryEffectiveDamageBoost 是当前特性对最终属性相性严格位于零与一之间的技能提供的最终伤害倍率。
	// nil 表示没有该规则；完全免疫的零倍率不触发补偿。
	NotVeryEffectiveDamageBoost *NotVeryEffectiveDamageBoost `json:"notVeryEffectiveDamageBoost,omitempty"`
	// TargetGenderDamageMultiplier 是当前特性按双方非空性别代码关系提供的最终伤害倍率。
	// nil 表示没有该规则；任一方无性别时始终保持一倍。
	TargetGenderDamageMultiplier *TargetGenderDamageMultiplier `json:"targetGenderDamageMultiplier,omitempty"`
	// PunchBasedSkillDamageBoost 是当前特性对拳击类普通直接伤害技能提供的最终伤害倍率。
	PunchBasedSkillDamageBoost *PunchBasedSkillDamageBoost `json:"punchBasedSkillDamageBoost,omitempty"`
	// SlicingBasedSkillDamageBoost 是当前特性对切割类普通直接伤害技能提供的最终伤害倍率。
	SlicingBasedSkillDamageBoost *SlicingBasedSkillDamageBoost `json:"slicingBasedSkillDamageBoost,omitempty"`
	// SoundBasedSkillDamageBoost 是当前特性对声音类普通直接伤害技能提供的最终伤害倍率。
	SoundBasedSkillDamageBoost *SoundBasedSkillDamageBoost `json:"soundBasedSkillDamageBoost,omitempty"`
	// PulseBasedSkillDamageBoost 是当前特性对波动类普通直接伤害技能提供的最终伤害倍率。
	PulseBasedSkillDamageBoost *PulseBasedSkillDamageBoost `json:"pulseBasedSkillDamageBoost,omitempty"`
	// BiteBasedSkillDamageBoost 是当前特性对啃咬类普通直接伤害技能提供的最终伤害倍率。
	BiteBasedSkillDamageBoost *BiteBasedSkillDamageBoost `json:"biteBasedSkillDamageBoost,omitempty"`
	// SecondaryEffectsSuppressedDamageBoost 是当前特性以移除技能附加异常和能力变化换取的最终伤害倍率。
	SecondaryEffectsSuppressedDamageBoost *SecondaryEffectsSuppressedDamageBoost `json:"secondaryEffectsSuppressedDamageBoost,omitempty"`
	// SoundBasedSkillDamageReduction 是当前特性承受声音类普通直接伤害时使用的最终伤害倍率。
	SoundBasedSkillDamageReduction *SoundBasedSkillDamageReduction `json:"soundBasedSkillDamageReduction,omitempty"`
	// SuperEffectiveDamageReduction 是当前特性承受严格克制普通直接伤害时使用的最终伤害倍率。
	SuperEffectiveDamageReduction *SuperEffectiveDamageReduction `json:"superEffectiveDamageReduction,omitempty"`
	// FullHPDamageReduction 是当前特性在每一段伤害开始时仍满生命时使用的最终伤害倍率。
	FullHPDamageReduction *FullHPDamageReduction `json:"fullHpDamageReduction,omitempty"`
	// DamageClassDamageReduction 是当前特性承受指定物理或特殊伤害分类时使用的最终伤害倍率。
	DamageClassDamageReduction *DamageClassDamageReduction `json:"damageClassDamageReduction,omitempty"`
	// ElementSkillDamageReduction 是当前特性承受指定有效属性技能时使用的最终伤害倍率。
	ElementSkillDamageReduction *ElementSkillDamageReduction `json:"elementSkillDamageReduction,omitempty"`
	// ContactBasedSkillDamageReduction 是当前特性承受本次仍构成有效接触的技能时使用的最终伤害倍率。
	ContactBasedSkillDamageReduction *ContactBasedSkillDamageReduction `json:"contactBasedSkillDamageReduction,omitempty"`
	// AttackingStatMultiplier 是当前特性对持有者攻击侧公式能力的条件倍率。
	AttackingStatMultiplier *AttackingStatMultiplier `json:"attackingStatMultiplier,omitempty"`
	// OpponentAttackingStatMultiplier 是当前特性作为防守方时对攻击者公式能力的倍率。
	OpponentAttackingStatMultiplier *OpponentAttackingStatMultiplier `json:"opponentAttackingStatMultiplier,omitempty"`
	// DefendingStatMultiplier 是当前特性对持有者防守侧公式能力的条件倍率。
	DefendingStatMultiplier *DefendingStatMultiplier `json:"defendingStatMultiplier,omitempty"`
	// OpponentDefendingStatMultiplier 是当前特性作为攻击方时对目标公式防御能力的倍率。
	OpponentDefendingStatMultiplier *OpponentDefendingStatMultiplier `json:"opponentDefendingStatMultiplier,omitempty"`
	// AllySkillDamageBoost 是当前特性为同侧其它存活上场成员提供的分类伤害倍率。
	AllySkillDamageBoost *AllySkillDamageBoost `json:"allySkillDamageBoost,omitempty"`
	// AllyReceivedDamageReduction 是当前特性为同侧其它存活上场成员提供的公式承伤倍率。
	AllyReceivedDamageReduction *AllyReceivedDamageReduction `json:"allyReceivedDamageReduction,omitempty"`
	// AllyAbilityGroupCode 是当前特性声明的互助组稳定代码；空字符串表示不属于任何互助组。
	AllyAbilityGroupCode string `json:"allyAbilityGroupCode,omitempty"`
	// AllyAbilityPresenceAttackingStatMultiplier 是匹配互助组伙伴在场时对持有者攻击能力的倍率。
	AllyAbilityPresenceAttackingStatMultiplier *AllyAbilityPresenceAttackingStatMultiplier `json:"allyAbilityPresenceAttackingStatMultiplier,omitempty"`
	// AccuracyMultiplier 是当前特性对持有成员任意技能命中率的精确整数分数修正。
	// nil 表示没有该规则；它只影响普通命中判定，不能绕过一击必杀的独立等级规则。
	AccuracyMultiplier *AccuracyMultiplier `json:"accuracyMultiplier,omitempty"`
	// PhysicalSkillAccuracyMultiplier 是当前特性仅对物理技能命中率的精确整数分数修正。
	// nil 表示没有该规则，特殊和变化技能不会读取这个倍率。
	PhysicalSkillAccuracyMultiplier *AccuracyMultiplier `json:"physicalSkillAccuracyMultiplier,omitempty"`
	// OpponentAccuracySandstormMultiplier 是普通沙暴中对手以本成员为目标时的命中率修正。
	// nil 表示没有该规则；普通天气被封锁时不会生效。
	OpponentAccuracySandstormMultiplier *AccuracyMultiplier `json:"opponentAccuracySandstormMultiplier,omitempty"`
	// OpponentAccuracySnowMultiplier 是普通降雪中对手以本成员为目标时的命中率修正。
	// nil 表示没有该规则，不能与沙暴规则合并。
	OpponentAccuracySnowMultiplier *AccuracyMultiplier `json:"opponentAccuracySnowMultiplier,omitempty"`
	// OpponentAccuracyConfusionMultiplier 是本成员处于混乱时对手以其为目标的命中率修正。
	// nil 表示没有该规则；混乱结束后该倍率立即停止参与命中公式。
	OpponentAccuracyConfusionMultiplier *AccuracyMultiplier `json:"opponentAccuracyConfusionMultiplier,omitempty"`
	// AccuracyAlwaysHits 表示本成员使用技能或成为技能目标时跳过普通命中判定。
	// 它不会跳过保护、属性免疫或一击必杀等级限制。
	AccuracyAlwaysHits bool `json:"accuracyAlwaysHits"`
	// StatusSkillAccuracyCap 是对手以本成员为目标的变化技能最终命中上限。
	// 0 表示没有该规则；其它值必须为 1 至 100。
	StatusSkillAccuracyCap uint8 `json:"statusSkillAccuracyCap"`
	// IgnoreOpponentAccuracyStatStages 表示本成员在命中判定时忽略对手的命中或闪避阶级。
	// 使用技能时忽略目标闪避，作为目标时忽略使用者命中；真实状态阶级不被写回。
	IgnoreOpponentAccuracyStatStages bool `json:"ignoreOpponentAccuracyStatStages"`
	// CriticalHitImmunity 表示本成员免疫对手技能造成的击中要害。
	// 要害随机数仍按原流程消费；此字段只使最终伤害及后续要害例外按普通命中处理。
	CriticalHitImmunity bool `json:"criticalHitImmunity"`
	// SkillRecoilDamageImmunity 表示本成员免疫按实际造成伤害计算的技能反作用。
	// 该字段不影响按最大生命支付的技能代价、天气、异常、道具反伤或其它间接伤害。
	SkillRecoilDamageImmunity bool `json:"skillRecoilDamageImmunity"`
	// IndirectDamageImmunity 表示本成员免疫非技能直接伤害。
	// 它覆盖天气、异常、束缚、寄生种子和入场危害，不影响技能本体伤害、按实际伤害计算的反作用或强制生命代价。
	IndirectDamageImmunity bool `json:"indirectDamageImmunity"`
	// ContactDamageToAttackerDenominator 表示本成员受到有效接触技能本体伤害后，对攻击者造成最大生命固定比例伤害的分母。
	// 0 表示没有该反制特性；正值必须是已由 Battle 编译和冻结的合法分母。
	ContactDamageToAttackerDenominator uint16 `json:"contactDamageToAttackerDenominator"`
	// IgnoreOpponentDamageStatStages 表示本成员在普通物理或特殊伤害公式中无视对手相关能力阶级。
	// 它不改变权威状态中保存的能力阶级，也不影响命中、闪避、异常或环境结算。
	IgnoreOpponentDamageStatStages bool `json:"ignoreOpponentDamageStatStages"`
	// IgnoreTargetAbilityEffects 表示本成员使用技能时无视目标侧的防守特性。
	// 该开关只作用于当前技能结算读取的目标特性，不能删除目标自身规则或影响其非技能生命周期。
	IgnoreTargetAbilityEffects bool `json:"ignoreTargetAbilityEffects"`
	// SurviveFatalDamageAtFullHP 表示本成员满生命时承受致命对手技能伤害后保留 1 HP。
	// 它只在伤害写入本体前读取，不会阻止替身、天气、异常或其它间接伤害。
	SurviveFatalDamageAtFullHP bool `json:"surviveFatalDamageAtFullHp"`
	// OpponentStatusSkillImmunity 表示本成员免疫对手使用的变化技能。
	// 它只在敌方变化技能的目标结算前读取；同侧辅助、自身目标及伤害技能不受影响。
	OpponentStatusSkillImmunity bool `json:"opponentStatusSkillImmunity"`
	// NonSuperEffectiveDamageImmunity 表示本成员免疫属性相性不克制的对手伤害技能。
	// 相性由本次技能的有效属性、冻结规则表和强风修正共同决定，不能由技能名称或显示文本推断。
	NonSuperEffectiveDamageImmunity bool `json:"nonSuperEffectiveDamageImmunity"`
	// CriticalHitStageBoost 是当前特性固定赋予本成员的击中要害等级增益。
	// 0 表示没有规则；作为使用者时会与技能自身等级相加，不会修改技能快照或目标方防守规则。
	CriticalHitStageBoost uint8 `json:"criticalHitStageBoost"`
	// MultiHitMaximum 表示本成员使用可变连续命中技能时固定采用声明的最大段数。
	// 它不改变固定段数技能，也不会伪造本不需要消费的随机轨迹。
	MultiHitMaximum bool `json:"multiHitMaximum"`
	// DamagingSkillSecondaryEffectImmunity 表示本成员免疫对手伤害技能施加的追加效果。
	// 它只阻止本成员成为目标时的异常、能力阶级、畏缩和易变状态，不阻止伤害本体或使用者自身效果。
	DamagingSkillSecondaryEffectImmunity bool `json:"damagingSkillSecondaryEffectImmunity"`
	// PriorityMoveImmunityForSideEnabled 表示本成员当前特性阻止对手正优先度技能影响本方场上成员。
	// 判定只遍历目标侧当前上场成员；同侧技能、零或负优先度技能以及攻击方无视目标特性的技能不受影响。
	PriorityMoveImmunityForSideEnabled bool `json:"priorityMoveImmunityForSideEnabled"`
	// PriorityMoveImmunityForSideProtectsAllies 表示先制技能侧免疫是否同时保护本成员当前上场的同侧伙伴。
	// 它保留为独立快照事实，避免将“规则存在”和“保护范围”压缩为一个不可扩展的通用效果标记。
	PriorityMoveImmunityForSideProtectsAllies bool `json:"priorityMoveImmunityForSideProtectsAllies"`
	// StatusSkillMovesLastAndIgnoresTargetAbility 表示本成员使用变化技能时在相同优先度内最后行动。
	// 同一规则仅使该变化技能结算期间无视目标防守特性；伤害技能不会得到排序或特性穿透效果。
	StatusSkillMovesLastAndIgnoresTargetAbility bool `json:"statusSkillMovesLastAndIgnoresTargetAbility"`
	// ContactSkillProtectionBypass 表示本成员用接触类技能攻击对手时绕过目标的个人保护。
	// 它只跳过本次命中前的保护 gate，既不会移除保护状态，也不影响同侧目标、非接触技能或其它侧防护。
	ContactSkillProtectionBypass bool `json:"contactSkillProtectionBypass"`
	// ContactSkillProtectionBypassDamageMultiplier 是成功以接触技能穿透个人保护时应用的独立伤害倍率。
	// nil 表示穿透后保持完整伤害；目标没有保护或技能不构成有效接触时不会读取该倍率。
	ContactSkillProtectionBypassDamageMultiplier *DamageFraction `json:"contactSkillProtectionBypassDamageMultiplier,omitempty"`
	// SkillWeatherOverride 是该成员使用技能时观察到的普通天气；空值表示读取真实有效环境天气。
	// 它只影响技能结算，不建立环境天气，也不改变其它成员和回合末环境生命周期。
	SkillWeatherOverride WeatherKind `json:"skillWeatherOverride,omitempty"`
	// SkillElementConversion 是该成员使用技能时执行的单向属性转换及转换专属倍率；nil 表示没有规则。
	SkillElementConversion *SkillElementConversion `json:"skillElementConversion,omitempty"`
	// ContactSuppression 表示本成员的接触类技能在本次结算中不再构成有效接触。
	// 它不改写 SkillSnapshot.MakesContact；保护穿透和后续接触副作用必须统一通过有效接触入口读取该动态结果。
	ContactSuppression bool `json:"contactSuppression"`
	// ReceivedContactDamageHalved 表示本成员受到有效接触伤害时的最终伤害减半。
	// 它是目标侧防守特性，攻击者无视目标特性时不生效；非接触技能、状态技能与不造成伤害的命中不会读取它。
	ReceivedContactDamageHalved bool `json:"receivedContactDamageHalved"`
	// ReceivedFireDamageDoubled 表示本成员受到火属性技能伤害时的最终伤害翻倍。
	// 属性必须按当前有效技能属性判断，攻击者无视目标特性时不生效；它不改变资料中的原始技能属性。
	ReceivedFireDamageDoubled bool `json:"receivedFireDamageDoubled"`
	// ForcedSwitchImmunity 表示该成员当前特性阻止其被技能或道具强制换下。
	// 它不影响主动换人、倒下后的强制补位或其它成员的换人；被复制特性、变身和离场还原均必须保存这一冻结规则。
	ForcedSwitchImmunity bool `json:"forcedSwitchImmunity"`
	// OpponentSwitchRestriction 是该成员当前特性施加给敌方主动换人的冻结限制规则。
	// nil 表示不限制对手；它只在敌方主动换人命令校验时读取，不能错误作用于倒下补位或其它强制换人路径。
	OpponentSwitchRestriction *OpponentSwitchRestriction `json:"opponentSwitchRestriction,omitempty"`
	// DamageCrossedHalfHPForceSelfSwitch 表示成员生命首次从高于二分之一降至二分之一或以下时强制自身换下。
	// 该规则只读取本体实际生命变化；替身承伤、倒下和没有健康后备时均不会触发换人选择或消耗随机数。
	DamageCrossedHalfHPForceSelfSwitch bool `json:"damageCrossedHalfHpForceSelfSwitch"`
	// SwitchOutMajorStatusCure 表示成员成功离场时清除自身主要异常状态；倒下后的补位不属于成功离场。
	SwitchOutMajorStatusCure bool `json:"switchOutMajorStatusCure"`
	// SwitchOutHealDenominator 是成员成功离场时按最大生命回复的正分母；0 表示没有离场回复规则。
	SwitchOutHealDenominator uint16 `json:"switchOutHealDenominator"`
	// WeatherEndTurnHealing 是当前特性冻结的普通天气回合末回复规则。
	// nil 表示没有该规则；规则命中时只在天气回合末按最大生命比例回复，并单独产生天气回复事件。
	WeatherEndTurnHealing *WeatherEndTurnHealing `json:"weatherEndTurnHealing,omitempty"`
	// WeatherSpeedMultipliers 是当前特性冻结的普通天气行动速度整数分数倍率集合。
	// 它只影响同优先度行动排序；天气被封锁时不会读取此集合，且不能借此改变天气生命周期。
	WeatherSpeedMultipliers []WeatherSpeedMultiplier `json:"weatherSpeedMultipliers"`
	// EnvironmentHighestStatMultiplier 是当前特性在指定普通环境下强化最高原始能力的冻结规则。
	//
	// nil 表示没有该规则。它只声明触发环境；实际强化能力项由五项原始能力的固定优先级决定，倍率也由引擎
	// 固定为速度 3/2、其余能力 13/10，不能由实时资料或客户端在对局期间重新解释。
	EnvironmentHighestStatMultiplier *EnvironmentHighestStatMultiplier `json:"environmentHighestStatMultiplier,omitempty"`
	// TerastallizationStatStageChange 是当前特性在持有成员完成太晶化时立即改变自身能力阶级的冻结规则。
	// nil 表示没有该规则；它不参与普通入场、技能命中或回合末结算。
	TerastallizationStatStageChange *TerastallizationStatStageChange `json:"terastallizationStatStageChange,omitempty"`
	// TerastallizationEnvironmentClear 表示当前特性在持有成员完成太晶化时清除普通天气与普通场地。
	// 强天气不属于该规则的清除范围，且清场只发生一次，因为太晶化本身一场只能成功一次。
	TerastallizationEnvironmentClear bool `json:"terastallizationEnvironmentClear"`
	// SwitchInStrongWeather 是当前特性在成员进入场地时建立的强天气；空值表示当前特性没有强天气入场效果。
	// 它只描述该成员能维持的封闭天气种类；当前实际天气及来源由 EnvironmentSnapshot.StrongWeather 单独保存。
	SwitchInStrongWeather StrongWeatherKind `json:"switchInStrongWeather,omitempty"`
	// SwitchInWeather 是当前特性在成员进入场地时建立的普通天气规则；nil 表示没有该入场效果。
	// 它与强天气、技能天气及天气威力等规则独立，不能用空持续回合或名称文本表达。
	SwitchInWeather *SwitchInWeather `json:"switchInWeather,omitempty"`
	// SwitchInTerrain 是当前特性在成员进入场地时建立的普通场地规则；nil 表示没有该入场效果。
	// 它与技能场地、天气、侧状态及接地结算独立，不能通过泛型环境效果列表表达。
	SwitchInTerrain *SwitchInTerrain `json:"switchInTerrain,omitempty"`
	// SwitchInStatStageChange 是当前特性在成员进入场地时立即执行的能力阶级变化规则；nil 表示没有该效果。
	// 它与技能命中后的能力阶级变化独立，不携带概率，也不从技能目标或自由文本推断作用范围。
	SwitchInStatStageChange *SwitchInStatStageChange `json:"switchInStatStageChange,omitempty"`
	// SwitchInAllyHeal 是当前特性在成员进入场地时为同侧其它上场成员回复生命的规则；nil 表示没有该效果。
	// 它不回复触发者或后备成员，并与技能、天气、场地回复保持独立生命周期和事件种类。
	SwitchInAllyHeal *SwitchInAllyHeal `json:"switchInAllyHeal,omitempty"`
	// SwitchInOpponentDefenseComparisonBoost 表示当前特性会在入场时比较场上对手物防与特防总和并强化自身。
	// false 表示没有该规则；比较只读取冻结基础能力，不从特性文本或通用伤害倍率推断。
	SwitchInOpponentDefenseComparisonBoost bool `json:"switchInOpponentDefenseComparisonBoost"`
	// SwitchInAllyStatStageCopy 表示当前特性会在入场时复制同侧其它上场成员的全部能力阶级。
	// false 表示没有该规则；它不叠加技能效果，也不从任意队友或后备成员集合中猜测来源。
	SwitchInAllyStatStageCopy bool `json:"switchInAllyStatStageCopy"`
	// SwitchInAllyStatStageReset 表示当前特性会在入场时将同侧其它上场成员的全部能力阶级重置为零。
	// false 表示没有该规则；它不影响来源自身或后备成员，也不与复制效果共用生命周期。
	SwitchInAllyStatStageReset bool `json:"switchInAllyStatStageReset"`
	// SwitchInClearAllSideDamageReductions 表示当前特性会在入场时清除双方阵营的反射壁、光墙和极光幕。
	// false 表示没有该规则；它不清除顺风、入场危害、天气或场地，且与技能清除浓雾保持独立生命周期。
	SwitchInClearAllSideDamageReductions bool `json:"switchInClearAllSideDamageReductions"`
	// SwitchInCopyOpponentAbility 表示当前特性会在入场时复制一名存活对手的当前特性及全部已冻结规则。
	// false 表示没有该规则；复制结果属于本场运行态，绝不在回合内反查实时资料，也不会复制技能、道具或成员基础属性。
	SwitchInCopyOpponentAbility bool `json:"switchInCopyOpponentAbility"`
	// SwitchInRevealOpponentHeldItems 表示当前特性会在入场时公开所有存活上场对手的持有道具。
	// false 表示没有该规则；没有道具的对手不会产生事件，公开只写入事件账本，不改变对手运行态。
	SwitchInRevealOpponentHeldItems bool `json:"switchInRevealOpponentHeldItems"`
	// SwitchInRevealOpponentHighestPowerSkill 表示当前特性会在入场时公开对手当前最高基础威力技能。
	// false 表示没有该规则；并列技能按稳定 Identifier 倒序选择，公开不改变技能、PP 或成员运行态。
	SwitchInRevealOpponentHighestPowerSkill bool `json:"switchInRevealOpponentHighestPowerSkill"`
	// SwitchInTransformIntoOpponent 表示当前特性会在入场时复制一名存活上场对手的战斗画像。
	// 复制只在本次连续上场期间有效，离场时必须使用 TransformSnapshot 恢复原始种类、能力、属性、技能和特性规则。
	SwitchInTransformIntoOpponent bool `json:"switchInTransformIntoOpponent"`
	// SwitchInDetectDangerousOpponentSkill 表示当前特性会在入场时侦测一项对自身危险的对手技能。
	SwitchInDetectDangerousOpponentSkill bool `json:"switchInDetectDangerousOpponentSkill"`
	// SwitchInDisguiseAsLastHealthyAlly 表示当前特性会在入场时把披露身份伪装为同侧最后一名可战斗队友。
	// 它只改变客户端可见身份，不改变 CreatureID 或任何权威战斗计算字段。
	SwitchInDisguiseAsLastHealthyAlly bool `json:"switchInDisguiseAsLastHealthyAlly"`
	// SwitchInHeldItemElementIdentity 表示当前特性会在入场时按所持道具的属性伤害强化身份替换自身属性。
	//
	// false 表示没有该规则。道具是否具备属性身份由 Battle 在启动时冻结到 HeldItemElementID；引擎绝不根据
	// 道具名称、效果文本或实时资料推断该行为。
	SwitchInHeldItemElementIdentity bool `json:"switchInHeldItemElementIdentity"`
	// SwitchInFormChange 是当前特性进入场地时触发的确定形态切换规则；nil 表示没有该规则。
	// 它只切换成员自身的冻结基础画像，不复用天气、太晶或道具的形态语义。
	SwitchInFormChange *SwitchInFormChange `json:"switchInFormChange,omitempty"`
	// SwitchOutFormChange 是当前特性在成员成功离场时触发的确定形态切换规则；nil 表示没有该规则。
	// 它只会在成员仍存活且完成实际换出时结算，不能在倒下补位路径中触发。
	SwitchOutFormChange *SwitchOutFormChange `json:"switchOutFormChange,omitempty"`
	// WeatherFormChange 是当前特性按有效普通天气同步成员形态的规则；nil 表示没有该规则。
	// 天气效果被特性封锁时会回退其默认形态，但不会删除原始环境天气或改变其生命周期。
	WeatherFormChange *WeatherFormChange `json:"weatherFormChange,omitempty"`
	// ApparentCreatureID 是当前对外披露的视觉种类；空字符串表示使用真实 CreatureID。
	// 该字段不参与伤害、属性、速度或目标选择计算，成员离场时会清空。
	ApparentCreatureID Identifier `json:"apparentCreatureId,omitempty"`
	// TransformSnapshot 保存成员变身前必须在离场时恢复的冻结战斗画像；nil 表示成员当前没有变身。
	// 它是运行态而非实时资料引用，因此对战期间的资料修改不能影响已开始对局的还原结果。
	TransformSnapshot *MemberTransformSnapshot `json:"transformSnapshot,omitempty"`
	// ItemID 是当前携带道具的稳定 Identifier；空字符串表示没有携带道具。
	ItemID Identifier `json:"itemId,omitempty"`
	// HighestStatBoosterAbilityIDs 是当前持有道具允许消耗并激活最高原始能力强化的特性 Identifier 集合。
	//
	// 它由 Battle 从 Item Metadata 冻结，集合中的任一 Identifier 与成员当前 AbilityID 匹配时才可能消耗 ItemID。
	// 道具消耗后该集合保留为审计输入，但不会再次触发，因为 ItemID 已被清空。
	HighestStatBoosterAbilityIDs []Identifier `json:"highestStatBoosterAbilityIds,omitempty"`
	// DamagedForceSelfSwitch 表示持有道具会在该成员受到对手技能实际伤害后消耗自身并强制成员换下。
	// 替身承受的正伤害同样属于本次技能的实际伤害；成员倒下、已离场或没有健康后备时不会消耗道具。
	DamagedForceSelfSwitch bool `json:"damagedForceSelfSwitch"`
	// DamagedForceAttackerSwitch 表示持有道具会在该成员受到对手技能实际伤害后消耗自身并强制攻击者换下。
	// 攻击者的 ForcedSwitchImmunity 可以阻止该效果；持有者自身不因这一规则被替换。
	DamagedForceAttackerSwitch bool `json:"damagedForceAttackerSwitch"`
	// NegativeStatStageForceSelfSwitch 表示持有道具会在该成员实际被降低任一能力阶级后消耗自身并强制成员换下。
	// 只有 StatStageChangedEvent 中的负实际变化能触发，不能把已处于下限的无效下降当作道具触发。
	NegativeStatStageForceSelfSwitch bool `json:"negativeStatStageForceSelfSwitch"`
	// SwitchRestrictionImmunity 表示当前持有道具允许成员绕过敌方特性造成的主动换人限制。
	// 它不阻止技能、道具或规则造成的强制换人，且道具被消耗或替换后会随 ItemID 的生命周期失效。
	SwitchRestrictionImmunity bool `json:"switchRestrictionImmunity"`
	// ContactSideEffectImmunity 表示当前持有道具使成员免疫目标因本次有效接触施加的反制副作用。
	// 它不会抹去技能的有效接触事实，不能借此跳过接触保护穿透、接触伤害倍率或技能自身附加效果。
	ContactSideEffectImmunity bool `json:"contactSideEffectImmunity"`
	// HeldItemContactDamageToAttackerDenominator 表示当前持有道具在成员受到有效接触本体伤害后反伤攻击者的分母。
	// 0 表示当前没有道具来源的接触反伤；它与特性来源字段独立，以保留两条规则的来源和结算顺序。
	HeldItemContactDamageToAttackerDenominator uint16 `json:"heldItemContactDamageToAttackerDenominator"`
	// HeldItemEndTurnHealDenominator 表示当前持有道具在回合末按最大生命回复的固定比例分母。
	// 0 表示当前道具不提供回合末回复；道具失去、消费或转移后必须清空该投影，避免旧所有者继续获得回复。
	HeldItemEndTurnHealDenominator uint16 `json:"heldItemEndTurnHealDenominator"`
	// HeldItemEndTurnHealForElementID 是当前持有道具要求持有者具备的有效属性稳定 Identifier。
	// 空字符串表示当前道具没有属性条件回复；结算始终读取 ElementIDs，故太晶化、形态变化与道具属性身份都会即时生效。
	HeldItemEndTurnHealForElementID Identifier `json:"heldItemEndTurnHealForElementId,omitempty"`
	// HeldItemEndTurnHealForElementDenominator 表示属性匹配时按最大生命回复的固定比例分母。
	// 0 表示当前道具没有属性条件回复；它必须与 HeldItemEndTurnHealForElementID 同时设置或同时清空。
	HeldItemEndTurnHealForElementDenominator uint16 `json:"heldItemEndTurnHealForElementDenominator"`
	// HeldItemEndTurnDamageDenominator 表示当前持有道具在回合末按最大生命造成间接伤害的固定比例分母。
	// 0 表示当前道具不提供回合末自伤；道具失去、消费或转移后必须清空该投影，且间接伤害免疫可以阻止结算。
	HeldItemEndTurnDamageDenominator uint16 `json:"heldItemEndTurnDamageDenominator"`
	// HeldItemEndTurnDamageWithoutElementID 是当前持有道具要求持有者不具备的有效属性稳定 Identifier。
	// 空字符串表示当前道具没有属性条件自伤；结算读取当前 ElementIDs，故任何运行态属性改变都会即时影响结果。
	HeldItemEndTurnDamageWithoutElementID Identifier `json:"heldItemEndTurnDamageWithoutElementId,omitempty"`
	// HeldItemEndTurnDamageWithoutElementDenominator 表示当前属性不包含指定属性时的固定比例间接伤害分母。
	// 0 表示当前道具没有属性条件自伤；它必须与 HeldItemEndTurnDamageWithoutElementID 同时设置或同时清空。
	HeldItemEndTurnDamageWithoutElementDenominator uint16 `json:"heldItemEndTurnDamageWithoutElementDenominator"`
	// HeldItemConsumableElementDamageBoostElementID 是当前持有的一次性属性威力强化道具对应的技能属性稳定 Identifier。
	// 空字符串表示当前道具没有该规则；匹配必须按技能有效属性判定，以便天气等规则改写技能属性时仍保持正确。
	HeldItemConsumableElementDamageBoostElementID Identifier `json:"heldItemConsumableElementDamageBoostElementId,omitempty"`
	// HeldItemConsumableElementDamageBoostNumerator 是一次性属性威力强化倍率的正分子。
	// 它只与对应分母、非空属性 Identifier 一同有效，成功造成真实本体伤害后整套道具运行态会被清空。
	HeldItemConsumableElementDamageBoostNumerator uint16 `json:"heldItemConsumableElementDamageBoostNumerator"`
	// HeldItemConsumableElementDamageBoostDenominator 是一次性属性威力强化倍率的正分母。
	// 零表示不存在该规则，避免把未配置道具误解释为零倍率并使普通伤害公式失效。
	HeldItemConsumableElementDamageBoostDenominator uint16 `json:"heldItemConsumableElementDamageBoostDenominator"`
	// ContactTransferToAttacker 表示当前持有道具在成员受到有效接触本体伤害后会转移给无道具攻击者。
	// 该开关随 ItemID 生命周期迁移；空 ItemID 时即使字段残留也不能再触发。
	ContactTransferToAttacker bool `json:"contactTransferToAttacker"`
	// ChargeSkipOnce 表示当前持有道具可在首次蓄力技能行动时被消耗，并跳过本次蓄力等待。
	// 空 ItemID 时该字段不能生效，防止已消费或已转移道具被错误重复使用。
	ChargeSkipOnce bool `json:"chargeSkipOnce"`
	// HeldItemSurviveFatalDamageAtFullHP 表示当前持有道具会在成员满生命承受致命对手技能伤害时保留 1 HP。
	// 该规则与特性来源保命分离；触发时消费当前道具及其所有运行时投影，且不会受无视目标特性影响。
	HeldItemSurviveFatalDamageAtFullHP bool `json:"heldItemSurviveFatalDamageAtFullHp"`
	// HeldItemReflectTurnsRemaining 表示当前持有道具建立反射壁时允许提供的最大初始持续回合；0 表示不延长。
	// 实际建立时与技能资料声明值取较大值，且已存在反射壁不会被这项道具规则刷新。
	HeldItemReflectTurnsRemaining uint8 `json:"heldItemReflectTurnsRemaining"`
	// HeldItemLightScreenTurnsRemaining 表示当前持有道具建立光墙时允许提供的最大初始持续回合；0 表示不延长。
	// 它与反射壁和极光幕分别建模，避免把三种独立侧状态压缩为不透明效果集合。
	HeldItemLightScreenTurnsRemaining uint8 `json:"heldItemLightScreenTurnsRemaining"`
	// HeldItemAuroraVeilTurnsRemaining 表示当前持有道具建立极光幕时允许提供的最大初始持续回合；0 表示不延长。
	// 空 ItemID 时该字段不能生效，防止道具被消费或转移后继续延长屏障。
	HeldItemAuroraVeilTurnsRemaining uint8 `json:"heldItemAuroraVeilTurnsRemaining"`
	// HeldItemRainTurnsRemaining 表示当前持有道具建立普通降雨时允许提供的最大初始持续回合；0 表示不延长。
	// 它仅对降雨生效，技能、入场特性和其他来源均在写入环境前使用同一冻结投影。
	HeldItemRainTurnsRemaining uint8 `json:"heldItemRainTurnsRemaining"`
	// HeldItemSandstormTurnsRemaining 表示当前持有道具建立普通沙暴时允许提供的最大初始持续回合；0 表示不延长。
	// 该字段独立于降雨，只有持有者当前仍拥有道具时才会在建立沙暴前生效。
	HeldItemSandstormTurnsRemaining uint8 `json:"heldItemSandstormTurnsRemaining"`
	// HeldItemSnowTurnsRemaining 表示当前持有道具建立普通降雪时允许提供的最大初始持续回合；0 表示不延长。
	// 道具被消费、转移或成员倒下后不能再以此字段延长后续的降雪。
	HeldItemSnowTurnsRemaining uint8 `json:"heldItemSnowTurnsRemaining"`
	// HeldItemSunTurnsRemaining 表示当前持有道具建立普通日照时允许提供的最大初始持续回合；0 表示不延长。
	// 它独立于其它天气的道具字段，防止日照以外的来源获得额外持续回合。
	HeldItemSunTurnsRemaining uint8 `json:"heldItemSunTurnsRemaining"`
	// HeldItemTerrainTurnsRemaining 表示当前持有道具建立任一普通场地时允许提供的最大初始持续回合；0 表示不延长。
	// 它只影响四种标准场地，不能改变天气、强天气或已存在环境。
	HeldItemTerrainTurnsRemaining uint8 `json:"heldItemTerrainTurnsRemaining"`
	// HeldItemSandstormDamageImmunity 表示当前持有道具是否使成员免疫回合末普通沙暴伤害。
	// 它不影响天气本身、其它成员或任何非沙暴的间接伤害；空 ItemID 时该投影无效。
	HeldItemSandstormDamageImmunity bool `json:"heldItemSandstormDamageImmunity"`
	// HeldItemEntryHazardImmunity 表示当前持有道具是否使成员免疫自身换入时的四类入场危害。
	// 它不移除侧状态，也不阻止其它成员结算危害；空 ItemID 时该投影无效。
	HeldItemEntryHazardImmunity bool `json:"heldItemEntryHazardImmunity"`
	// HeldItemWeightHalf 表示当前持有道具是否将成员参与规则计算的有效体重减半。
	// 它不改写 Weight 的权威资料值，且空 ItemID 时不再改变动态威力等体重读取。
	HeldItemWeightHalf bool `json:"heldItemWeightHalf"`
	// HeldItemCuresParalysis 表示当前持有道具是否在成员成功获得麻痹后立即消耗并解除该异常。
	// 它只响应麻痹的实际写入，空 ItemID 时失效，不能作为其它主要异常的通用治疗标志。
	HeldItemCuresParalysis bool `json:"heldItemCuresParalysis"`
	// HeldItemCuresSleep 表示当前持有道具是否在成员成功获得睡眠后立即消耗并解除该异常。
	// 它只响应睡眠的实际写入，并随消费清空 SleepTurnsRemaining，空 ItemID 时不再生效。
	HeldItemCuresSleep bool `json:"heldItemCuresSleep"`
	// HeldItemCuresPoison 表示当前持有道具是否在成员成功获得普通中毒或剧毒后立即消耗并解除该异常。
	// 它只响应两种中毒状态的实际写入，并随消费清空 BadPoisonCounter，空 ItemID 时不再生效。
	HeldItemCuresPoison bool `json:"heldItemCuresPoison"`
	// HeldItemCuresBurn 表示当前持有道具是否在成员成功获得灼伤后立即消耗并解除该异常。
	// 它只响应灼伤的实际写入，空 ItemID 时失效，不能作为其它主要异常的通用治疗标志。
	HeldItemCuresBurn bool `json:"heldItemCuresBurn"`
	// HeldItemCuresFreeze 表示当前持有道具是否在成员成功获得冰冻后立即消耗并解除该异常。
	// 它只响应冰冻的实际写入，空 ItemID 时失效，不能作为其它主要异常的通用治疗标志。
	HeldItemCuresFreeze bool `json:"heldItemCuresFreeze"`
	// HeldItemCuresAllMajorStatuses 表示当前持有道具是否在成员成功获得任一种主要异常后立即消耗并解除该异常。
	// 它覆盖六种主要异常，并在消费时同步清空 SleepTurnsRemaining 和 BadPoisonCounter。
	HeldItemCuresAllMajorStatuses bool `json:"heldItemCuresAllMajorStatuses"`
	// HeldItemCuresConfusion 表示当前持有道具是否在成员成功获得混乱后立即消耗并解除该易变状态。
	// 它只清空 ConfusionTurnsRemaining，空 ItemID 时失效，不能作为其它易变状态的通用治疗标志。
	HeldItemCuresConfusion bool `json:"heldItemCuresConfusion"`
	// HeldItemPunchBasedSkillPowerBoost 表示当前持有道具是否使拳击类技能在直接伤害威力阶段获得固定 10% 强化。
	// 它只在非空 ItemID、普通直接伤害和 SkillSnapshot.PunchBased 同时满足时生效。
	HeldItemPunchBasedSkillPowerBoost bool `json:"heldItemPunchBasedSkillPowerBoost"`
	// HeldItemPhysicalDamagePowerBoost 表示当前持有道具是否把普通物理直接伤害技能的有效威力固定提高 10%。
	HeldItemPhysicalDamagePowerBoost bool `json:"heldItemPhysicalDamagePowerBoost"`
	// HeldItemSpecialDamagePowerBoost 表示当前持有道具是否把普通特殊直接伤害技能的有效威力固定提高 10%。
	HeldItemSpecialDamagePowerBoost bool `json:"heldItemSpecialDamagePowerBoost"`
	// HeldItemElementDamageReductionElementID 是当前一次性抗性道具匹配的技能有效属性稳定 Identifier。
	HeldItemElementDamageReductionElementID Identifier `json:"heldItemElementDamageReductionElementId,omitempty"`
	// HeldItemElementDamageReductionRequiresSuperEffective 表示该抗性道具是否还要求技能严格克制当前成员。
	HeldItemElementDamageReductionRequiresSuperEffective bool `json:"heldItemElementDamageReductionRequiresSuperEffective"`
	// HeldItemSuperEffectiveDamageBoost 表示持有道具是否把效果绝佳的普通直接伤害固定提高 20%。
	HeldItemSuperEffectiveDamageBoost bool `json:"heldItemSuperEffectiveDamageBoost"`
	// HeldItemDamageBoostWithRecoil 表示持有道具是否强化普通直接伤害 30% 并在造成伤害后反伤。
	HeldItemDamageBoostWithRecoil bool `json:"heldItemDamageBoostWithRecoil"`
	// HeldItemDamageDealtHeal 表示持有道具在成员造成实际伤害后回复伤害量的八分之一。
	HeldItemDamageDealtHeal bool `json:"heldItemDamageDealtHeal"`
	// HeldItemDrainHealingBoost 表示持有道具把吸取回复量固定提高 30%。
	HeldItemDrainHealingBoost bool `json:"heldItemDrainHealingBoost"`
	// HeldItemAccuracyBoost 表示持有道具把普通技能命中率乘以 11/10。
	HeldItemAccuracyBoost bool `json:"heldItemAccuracyBoost"`
	// HeldItemOpponentAccuracyReduction 表示持有道具把对手针对自己的普通技能命中率乘以 9/10。
	HeldItemOpponentAccuracyReduction bool `json:"heldItemOpponentAccuracyReduction"`
	// HeldItemCriticalHitStageBoost 表示持有道具为普通要害判定增加一级。
	HeldItemCriticalHitStageBoost bool `json:"heldItemCriticalHitStageBoost"`
	// HeldItemAirborneUntilDamaged 表示持有道具让成员视为空中，直到首次承受本体伤害后关闭。
	HeldItemAirborneUntilDamaged bool `json:"heldItemAirborneUntilDamaged"`
	// HeldItemForceGrounded 表示持有道具强制成员视为接地。
	HeldItemForceGrounded bool `json:"heldItemForceGrounded"`
	// HeldItemSpeedHalf 表示持有道具把行动排序速度减半。
	HeldItemSpeedHalf bool `json:"heldItemSpeedHalf"`
	// HeldItemSpecialDefenseBoost 表示持有道具把普通特殊伤害公式中的特防乘以 3/2。
	HeldItemSpecialDefenseBoost bool `json:"heldItemSpecialDefenseBoost"`
	// HeldItemStatusSkillRestriction 表示持有道具禁止成员选择变化技能。
	HeldItemStatusSkillRestriction bool `json:"heldItemStatusSkillRestriction"`
	// HeldItemPhysicalDamagePowerBoost50 表示讲究头带把普通物理技能威力提高 50%。
	HeldItemPhysicalDamagePowerBoost50 bool `json:"heldItemPhysicalDamagePowerBoost50"`
	// HeldItemSpecialDamagePowerBoost50 表示讲究眼镜把普通特殊技能威力提高 50%。
	HeldItemSpecialDamagePowerBoost50 bool `json:"heldItemSpecialDamagePowerBoost50"`
	// HeldItemChoiceSkillLock 表示道具会把成员限制在首次实际宣告的技能槽。
	HeldItemChoiceSkillLock bool `json:"heldItemChoiceSkillLock"`
	// HeldItemChoiceLockedSkillPosition 是讲究类道具当前锁定的技能槽；0 表示尚未宣告技能。
	HeldItemChoiceLockedSkillPosition SkillPosition `json:"heldItemChoiceLockedSkillPosition"`
	// HeldItemSpeedBoost50 表示讲究围巾把行动排序速度提高 50%。
	HeldItemSpeedBoost50 bool `json:"heldItemSpeedBoost50"`
	// HeldItemAccuracyAfterTargetActedBoost 表示目标已行动时把普通命中率提高 20%。
	HeldItemAccuracyAfterTargetActedBoost bool `json:"heldItemAccuracyAfterTargetActedBoost"`
	// HeldItemTypeImmunitySuppression 表示道具让持有者自身属性提供的伤害免疫失效。
	HeldItemTypeImmunitySuppression bool `json:"heldItemTypeImmunitySuppression"`
	// HeldItemOpponentStatStageReductionImmunity 表示道具阻止对手降低持有者能力阶级。
	HeldItemOpponentStatStageReductionImmunity bool `json:"heldItemOpponentStatStageReductionImmunity"`
	// HeldItemNegativeStatStageReset 表示实际下降后清零全部负能力阶级并消费道具。
	HeldItemNegativeStatStageReset bool `json:"heldItemNegativeStatStageReset"`
	// HeldItemAbilityStatReductionSpeedBoost 表示对手入场特性降低能力后提升速度并消费道具。
	HeldItemAbilityStatReductionSpeedBoost bool `json:"heldItemAbilityStatReductionSpeedBoost"`
	// HeldItemOpponentPositiveStatStageCopy 表示对手因技能提升能力后复制其正向能力阶级并消费道具。
	HeldItemOpponentPositiveStatStageCopy bool `json:"heldItemOpponentPositiveStatStageCopy"`
	// HeldItemDamagingSkillSecondaryEffectImmunity 表示道具阻止伤害技能写入目标侧追加效果。
	HeldItemDamagingSkillSecondaryEffectImmunity bool `json:"heldItemDamagingSkillSecondaryEffectImmunity"`
	// HeldItemBindingTurns 表示道具建立束缚时覆盖的固定持续次数；0 表示沿用技能资料区间。
	HeldItemBindingTurns uint8 `json:"heldItemBindingTurns"`
	// HeldItemBindingDamageDenominator 表示道具建立束缚时冻结的回合末伤害分母；0 表示默认八分之一。
	HeldItemBindingDamageDenominator uint16 `json:"heldItemBindingDamageDenominator"`
	// HeldItemAccuracyMissStatStageBoostStat 是技能因普通命中判定落空后提升的能力项。
	HeldItemAccuracyMissStatStageBoostStat Stat `json:"heldItemAccuracyMissStatStageBoostStat"`
	// HeldItemAccuracyMissStatStageBoostDelta 是技能因普通命中判定落空后应用的正阶级变化量。
	HeldItemAccuracyMissStatStageBoostDelta int8 `json:"heldItemAccuracyMissStatStageBoostDelta"`
	// HeldItemWeaknessPolicy 表示承受效果绝佳真实本体技能伤害后攻击与特攻各提升两级并消费道具。
	HeldItemWeaknessPolicy bool `json:"heldItemWeaknessPolicy"`
	// HeldItemWaterDamageSpecialAttackBoostElementID 是触发球根类特攻强化的水属性稳定 Identifier；空值表示无规则。
	HeldItemWaterDamageSpecialAttackBoostElementID Identifier `json:"heldItemWaterDamageSpecialAttackBoostElementId,omitempty"`
	// HeldItemElectricDamageAttackBoostElementID 是触发充电电池类攻击强化的电属性稳定 Identifier；空值表示无规则。
	HeldItemElectricDamageAttackBoostElementID Identifier `json:"heldItemElectricDamageAttackBoostElementId,omitempty"`
	// HeldItemWaterDamageSpecialDefenseBoostElementID 是触发光苔类特防强化的水属性稳定 Identifier；空值表示无规则。
	HeldItemWaterDamageSpecialDefenseBoostElementID Identifier `json:"heldItemWaterDamageSpecialDefenseBoostElementId,omitempty"`
	// HeldItemIceDamageAttackBoostElementID 是触发雪球类攻击强化的冰属性稳定 Identifier；空值表示无规则。
	HeldItemIceDamageAttackBoostElementID Identifier `json:"heldItemIceDamageAttackBoostElementId,omitempty"`
	// HeldItemAdditionalFlinchChancePercent 是伤害技能命中后由当前道具追加的畏缩概率。
	HeldItemAdditionalFlinchChancePercent uint8 `json:"heldItemAdditionalFlinchChancePercent"`
	// HeldItemRandomActionOrderBoostChancePercent 是同优先度技能行动进入先行层的非消费概率。
	HeldItemRandomActionOrderBoostChancePercent uint8 `json:"heldItemRandomActionOrderBoostChancePercent"`
	// HeldItemForcedLastActionOrder 表示技能行动在同优先度内强制进入最后行动层。
	HeldItemForcedLastActionOrder bool `json:"heldItemForcedLastActionOrder"`
	// HeldItemLowHPActionOrderBoost 表示生命不高于四分之一时在所有技能行动前消费并进入先行层。
	HeldItemLowHPActionOrderBoost bool `json:"heldItemLowHpActionOrderBoost"`
	// HeldItemFieldSpeedOrderSpeedStageDrop 表示戏法空间成功建立后速度下降一级并在实际下降后消费道具。
	HeldItemFieldSpeedOrderSpeedStageDrop bool `json:"heldItemFieldSpeedOrderSpeedStageDrop"`
	// HeldItemConsecutiveSkillDamageBoost 表示连续成功使用同一技能时按次数递增普通伤害威力。
	HeldItemConsecutiveSkillDamageBoost bool `json:"heldItemConsecutiveSkillDamageBoost"`
	// HeldItemPunchBasedContactSuppression 表示当前持有道具是否使拳击类接触技能在本次行动中不再构成有效接触。
	// 它不改写技能的静态标签，且仅在 ItemID 非空、MakesContact 与 PunchBased 同时为 true 时生效。
	HeldItemPunchBasedContactSuppression bool `json:"heldItemPunchBasedContactSuppression"`
	// HeldItemPowderSkillImmunity 表示当前持有道具是否在命中前阻止带 PowderBased 标签的技能影响本成员。
	// 它要求 ItemID 非空，不影响技能宣告或 PP 消耗，且不会因攻击方无视目标特性而被绕过。
	HeldItemPowderSkillImmunity bool `json:"heldItemPowderSkillImmunity"`
	// HeldItemMultiHitCountMinimum 是当前持有道具将匹配多段技能收窄后的实际段数下界；0 表示未声明该规则。
	// 它必须与最大值和原始技能区间一同解释，不能单独把任意多段技能固定到某个段数。
	HeldItemMultiHitCountMinimum uint8 `json:"heldItemMultiHitCountMinimum"`
	// HeldItemMultiHitCountMaximum 是当前持有道具将匹配多段技能收窄后的实际段数上界；0 表示未声明该规则。
	HeldItemMultiHitCountMaximum uint8 `json:"heldItemMultiHitCountMaximum"`
	// HeldItemMultiHitRequiredMinimum 是道具能够覆盖的技能原始段数下界；0 表示未声明该规则。
	HeldItemMultiHitRequiredMinimum uint8 `json:"heldItemMultiHitRequiredMinimum"`
	// HeldItemMultiHitRequiredMaximum 是道具能够覆盖的技能原始段数上界；0 表示未声明该规则。
	HeldItemMultiHitRequiredMaximum uint8 `json:"heldItemMultiHitRequiredMaximum"`
	// BoosterEnergyStat 是已消耗最高原始能力强化道具后持续被强化的能力项；空值表示本道具尚未激活。
	// 它在道具消失后仍存在，且必须是攻击、防御、特攻、特防或速度之一；匹配环境特性激活时由环境规则优先覆盖。
	BoosterEnergyStat Stat `json:"boosterEnergyStat,omitempty"`
	// HeldItemElementID 是当前道具提供属性伤害强化时冻结的属性稳定 Identifier；空字符串表示该道具不提供此身份。
	//
	// 它是 Item Metadata 在 Battle 启动时的运行时投影，不表示任何伤害倍率，也不会因资料维护而在运行中改变。
	HeldItemElementID Identifier `json:"heldItemElementId,omitempty"`
	// HeldItemElementIdentityBaseElementIDs 是本次连续上场中，被携带道具属性身份覆盖前的自然属性集合。
	// 非空表示身份替换当前生效；成员离场、变身还原或复制到不含此规则的特性时必须用它恢复自然属性。
	HeldItemElementIdentityBaseElementIDs []Identifier `json:"heldItemElementIdentityBaseElementIds,omitempty"`
}

// MemberTransformSnapshot 保存一名成员变身前、需要在离开场地时恢复的战斗画像。
//
// 它故意只包含变身会覆盖的字段。生命值、主要异常、道具和其它连续战斗状态仍属于原成员，不能在还原时
// 回退到变身发生前的旧值；相反，技能剩余 PP 与特性规则属于被覆盖画像的一部分，必须完整恢复。
type MemberTransformSnapshot struct {
	// CreatureID 是成员变身前的种类稳定 Identifier。
	CreatureID Identifier `json:"creatureId"`
	// Stats 是成员变身前的五项非生命基础能力。
	Stats StatBlock `json:"stats"`
	// Weight 是成员变身前的冻结体重。
	Weight uint32 `json:"weight"`
	// ElementIDs 是成员变身前的一至两个属性稳定 Identifier。
	ElementIDs []Identifier `json:"elementIds"`
	// NaturalElementIDs 是成员变身前自然形态的一至两个属性稳定 Identifier。
	// 已太晶化成员的 ElementIDs 是唯一太晶属性，因而必须单独保存本字段，才能在变身还原后继续保持正确的
	// 自然属性基线。
	NaturalElementIDs []Identifier `json:"naturalElementIds"`
	// Skills 是成员变身前的技能快照及其当时剩余 PP。
	Skills []SkillSnapshot `json:"skills"`
	// AbilityID 是成员变身前的特性稳定 Identifier；空字符串表示没有特性。
	AbilityID Identifier `json:"abilityId,omitempty"`
	// WeatherDamageImmunities 是变身前特性提供的天气伤害免疫集合。
	WeatherDamageImmunities []WeatherKind `json:"weatherDamageImmunities"`
	// WeatherEffectsSuppressed 是变身前特性是否封锁普通天气可执行效果。
	WeatherEffectsSuppressed bool `json:"weatherEffectsSuppressed"`
	// ReactiveAbilityRules 是变身前冻结的回合末、受伤和倒下触发特性规则。
	ReactiveAbilityRules *ReactiveAbilityRules `json:"reactiveAbilityRules,omitempty"`
	// BasePowerAtMostDamageBoost 是变身前特性的低基础威力技能最终伤害倍率规则。
	BasePowerAtMostDamageBoost *BasePowerAtMostDamageBoost `json:"basePowerAtMostDamageBoost,omitempty"`
	// RecoilSkillDamageBoost 是变身前特性的按实际伤害反作用技能最终伤害倍率规则。
	RecoilSkillDamageBoost *RecoilSkillDamageBoost `json:"recoilSkillDamageBoost,omitempty"`
	// LowHPElementDamageBoost 是变身前特性的低生命指定属性最终伤害倍率规则。
	LowHPElementDamageBoost *LowHPElementDamageBoost `json:"lowHpElementDamageBoost,omitempty"`
	// WeatherElementDamageBoost 是变身前特性的指定天气属性最终伤害倍率规则。
	WeatherElementDamageBoost *WeatherElementDamageBoost `json:"weatherElementDamageBoost,omitempty"`
	// ElementSkillDamageBoost 是变身前特性的一组有效属性最终伤害倍率规则。
	ElementSkillDamageBoost *ElementSkillDamageBoost `json:"elementSkillDamageBoost,omitempty"`
	// SameElementBonusOverride 是变身前特性替换默认属性一致加成的倍率规则。
	SameElementBonusOverride *SameElementBonusOverride `json:"sameElementBonusOverride,omitempty"`
	// ContactBasedSkillDamageBoost 是变身前特性对有效接触技能的最终伤害倍率规则。
	ContactBasedSkillDamageBoost *ContactBasedSkillDamageBoost `json:"contactBasedSkillDamageBoost,omitempty"`
	// CriticalHitDamageBoost 是变身前特性对实际击中要害的额外最终伤害倍率规则。
	CriticalHitDamageBoost *CriticalHitDamageBoost `json:"criticalHitDamageBoost,omitempty"`
	// SuperEffectiveDamageBoost 是变身前特性对最终严格克制技能的最终伤害倍率规则。
	SuperEffectiveDamageBoost *SuperEffectiveDamageBoost `json:"superEffectiveDamageBoost,omitempty"`
	// NotVeryEffectiveDamageBoost 是变身前特性对最终非零抗性技能的最终伤害倍率规则。
	NotVeryEffectiveDamageBoost *NotVeryEffectiveDamageBoost `json:"notVeryEffectiveDamageBoost,omitempty"`
	// TargetGenderDamageMultiplier 是变身前特性按双方性别关系修正最终伤害的规则。
	TargetGenderDamageMultiplier *TargetGenderDamageMultiplier `json:"targetGenderDamageMultiplier,omitempty"`
	// PunchBasedSkillDamageBoost 是变身前特性的拳击类技能伤害强化规则。
	PunchBasedSkillDamageBoost *PunchBasedSkillDamageBoost `json:"punchBasedSkillDamageBoost,omitempty"`
	// SlicingBasedSkillDamageBoost 是变身前特性的切割类技能伤害强化规则。
	SlicingBasedSkillDamageBoost *SlicingBasedSkillDamageBoost `json:"slicingBasedSkillDamageBoost,omitempty"`
	// SoundBasedSkillDamageBoost 是变身前特性的声音类技能伤害强化规则。
	SoundBasedSkillDamageBoost *SoundBasedSkillDamageBoost `json:"soundBasedSkillDamageBoost,omitempty"`
	// PulseBasedSkillDamageBoost 是变身前特性的波动类技能伤害强化规则。
	PulseBasedSkillDamageBoost *PulseBasedSkillDamageBoost `json:"pulseBasedSkillDamageBoost,omitempty"`
	// BiteBasedSkillDamageBoost 是变身前特性的啃咬类技能伤害强化规则。
	BiteBasedSkillDamageBoost *BiteBasedSkillDamageBoost `json:"biteBasedSkillDamageBoost,omitempty"`
	// SecondaryEffectsSuppressedDamageBoost 是变身前特性的附加效果抑制伤害强化规则。
	SecondaryEffectsSuppressedDamageBoost *SecondaryEffectsSuppressedDamageBoost `json:"secondaryEffectsSuppressedDamageBoost,omitempty"`
	// SoundBasedSkillDamageReduction 是变身前特性的声音类技能伤害减免规则。
	SoundBasedSkillDamageReduction *SoundBasedSkillDamageReduction `json:"soundBasedSkillDamageReduction,omitempty"`
	// SuperEffectiveDamageReduction 是变身前特性的严格克制伤害减免规则。
	SuperEffectiveDamageReduction *SuperEffectiveDamageReduction `json:"superEffectiveDamageReduction,omitempty"`
	// FullHPDamageReduction 是变身前特性的满生命伤害减免规则。
	FullHPDamageReduction *FullHPDamageReduction `json:"fullHpDamageReduction,omitempty"`
	// DamageClassDamageReduction 是变身前特性的伤害分类减免规则。
	DamageClassDamageReduction *DamageClassDamageReduction `json:"damageClassDamageReduction,omitempty"`
	// ElementSkillDamageReduction 是变身前特性的属性技能减免规则。
	ElementSkillDamageReduction *ElementSkillDamageReduction `json:"elementSkillDamageReduction,omitempty"`
	// ContactBasedSkillDamageReduction 是变身前特性的有效接触技能伤害减免规则。
	ContactBasedSkillDamageReduction *ContactBasedSkillDamageReduction `json:"contactBasedSkillDamageReduction,omitempty"`
	// AttackingStatMultiplier 是变身前特性的攻击侧能力倍率规则。
	AttackingStatMultiplier *AttackingStatMultiplier `json:"attackingStatMultiplier,omitempty"`
	// OpponentAttackingStatMultiplier 是变身前特性对攻击者公式能力的倍率规则。
	OpponentAttackingStatMultiplier *OpponentAttackingStatMultiplier `json:"opponentAttackingStatMultiplier,omitempty"`
	// DefendingStatMultiplier 是变身前特性的防守侧能力倍率规则。
	DefendingStatMultiplier *DefendingStatMultiplier `json:"defendingStatMultiplier,omitempty"`
	// OpponentDefendingStatMultiplier 是变身前特性对目标防御能力的倍率规则。
	OpponentDefendingStatMultiplier *OpponentDefendingStatMultiplier `json:"opponentDefendingStatMultiplier,omitempty"`
	// AllySkillDamageBoost 是变身前特性为上场伙伴提供的分类伤害倍率。
	AllySkillDamageBoost *AllySkillDamageBoost `json:"allySkillDamageBoost,omitempty"`
	// AllyReceivedDamageReduction 是变身前特性为上场伙伴提供的公式承伤倍率。
	AllyReceivedDamageReduction *AllyReceivedDamageReduction `json:"allyReceivedDamageReduction,omitempty"`
	// AllyAbilityGroupCode 是变身前特性声明的互助组稳定代码。
	AllyAbilityGroupCode string `json:"allyAbilityGroupCode,omitempty"`
	// AllyAbilityPresenceAttackingStatMultiplier 是变身前特性的伙伴互助组攻击能力倍率。
	AllyAbilityPresenceAttackingStatMultiplier *AllyAbilityPresenceAttackingStatMultiplier `json:"allyAbilityPresenceAttackingStatMultiplier,omitempty"`
	// AccuracyMultiplier 是变身前特性对任意技能命中率的整数分数修正。
	AccuracyMultiplier *AccuracyMultiplier `json:"accuracyMultiplier,omitempty"`
	// PhysicalSkillAccuracyMultiplier 是变身前特性对物理技能命中率的整数分数修正。
	PhysicalSkillAccuracyMultiplier *AccuracyMultiplier `json:"physicalSkillAccuracyMultiplier,omitempty"`
	// OpponentAccuracySandstormMultiplier 是变身前特性在普通沙暴中降低对手命中率的规则。
	OpponentAccuracySandstormMultiplier *AccuracyMultiplier `json:"opponentAccuracySandstormMultiplier,omitempty"`
	// OpponentAccuracySnowMultiplier 是变身前特性在普通降雪中降低对手命中率的规则。
	OpponentAccuracySnowMultiplier *AccuracyMultiplier `json:"opponentAccuracySnowMultiplier,omitempty"`
	// OpponentAccuracyConfusionMultiplier 是变身前特性在持有者混乱时降低对手命中率的规则。
	OpponentAccuracyConfusionMultiplier *AccuracyMultiplier `json:"opponentAccuracyConfusionMultiplier,omitempty"`
	// AccuracyAlwaysHits 是变身前特性是否跳过普通命中判定。
	AccuracyAlwaysHits bool `json:"accuracyAlwaysHits"`
	// StatusSkillAccuracyCap 是变身前特性施加的变化技能最终命中上限。
	StatusSkillAccuracyCap uint8 `json:"statusSkillAccuracyCap"`
	// IgnoreOpponentAccuracyStatStages 是变身前特性是否无视对手命中或闪避阶级。
	IgnoreOpponentAccuracyStatStages bool `json:"ignoreOpponentAccuracyStatStages"`
	// CriticalHitImmunity 是变身前特性是否免疫对手技能造成的击中要害。
	CriticalHitImmunity bool `json:"criticalHitImmunity"`
	// SkillRecoilDamageImmunity 是变身前特性是否免疫按实际伤害计算的技能反作用。
	SkillRecoilDamageImmunity bool `json:"skillRecoilDamageImmunity"`
	// IndirectDamageImmunity 是变身前特性是否免疫非技能直接伤害。
	// 变身还原必须恢复该开关，避免复制的特性在原画像恢复后遗留间接伤害免疫。
	IndirectDamageImmunity bool `json:"indirectDamageImmunity"`
	// ContactDamageToAttackerDenominator 是变身前特性向攻击者施加接触反制伤害的固定比例分母。
	// 0 表示变身前没有这项反制规则。
	ContactDamageToAttackerDenominator uint16 `json:"contactDamageToAttackerDenominator"`
	// IgnoreOpponentDamageStatStages 是变身前特性是否无视对手伤害能力阶级。
	IgnoreOpponentDamageStatStages bool `json:"ignoreOpponentDamageStatStages"`
	// IgnoreTargetAbilityEffects 是变身前特性是否在使用技能时无视目标防守特性。
	IgnoreTargetAbilityEffects bool `json:"ignoreTargetAbilityEffects"`
	// SurviveFatalDamageAtFullHP 是变身前特性是否让满生命成员承受致命对手技能伤害后保留 1 HP。
	SurviveFatalDamageAtFullHP bool `json:"surviveFatalDamageAtFullHp"`
	// OpponentStatusSkillImmunity 是变身前特性是否免疫对手使用的变化技能。
	OpponentStatusSkillImmunity bool `json:"opponentStatusSkillImmunity"`
	// NonSuperEffectiveDamageImmunity 是变身前特性是否免疫属性相性不克制的对手伤害技能。
	NonSuperEffectiveDamageImmunity bool `json:"nonSuperEffectiveDamageImmunity"`
	// CriticalHitStageBoost 是变身前特性固定赋予成员的击中要害等级增益。
	CriticalHitStageBoost uint8 `json:"criticalHitStageBoost"`
	// MultiHitMaximum 是变身前特性是否让可变连续命中技能固定采用最大段数。
	MultiHitMaximum bool `json:"multiHitMaximum"`
	// DamagingSkillSecondaryEffectImmunity 是变身前特性是否免疫对手伤害技能施加的追加效果。
	DamagingSkillSecondaryEffectImmunity bool `json:"damagingSkillSecondaryEffectImmunity"`
	// PriorityMoveImmunityForSideEnabled 是变身前特性是否阻止对手正优先度技能影响本方场上成员。
	// 变身结束必须恢复此字段，避免复制的特性在恢复原貌后遗留错误的侧保护规则。
	PriorityMoveImmunityForSideEnabled bool `json:"priorityMoveImmunityForSideEnabled"`
	// PriorityMoveImmunityForSideProtectsAllies 是变身前先制技能侧免疫是否保护当前上场同侧伙伴。
	PriorityMoveImmunityForSideProtectsAllies bool `json:"priorityMoveImmunityForSideProtectsAllies"`
	// StatusSkillMovesLastAndIgnoresTargetAbility 是变身前特性是否使变化技能后置并无视目标防守特性。
	StatusSkillMovesLastAndIgnoresTargetAbility bool `json:"statusSkillMovesLastAndIgnoresTargetAbility"`
	// ContactSkillProtectionBypass 是变身前特性是否允许接触类对手技能绕过目标个人保护。
	// 离场或变身还原时必须恢复它，避免复制特性在原特性恢复后错误遗留穿透能力。
	ContactSkillProtectionBypass bool `json:"contactSkillProtectionBypass"`
	// ContactSkillProtectionBypassDamageMultiplier 是变身前成功穿透个人保护时使用的独立伤害倍率。
	ContactSkillProtectionBypassDamageMultiplier *DamageFraction `json:"contactSkillProtectionBypassDamageMultiplier,omitempty"`
	// SkillWeatherOverride 是变身前成员使用技能时观察到的普通天气语义。
	SkillWeatherOverride WeatherKind `json:"skillWeatherOverride,omitempty"`
	// SkillElementConversion 是变身前成员使用技能时执行的属性转换及转换专属倍率。
	SkillElementConversion *SkillElementConversion `json:"skillElementConversion,omitempty"`
	// ContactSuppression 是变身前特性是否使持有成员的接触类技能失去有效接触事实。
	// 它必须在变身还原时恢复，防止复制到的特性让原技能静态接触标签长期失效。
	ContactSuppression bool `json:"contactSuppression"`
	// ReceivedContactDamageHalved 是变身前特性是否使持有成员受到有效接触伤害时最终伤害减半。
	// 变身还原必须恢复这一目标侧防守规则，防止复制的特性在原画像恢复后遗留减伤。
	ReceivedContactDamageHalved bool `json:"receivedContactDamageHalved"`
	// ReceivedFireDamageDoubled 是变身前特性是否使持有成员受到火属性技能伤害时最终伤害翻倍。
	// 变身还原必须恢复这一目标侧弱点规则，防止复制的特性在原画像恢复后遗留翻倍伤害。
	ReceivedFireDamageDoubled bool `json:"receivedFireDamageDoubled"`
	// ForcedSwitchImmunity 是变身前特性是否阻止持有成员被技能或道具强制换下。
	ForcedSwitchImmunity bool `json:"forcedSwitchImmunity"`
	// OpponentSwitchRestriction 是变身前特性施加给敌方主动换人的限制规则。
	OpponentSwitchRestriction *OpponentSwitchRestriction `json:"opponentSwitchRestriction,omitempty"`
	// DamageCrossedHalfHPForceSelfSwitch 是变身前特性的半血跨越强制自换开关。
	DamageCrossedHalfHPForceSelfSwitch bool `json:"damageCrossedHalfHpForceSelfSwitch"`
	// SwitchOutMajorStatusCure 是变身前特性的成功离场主要异常净化开关。
	SwitchOutMajorStatusCure bool `json:"switchOutMajorStatusCure"`
	// SwitchOutHealDenominator 是变身前特性的成功离场固定比例回复分母；0 表示没有该规则。
	SwitchOutHealDenominator uint16 `json:"switchOutHealDenominator"`
	// WeatherEndTurnHealing 是变身前特性提供的普通天气回合末回复规则。
	WeatherEndTurnHealing *WeatherEndTurnHealing `json:"weatherEndTurnHealing,omitempty"`
	// WeatherSpeedMultipliers 是变身前特性提供的普通天气速度倍率集合。
	WeatherSpeedMultipliers []WeatherSpeedMultiplier `json:"weatherSpeedMultipliers"`
	// EnvironmentHighestStatMultiplier 是变身前特性按环境强化最高原始能力的规则。
	// 它必须随变身画像一起恢复，避免成员离场后继续保留被复制种类的环境强化效果。
	EnvironmentHighestStatMultiplier *EnvironmentHighestStatMultiplier `json:"environmentHighestStatMultiplier,omitempty"`
	// TerastallizationStatStageChange 是变身前特性提供的太晶化自身能力阶级变化规则。
	TerastallizationStatStageChange *TerastallizationStatStageChange `json:"terastallizationStatStageChange,omitempty"`
	// TerastallizationEnvironmentClear 是变身前特性是否在太晶化时清除普通天气和普通场地。
	TerastallizationEnvironmentClear bool `json:"terastallizationEnvironmentClear"`
	// SwitchInStrongWeather 是变身前特性的入场强天气规则。
	SwitchInStrongWeather StrongWeatherKind `json:"switchInStrongWeather,omitempty"`
	// SwitchInWeather 是变身前特性的入场普通天气规则。
	SwitchInWeather *SwitchInWeather `json:"switchInWeather,omitempty"`
	// SwitchInTerrain 是变身前特性的入场普通场地规则。
	SwitchInTerrain *SwitchInTerrain `json:"switchInTerrain,omitempty"`
	// SwitchInStatStageChange 是变身前特性的入场能力阶级变化规则。
	SwitchInStatStageChange *SwitchInStatStageChange `json:"switchInStatStageChange,omitempty"`
	// SwitchInAllyHeal 是变身前特性的入场同侧回复规则。
	SwitchInAllyHeal *SwitchInAllyHeal `json:"switchInAllyHeal,omitempty"`
	// SwitchInOpponentDefenseComparisonBoost 是变身前特性的入场防御比较强化开关。
	SwitchInOpponentDefenseComparisonBoost bool `json:"switchInOpponentDefenseComparisonBoost"`
	// SwitchInAllyStatStageCopy 是变身前特性的入场同侧能力阶级复制开关。
	SwitchInAllyStatStageCopy bool `json:"switchInAllyStatStageCopy"`
	// SwitchInAllyStatStageReset 是变身前特性的入场同侧能力阶级重置开关。
	SwitchInAllyStatStageReset bool `json:"switchInAllyStatStageReset"`
	// SwitchInClearAllSideDamageReductions 是变身前特性的入场双方减伤屏障清除开关。
	SwitchInClearAllSideDamageReductions bool `json:"switchInClearAllSideDamageReductions"`
	// SwitchInCopyOpponentAbility 是变身前特性的入场复制对手特性开关。
	SwitchInCopyOpponentAbility bool `json:"switchInCopyOpponentAbility"`
	// SwitchInRevealOpponentHeldItems 是变身前特性的入场公开对手道具开关。
	SwitchInRevealOpponentHeldItems bool `json:"switchInRevealOpponentHeldItems"`
	// SwitchInRevealOpponentHighestPowerSkill 是变身前特性的入场公开对手最高威力技能开关。
	SwitchInRevealOpponentHighestPowerSkill bool `json:"switchInRevealOpponentHighestPowerSkill"`
	// SwitchInTransformIntoOpponent 是变身前特性的入场复制对手战斗画像开关。
	SwitchInTransformIntoOpponent bool `json:"switchInTransformIntoOpponent"`
	// SwitchInDetectDangerousOpponentSkill 是变身前特性的入场危险技能侦测开关。
	SwitchInDetectDangerousOpponentSkill bool `json:"switchInDetectDangerousOpponentSkill"`
	// SwitchInDisguiseAsLastHealthyAlly 是变身前特性的入场视觉伪装开关。
	SwitchInDisguiseAsLastHealthyAlly bool `json:"switchInDisguiseAsLastHealthyAlly"`
	// SwitchInHeldItemElementIdentity 是变身前特性的入场携带道具属性身份替换开关。
	SwitchInHeldItemElementIdentity bool `json:"switchInHeldItemElementIdentity"`
	// SwitchRestrictionImmunity 是变身前持有道具提供的主动换人限制豁免。
	SwitchRestrictionImmunity bool `json:"switchRestrictionImmunity"`
	// ContactSideEffectImmunity 是变身前持有道具提供的接触反制副作用免疫。
	// 变身还原时必须恢复道具冻结事实，避免复制特性改变道具反制边界。
	ContactSideEffectImmunity bool `json:"contactSideEffectImmunity"`
	// HeldItemContactDamageToAttackerDenominator 是变身前持有道具来源的接触反伤分母。
	// 0 表示变身前的道具没有该规则，离场还原时必须精确恢复这一事实。
	HeldItemContactDamageToAttackerDenominator uint16 `json:"heldItemContactDamageToAttackerDenominator"`
	// HeldItemEndTurnHealDenominator 是变身前持有道具来源的回合末固定比例回复分母。
	// 变身覆盖特性画像时仍必须保留该道具投影，以便离场还原后继续使用变身前的规则事实。
	HeldItemEndTurnHealDenominator uint16 `json:"heldItemEndTurnHealDenominator"`
	// HeldItemEndTurnHealForElementID 是变身前持有道具的属性条件回复所需有效属性稳定 Identifier。
	// 它与分母一同恢复，保证变身还原不会改变成员实际持有道具的触发条件。
	HeldItemEndTurnHealForElementID Identifier `json:"heldItemEndTurnHealForElementId,omitempty"`
	// HeldItemEndTurnHealForElementDenominator 是变身前持有道具的属性条件回合末回复分母。
	// 零值仅表示未声明该效果；非零值必须配合对应属性 Identifier 使用。
	HeldItemEndTurnHealForElementDenominator uint16 `json:"heldItemEndTurnHealForElementDenominator"`
	// HeldItemEndTurnDamageDenominator 是变身前持有道具来源的回合末固定比例自伤分母。
	// 它与回复规则独立保存，避免变身还原后把仍持有的自伤道具错误变为无效果道具。
	HeldItemEndTurnDamageDenominator uint16 `json:"heldItemEndTurnDamageDenominator"`
	// HeldItemEndTurnDamageWithoutElementID 是变身前持有道具的属性条件自伤所排除的有效属性稳定 Identifier。
	// 变身还原必须恢复该条件，避免复制目标画像永久改变成员实际持有道具的触发边界。
	HeldItemEndTurnDamageWithoutElementID Identifier `json:"heldItemEndTurnDamageWithoutElementId,omitempty"`
	// HeldItemEndTurnDamageWithoutElementDenominator 是变身前持有道具的属性条件回合末自伤分母。
	// 零值仅表示未声明该效果；非零值必须配合对应属性 Identifier 使用。
	HeldItemEndTurnDamageWithoutElementDenominator uint16 `json:"heldItemEndTurnDamageWithoutElementDenominator"`
	// HeldItemConsumableElementDamageBoostElementID 是变身前一次性属性威力强化道具要求的技能属性稳定 Identifier。
	// 变身不改变道具所有权，离场还原必须恢复该条件，避免复制特性画像永久改变道具消费边界。
	HeldItemConsumableElementDamageBoostElementID Identifier `json:"heldItemConsumableElementDamageBoostElementId,omitempty"`
	// HeldItemConsumableElementDamageBoostNumerator 是变身前一次性属性威力强化倍率的正分子。
	// 它与分母成对恢复，防止变身还原后发生倍率截断或错误消费。
	HeldItemConsumableElementDamageBoostNumerator uint16 `json:"heldItemConsumableElementDamageBoostNumerator"`
	// HeldItemConsumableElementDamageBoostDenominator 是变身前一次性属性威力强化倍率的正分母。
	// 零值仅表示未声明该效果；有效值必须与属性 Identifier 和正分子共同存在。
	HeldItemConsumableElementDamageBoostDenominator uint16 `json:"heldItemConsumableElementDamageBoostDenominator"`
	// ContactTransferToAttacker 是变身前持有道具是否允许有效接触后转移给攻击者的冻结事实。
	// 变身不改变成员当前持有道具，因此离场还原时必须恢复并保持与 ItemID 一致。
	ContactTransferToAttacker bool `json:"contactTransferToAttacker"`
	// ChargeSkipOnce 是变身前持有道具提供的一次性蓄力跳过资格。
	// 变身期间的道具状态保持连续，离场还原时必须恢复该冻结字段。
	ChargeSkipOnce bool `json:"chargeSkipOnce"`
	// HeldItemSurviveFatalDamageAtFullHP 是变身前持有道具提供的一次性满生命保命资格。
	// 变身不会消耗道具；若变身期间未触发，离场还原必须精确恢复这项运行时投影。
	HeldItemSurviveFatalDamageAtFullHP bool `json:"heldItemSurviveFatalDamageAtFullHp"`
	// HeldItemReflectTurnsRemaining 是变身前持有道具建立反射壁时提供的最大初始持续回合。
	// 变身期间持有道具不变，离场还原时必须恢复这一独立投影。
	HeldItemReflectTurnsRemaining uint8 `json:"heldItemReflectTurnsRemaining"`
	// HeldItemLightScreenTurnsRemaining 是变身前持有道具建立光墙时提供的最大初始持续回合。
	// 它与其它两种屏障分开保存，保持侧状态建立时的精确匹配范围。
	HeldItemLightScreenTurnsRemaining uint8 `json:"heldItemLightScreenTurnsRemaining"`
	// HeldItemAuroraVeilTurnsRemaining 是变身前持有道具建立极光幕时提供的最大初始持续回合。
	// 道具在变身期间被消费或转移时，当前成员状态会先更新，离场还原仅恢复仍权威存在的快照字段。
	HeldItemAuroraVeilTurnsRemaining uint8 `json:"heldItemAuroraVeilTurnsRemaining"`
	// HeldItemRainTurnsRemaining 是变身前持有道具建立普通降雨时提供的最大初始持续回合。
	// 它随 ItemID 生命周期保存与还原，不能在变身复制后通过实时资料重新推断。
	HeldItemRainTurnsRemaining uint8 `json:"heldItemRainTurnsRemaining"`
	// HeldItemSandstormTurnsRemaining 是变身前持有道具建立普通沙暴时提供的最大初始持续回合。
	// 它与道具身份一起保存，保证离场后恢复的是战斗开始时冻结的资料事实。
	HeldItemSandstormTurnsRemaining uint8 `json:"heldItemSandstormTurnsRemaining"`
	// HeldItemSnowTurnsRemaining 是变身前持有道具建立普通降雪时提供的最大初始持续回合。
	// 保存该字段可使变身结束后的道具规则恢复不依赖实时资料读取。
	HeldItemSnowTurnsRemaining uint8 `json:"heldItemSnowTurnsRemaining"`
	// HeldItemSunTurnsRemaining 是变身前持有道具建立普通日照时提供的最大初始持续回合。
	// 它随原始道具投影保存，离场后恢复不会读取可能已经变更的资料。
	HeldItemSunTurnsRemaining uint8 `json:"heldItemSunTurnsRemaining"`
	// HeldItemTerrainTurnsRemaining 是变身前持有道具建立普通场地时提供的最大初始持续回合。
	// 保存该运行时事实后，变身还原和道具转移均不依赖实时资料重算。
	HeldItemTerrainTurnsRemaining uint8 `json:"heldItemTerrainTurnsRemaining"`
	// HeldItemSandstormDamageImmunity 是变身前持有道具提供的回合末沙暴伤害免疫事实。
	// 它随原始道具投影保存和还原，不能在变身期间被实时资料变更重写。
	HeldItemSandstormDamageImmunity bool `json:"heldItemSandstormDamageImmunity"`
	// HeldItemEntryHazardImmunity 是变身前持有道具提供的自身换入危害免疫事实。
	// 保存该字段确保变身结束后恢复的是战斗开始时冻结的道具效果。
	HeldItemEntryHazardImmunity bool `json:"heldItemEntryHazardImmunity"`
	// HeldItemWeightHalf 是变身前持有道具提供的有效体重减半事实。
	// 保存该字段后，变身结束或恢复原始成员时无需读取已经变化的实时道具资料。
	HeldItemWeightHalf bool `json:"heldItemWeightHalf"`
	// HeldItemCuresParalysis 是变身前持有道具在成员获得麻痹后立即消耗并净化的冻结事实。
	// 保存该字段保证变身还原仍遵守本场开始时冻结的道具生命周期，而不查询实时资料。
	HeldItemCuresParalysis bool `json:"heldItemCuresParalysis"`
	// HeldItemCuresSleep 是变身前持有道具在成员获得睡眠后立即消耗并净化的冻结事实。
	// 保存该字段保证变身还原后仍会正确清除睡眠及其回合计数，而不回查实时资料。
	HeldItemCuresSleep bool `json:"heldItemCuresSleep"`
	// HeldItemCuresPoison 是变身前持有道具在成员获得普通中毒或剧毒后立即消耗并净化的冻结事实。
	// 保存该字段保证变身还原后仍会正确清除中毒及剧毒计数，而不回查实时资料。
	HeldItemCuresPoison bool `json:"heldItemCuresPoison"`
	// HeldItemCuresBurn 是变身前持有道具在成员获得灼伤后立即消耗并净化的冻结事实。
	// 保存该字段保证变身还原仍遵守本场开始时冻结的道具生命周期，而不查询实时资料。
	HeldItemCuresBurn bool `json:"heldItemCuresBurn"`
	// HeldItemCuresFreeze 是变身前持有道具在成员获得冰冻后立即消耗并净化的冻结事实。
	// 保存该字段保证变身还原仍遵守本场开始时冻结的道具生命周期，而不查询实时资料。
	HeldItemCuresFreeze bool `json:"heldItemCuresFreeze"`
	// HeldItemCuresAllMajorStatuses 是变身前持有道具对全部主要异常的立即净化冻结事实。
	// 保存该字段保证变身还原后仍按同一场次冻结的状态集合清除异常及其附属计数。
	HeldItemCuresAllMajorStatuses bool `json:"heldItemCuresAllMajorStatuses"`
	// HeldItemCuresConfusion 是变身前持有道具在成员获得混乱后立即消耗并净化的冻结事实。
	// 保存该字段保证变身还原后仍会正确清空混乱持续回合，而不回查实时资料。
	HeldItemCuresConfusion bool `json:"heldItemCuresConfusion"`
	// HeldItemPunchBasedSkillPowerBoost 是变身前持有道具对拳击类技能提供固定威力强化的冻结事实。
	// 保存该字段保证变身还原后仍遵守本场开始时冻结的道具伤害语义，而不查询实时资料。
	HeldItemPunchBasedSkillPowerBoost bool `json:"heldItemPunchBasedSkillPowerBoost"`
	// HeldItemPhysicalDamagePowerBoost 是变身前持有道具对普通物理直接伤害的固定威力强化事实。
	HeldItemPhysicalDamagePowerBoost bool `json:"heldItemPhysicalDamagePowerBoost"`
	// HeldItemSpecialDamagePowerBoost 是变身前持有道具对普通特殊直接伤害的固定威力强化事实。
	HeldItemSpecialDamagePowerBoost bool `json:"heldItemSpecialDamagePowerBoost"`
	// HeldItemElementDamageReductionElementID 是变身前一次性抗性道具匹配的技能属性稳定 Identifier。
	HeldItemElementDamageReductionElementID Identifier `json:"heldItemElementDamageReductionElementId,omitempty"`
	// HeldItemElementDamageReductionRequiresSuperEffective 是变身前抗性道具的严格克制条件。
	HeldItemElementDamageReductionRequiresSuperEffective bool `json:"heldItemElementDamageReductionRequiresSuperEffective"`
	// HeldItemSuperEffectiveDamageBoost 是变身前持有道具的效果绝佳伤害强化事实。
	HeldItemSuperEffectiveDamageBoost bool `json:"heldItemSuperEffectiveDamageBoost"`
	// HeldItemDamageBoostWithRecoil 是变身前持有道具的伤害强化及反伤事实。
	HeldItemDamageBoostWithRecoil bool `json:"heldItemDamageBoostWithRecoil"`
	// HeldItemDamageDealtHeal 是变身前道具的伤害后回复事实。
	HeldItemDamageDealtHeal bool `json:"heldItemDamageDealtHeal"`
	// HeldItemDrainHealingBoost 是变身前道具的吸取回复强化事实。
	HeldItemDrainHealingBoost bool `json:"heldItemDrainHealingBoost"`
	// HeldItemAccuracyBoost 是变身前道具的命中强化事实。
	HeldItemAccuracyBoost bool `json:"heldItemAccuracyBoost"`
	// HeldItemOpponentAccuracyReduction 是变身前道具的对手命中削弱事实。
	HeldItemOpponentAccuracyReduction bool `json:"heldItemOpponentAccuracyReduction"`
	// HeldItemCriticalHitStageBoost 是变身前道具的要害等级强化事实。
	HeldItemCriticalHitStageBoost bool `json:"heldItemCriticalHitStageBoost"`
	// HeldItemAirborneUntilDamaged 是变身前道具的受伤前空中事实。
	HeldItemAirborneUntilDamaged bool `json:"heldItemAirborneUntilDamaged"`
	// HeldItemForceGrounded 是变身前道具的强制接地事实。
	HeldItemForceGrounded bool `json:"heldItemForceGrounded"`
	// HeldItemSpeedHalf 是变身前道具的速度减半事实。
	HeldItemSpeedHalf bool `json:"heldItemSpeedHalf"`
	// HeldItemSpecialDefenseBoost 是变身前道具的特殊防御强化事实。
	HeldItemSpecialDefenseBoost bool `json:"heldItemSpecialDefenseBoost"`
	// HeldItemStatusSkillRestriction 是变身前道具的变化技能选择限制事实。
	HeldItemStatusSkillRestriction bool `json:"heldItemStatusSkillRestriction"`
	// HeldItemPhysicalDamagePowerBoost50 是变身前讲究头带强化事实。
	HeldItemPhysicalDamagePowerBoost50 bool `json:"heldItemPhysicalDamagePowerBoost50"`
	// HeldItemSpecialDamagePowerBoost50 是变身前讲究眼镜强化事实。
	HeldItemSpecialDamagePowerBoost50 bool `json:"heldItemSpecialDamagePowerBoost50"`
	// HeldItemChoiceSkillLock 是变身前讲究类技能选择限制事实。
	HeldItemChoiceSkillLock bool `json:"heldItemChoiceSkillLock"`
	// HeldItemSpeedBoost50 是变身前讲究围巾速度强化事实。
	HeldItemSpeedBoost50 bool `json:"heldItemSpeedBoost50"`
	// HeldItemAccuracyAfterTargetActedBoost 是变身前后手命中强化事实。
	HeldItemAccuracyAfterTargetActedBoost bool `json:"heldItemAccuracyAfterTargetActedBoost"`
	// HeldItemTypeImmunitySuppression 是变身前属性免疫抑制事实。
	HeldItemTypeImmunitySuppression bool `json:"heldItemTypeImmunitySuppression"`
	// HeldItemOpponentStatStageReductionImmunity 是变身前对手降阶免疫事实。
	HeldItemOpponentStatStageReductionImmunity bool `json:"heldItemOpponentStatStageReductionImmunity"`
	// HeldItemNegativeStatStageReset 是变身前负阶级重置事实。
	HeldItemNegativeStatStageReset bool `json:"heldItemNegativeStatStageReset"`
	// HeldItemAbilityStatReductionSpeedBoost 是变身前特性降阶速度补偿事实。
	HeldItemAbilityStatReductionSpeedBoost bool `json:"heldItemAbilityStatReductionSpeedBoost"`
	// HeldItemOpponentPositiveStatStageCopy 是变身前对手正阶级复制事实。
	HeldItemOpponentPositiveStatStageCopy bool `json:"heldItemOpponentPositiveStatStageCopy"`
	// HeldItemDamagingSkillSecondaryEffectImmunity 是变身前道具提供的伤害技能追加效果免疫事实。
	HeldItemDamagingSkillSecondaryEffectImmunity bool `json:"heldItemDamagingSkillSecondaryEffectImmunity"`
	// HeldItemBindingTurns 是变身前道具覆盖的束缚固定持续次数。
	HeldItemBindingTurns uint8 `json:"heldItemBindingTurns"`
	// HeldItemBindingDamageDenominator 是变身前道具覆盖的束缚伤害分母。
	HeldItemBindingDamageDenominator uint16 `json:"heldItemBindingDamageDenominator"`
	// HeldItemAccuracyMissStatStageBoostStat 是变身前打空保险类道具提升的能力项。
	HeldItemAccuracyMissStatStageBoostStat Stat `json:"heldItemAccuracyMissStatStageBoostStat"`
	// HeldItemAccuracyMissStatStageBoostDelta 是变身前打空保险类道具的正阶级变化量。
	HeldItemAccuracyMissStatStageBoostDelta int8 `json:"heldItemAccuracyMissStatStageBoostDelta"`
	// HeldItemWeaknessPolicy 是变身前弱点保险规则开关。
	HeldItemWeaknessPolicy bool `json:"heldItemWeaknessPolicy"`
	// HeldItemWaterDamageSpecialAttackBoostElementID 是变身前球根规则的属性稳定 Identifier。
	HeldItemWaterDamageSpecialAttackBoostElementID Identifier `json:"heldItemWaterDamageSpecialAttackBoostElementId,omitempty"`
	// HeldItemElectricDamageAttackBoostElementID 是变身前充电电池规则的属性稳定 Identifier。
	HeldItemElectricDamageAttackBoostElementID Identifier `json:"heldItemElectricDamageAttackBoostElementId,omitempty"`
	// HeldItemWaterDamageSpecialDefenseBoostElementID 是变身前光苔规则的属性稳定 Identifier。
	HeldItemWaterDamageSpecialDefenseBoostElementID Identifier `json:"heldItemWaterDamageSpecialDefenseBoostElementId,omitempty"`
	// HeldItemIceDamageAttackBoostElementID 是变身前雪球规则的属性稳定 Identifier。
	HeldItemIceDamageAttackBoostElementID Identifier `json:"heldItemIceDamageAttackBoostElementId,omitempty"`
	// HeldItemAdditionalFlinchChancePercent 是变身前道具追加畏缩概率。
	HeldItemAdditionalFlinchChancePercent uint8 `json:"heldItemAdditionalFlinchChancePercent"`
	// HeldItemRandomActionOrderBoostChancePercent 是变身前先制之爪类触发概率。
	HeldItemRandomActionOrderBoostChancePercent uint8 `json:"heldItemRandomActionOrderBoostChancePercent"`
	// HeldItemForcedLastActionOrder 是变身前后攻之尾类规则开关。
	HeldItemForcedLastActionOrder bool `json:"heldItemForcedLastActionOrder"`
	// HeldItemLowHPActionOrderBoost 是变身前释陀果类低生命先行规则开关。
	HeldItemLowHPActionOrderBoost bool `json:"heldItemLowHpActionOrderBoost"`
	// HeldItemFieldSpeedOrderSpeedStageDrop 是变身前客房服务规则开关。
	HeldItemFieldSpeedOrderSpeedStageDrop bool `json:"heldItemFieldSpeedOrderSpeedStageDrop"`
	// HeldItemConsecutiveSkillDamageBoost 是变身前节拍器规则开关。
	HeldItemConsecutiveSkillDamageBoost bool `json:"heldItemConsecutiveSkillDamageBoost"`
	// HeldItemPunchBasedContactSuppression 是变身前持有道具对拳击类接触技能取消有效接触的冻结事实。
	// 保存该字段保证成员还原后仍按本场开始时冻结的道具语义计算接触反制与保护穿透。
	HeldItemPunchBasedContactSuppression bool `json:"heldItemPunchBasedContactSuppression"`
	// HeldItemPowderSkillImmunity 是变身前持有道具对粉末或孢子类技能提供命中前免疫的冻结事实。
	// 保存该字段保证成员还原后不回查实时资料，也不会将已经失去的道具效果残留在成员身上。
	HeldItemPowderSkillImmunity bool `json:"heldItemPowderSkillImmunity"`
	// HeldItemMultiHitCountMinimum 是变身前持有道具对匹配连续命中技能应用后的实际段数下界；0 表示不覆盖。
	// 四个边界必须作为一个整体恢复，避免变身结束后把原成员的道具误解释为固定段数效果。
	HeldItemMultiHitCountMinimum uint8 `json:"heldItemMultiHitCountMinimum"`
	// HeldItemMultiHitCountMaximum 是变身前持有道具对匹配连续命中技能应用后的实际段数上界；0 表示不覆盖。
	HeldItemMultiHitCountMaximum uint8 `json:"heldItemMultiHitCountMaximum"`
	// HeldItemMultiHitRequiredMinimum 是变身前持有道具可以覆盖的技能原始段数下界；0 表示不覆盖。
	HeldItemMultiHitRequiredMinimum uint8 `json:"heldItemMultiHitRequiredMinimum"`
	// HeldItemMultiHitRequiredMaximum 是变身前持有道具可以覆盖的技能原始段数上界；0 表示不覆盖。
	HeldItemMultiHitRequiredMaximum uint8 `json:"heldItemMultiHitRequiredMaximum"`
	// HeldItemElementID 是变身前成员所持道具冻结的属性伤害强化身份。
	HeldItemElementID Identifier `json:"heldItemElementId,omitempty"`
	// HeldItemElementIdentityBaseElementIDs 是变身前已生效的道具属性身份所覆盖的自然属性集合。
	HeldItemElementIdentityBaseElementIDs []Identifier `json:"heldItemElementIdentityBaseElementIds,omitempty"`
	// SwitchInFormChange 是变身前特性的入场确定形态切换规则。
	SwitchInFormChange *SwitchInFormChange `json:"switchInFormChange,omitempty"`
	// SwitchOutFormChange 是变身前特性的成功离场确定形态切换规则。
	SwitchOutFormChange *SwitchOutFormChange `json:"switchOutFormChange,omitempty"`
	// WeatherFormChange 是变身前特性的天气同步形态规则。
	WeatherFormChange *WeatherFormChange `json:"weatherFormChange,omitempty"`
}

// SideSnapshot 是一方队伍及其初始场上成员的权威快照。
type SideSnapshot struct {
	// Side 是该快照所属的稳定阵营位置。
	Side Side `json:"side"`
	// ActiveMembers 按槽位顺序保存当前上场成员位置；下标 0 对应 slotPosition 1。
	ActiveMembers []MemberPosition `json:"activeMembers"`
	// Members 按成员位置保存本场可以参与替换的完整队伍快照。
	Members []MemberSnapshot `json:"members"`
	// Conditions 是只作用于本方、会跨回合持续的权威侧状态。
	//
	// 它不能放入全场环境，也不能附着在某一成员上：成员换下或倒下时，顺风等侧状态仍需保留给同侧其它成员。
	Conditions SideConditionSnapshot `json:"conditions"`
	// TerastallizationUsed 表示该方已消耗本局唯一的太晶化机会。
	// 它属于阵营而非成员：太晶化成员换下后机会仍不能转交给同侧其它成员。
	TerastallizationUsed bool `json:"terastallizationUsed"`
}

// InitialState 是创建权威战斗状态所需的完整纯值输入。
type InitialState struct {
	// Format 是 Battle 创建时冻结的 BattleFormat 执行快照。
	Format FormatSnapshot `json:"format"`
	// Rules 是创建对战时从实时资料冻结的规则快照。
	Rules RuleSnapshot `json:"rules"`
	// Environment 是战斗开始时冻结的全场环境运行态。
	Environment EnvironmentSnapshot `json:"environment"`
	// Sides 必须恰好包含 SideOne 和 SideTwo 各一次。
	Sides []SideSnapshot `json:"sides"`
}

// State 是纯战斗引擎持有的不可变权威状态。
//
// 所有可变长度数据在构造和读取边界都会复制。后续状态转换返回新 State，调用方
// 不应也无法通过创建参数或查询结果修改既有候选状态。
type State struct {
	// format 是本场战斗冻结且不会在回合间变化的赛制快照。
	format FormatSnapshot
	// rules 是本场战斗冻结且不会在回合间变化的规则快照。
	rules RuleSnapshot
	// environment 保存天气、场地及全场速度顺序等不归属于任一阵营的权威运行态。
	environment EnvironmentSnapshot
	// sides 保存双方当前权威队伍、生命值和场上槽位映射。
	sides []SideSnapshot
	// turnNumber 是已经开始结算的回合编号；初始状态固定为 0。
	turnNumber uint32
	// result 保存引擎已经确认的终局事实；nil 表示战斗仍可继续。
	result *BattleResult
	// initialEvents 保存 NewState 初始入场阶段产生、尚未交给 Battle 持久化的公开事件。
	// 这些事件不属于回合状态快照，也不会在 ResolveTurn 中重复返回。
	initialEvents []Event
}

// StateSnapshot 是可写入 Turn Record 或离线重放文件的完整权威状态快照。
//
// 快照只包含语言无关的导出值和 lowerCamelCase JSON 字段，不暴露 Go 内部索引。
type StateSnapshot struct {
	// Format 是本场战斗冻结且不会随回合变化的赛制快照。
	Format FormatSnapshot `json:"format"`
	// Rules 是本场战斗冻结且不会随回合变化的规则快照。
	Rules RuleSnapshot `json:"rules"`
	// Environment 是当前完整全场运行态，包含会跨回合影响行动排序和回合末结算的效果。
	Environment EnvironmentSnapshot `json:"environment"`
	// Sides 按 SideOne、SideTwo 的固定顺序保存双方完整权威运行态。
	Sides []SideSnapshot `json:"sides"`
	// TurnNumber 是该快照已经完成结算的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Result 是引擎已经确认的终局事实；战斗尚未结束时明确编码为 null。
	Result *BattleResult `json:"result"`
}

// NewState 校验并深复制战斗初始快照。
func NewState(initial InitialState) (State, error) {
	if err := validateFormat(initial.Format); err != nil {
		return State{}, err
	}
	if initial.Rules.SchemaVersion != 1 {
		return State{}, fmt.Errorf("%w: rules.schemaVersion=%d", ErrInvalidInitialState, initial.Rules.SchemaVersion)
	}
	if err := validateRuleSnapshot(initial.Rules); err != nil {
		return State{}, err
	}
	if err := validateEnvironment(initial.Environment); err != nil {
		return State{}, err
	}
	if len(initial.Sides) != 2 {
		return State{}, fmt.Errorf("%w: sides 必须恰好包含两方", ErrInvalidInitialState)
	}

	seenSides := make(map[Side]struct{}, 2)
	for index := range initial.Sides {
		side := initial.Sides[index]
		if !side.Side.Valid() {
			return State{}, fmt.Errorf("%w: sides[%d].side=%d", ErrInvalidInitialState, index, side.Side)
		}
		if _, duplicate := seenSides[side.Side]; duplicate {
			return State{}, fmt.Errorf("%w: 阵营 %d 重复", ErrInvalidInitialState, side.Side)
		}
		seenSides[side.Side] = struct{}{}
		if err := validateSide(side, initial.Format, initial.Rules); err != nil {
			return State{}, err
		}
	}

	ownedSides := cloneSides(initial.Sides)
	sort.Slice(ownedSides, func(left, right int) bool {
		return ownedSides[left].Side < ownedSides[right].Side
	})
	for sideIndex := range ownedSides {
		for memberIndex := range ownedSides[sideIndex].Members {
			member := &ownedSides[sideIndex].Members[memberIndex]
			if len(member.NaturalElementIDs) == 0 {
				member.NaturalElementIDs = append([]Identifier(nil), member.ElementIDs...)
			}
		}
	}
	state := State{
		format:      initial.Format,
		rules:       cloneRuleSnapshot(initial.Rules),
		environment: cloneEnvironment(initial.Environment),
		sides:       ownedSides,
	}
	var initialEvents []Event
	var switchInFormEvents []Event
	state, switchInFormEvents = initializeSwitchInFormChanges(state)
	initialEvents = append(initialEvents, switchInFormEvents...)
	state = initializeSwitchInStatStageChanges(state)
	state = initializeSwitchInAllyHeals(state)
	state = initializeSwitchInOpponentDefenseComparisonBoosts(state)
	state = initializeSwitchInAllyStatStageCopies(state)
	state = initializeSwitchInAllyStatStageResets(state)
	state = initializeSwitchInClearAllSideDamageReductions(state)
	state = initializeSwitchInCopyOpponentAbilities(state)
	var opponentHeldItemEvents []Event
	state, opponentHeldItemEvents = initializeSwitchInRevealOpponentHeldItems(state)
	initialEvents = append(initialEvents, opponentHeldItemEvents...)
	var highestPowerSkillEvents []Event
	state, highestPowerSkillEvents = initializeSwitchInRevealOpponentHighestPowerSkill(state)
	initialEvents = append(initialEvents, highestPowerSkillEvents...)
	var transformEvents []Event
	state, transformEvents = initializeSwitchInTransformIntoOpponent(state)
	initialEvents = append(initialEvents, transformEvents...)
	var dangerousSkillEvents []Event
	state, dangerousSkillEvents = initializeSwitchInDetectDangerousOpponentSkill(state)
	initialEvents = append(initialEvents, dangerousSkillEvents...)
	state = initializeSwitchInDisguiseAsLastHealthyAlly(state)
	state = initializeSwitchInWeather(state)
	state = initializeSwitchInTerrain(state)
	state = initializeStrongWeather(state)
	var weatherFormEvents []Event
	state, weatherFormEvents = synchronizeWeatherForms(state)
	initialEvents = append(initialEvents, weatherFormEvents...)
	var heldItemElementIdentityEvents []Event
	state, heldItemElementIdentityEvents = initializeSwitchInHeldItemElementIdentities(state)
	initialEvents = append(initialEvents, heldItemElementIdentityEvents...)
	var heldItemHighestStatBoostEvents []Event
	state, heldItemHighestStatBoostEvents = initializeHeldItemHighestStatBoosts(state)
	initialEvents = append(initialEvents, heldItemHighestStatBoostEvents...)
	state.initialEvents = initialEvents
	return state, nil
}

// TurnNumber 返回当前权威状态的回合编号。
func (state State) TurnNumber() uint32 {
	return state.turnNumber
}

// Result 返回引擎已经确认的终局结果。
//
// 第二个返回值为 false 表示战斗尚未结束，调用方不能从零值推断平局或胜方。
func (state State) Result() (BattleResult, bool) {
	if state.result == nil {
		return BattleResult{}, false
	}
	return *state.result, true
}

// Snapshot 返回与当前 State 深度隔离的语言无关权威快照。
func (state State) Snapshot() StateSnapshot {
	return StateSnapshot{
		Format:      state.format,
		Rules:       cloneRuleSnapshot(state.rules),
		Environment: cloneEnvironment(state.environment),
		Sides:       cloneSides(state.sides),
		TurnNumber:  state.turnNumber,
		Result:      cloneBattleResult(state.result),
	}
}

// InitialEvents 返回初始入场阶段产生的结构化公开事件。
//
// Battle 应在创建对战时将这些事件与初始快照一并持久化；后续 ResolveTurn 只返回当前回合事件，避免同一公开
// 结果重复写入。返回的切片与 State 隔离，调用方可以安全修改切片顺序。
func (state State) InitialEvents() []Event {
	return append([]Event(nil), state.initialEvents...)
}

// ActiveMember 返回指定场上槽位当前对应的成员快照。
//
// 返回值中的切片与 State 隔离，调用方修改它不会影响权威状态。
func (state State) ActiveMember(ref SlotRef) (MemberSnapshot, bool) {
	if !ref.Side.Valid() || ref.Position == 0 {
		return MemberSnapshot{}, false
	}
	for _, side := range state.sides {
		if side.Side != ref.Side || int(ref.Position) > len(side.ActiveMembers) {
			continue
		}
		position := side.ActiveMembers[ref.Position-1]
		for _, member := range side.Members {
			if member.Position == position {
				return cloneMember(member), true
			}
		}
	}
	return MemberSnapshot{}, false
}

func validateFormat(format FormatSnapshot) error {
	if strings.TrimSpace(format.Code) == "" {
		return fmt.Errorf("%w: format.code 不能为空", ErrInvalidInitialState)
	}
	if format.ActiveSlotsPerSide < 1 || format.ActiveSlotsPerSide > 2 {
		return fmt.Errorf("%w: format.activeSlotsPerSide=%d", ErrInvalidInitialState, format.ActiveSlotsPerSide)
	}
	if format.TeamSize < uint8(format.ActiveSlotsPerSide) || format.TeamSize > MaximumMembersPerSide {
		return fmt.Errorf("%w: format.teamSize=%d", ErrInvalidInitialState, format.TeamSize)
	}
	return nil
}

func validateRuleSnapshot(rules RuleSnapshot) error {
	seenElementIDs := make(map[Identifier]struct{}, len(rules.ElementIDs))
	for code, elementID := range rules.ElementIDs {
		if strings.TrimSpace(code) == "" || !elementID.IsValid() {
			return fmt.Errorf("%w: rules.elementIds 只能包含非空 code 和 ID", ErrInvalidInitialState)
		}
		if _, duplicate := seenElementIDs[elementID]; duplicate {
			return fmt.Errorf("%w: rules.elementIds 中的属性 ID %q 重复", ErrInvalidInitialState, elementID)
		}
		seenElementIDs[elementID] = struct{}{}
	}
	type elementPair struct {
		// attack 是克制关系中的攻击属性稳定 ID。
		attack Identifier
		// defense 是克制关系中的防守属性稳定 ID。
		defense Identifier
	}
	seen := make(map[elementPair]struct{}, len(rules.ElementEffectiveness))
	for index, effectiveness := range rules.ElementEffectiveness {
		if !effectiveness.AttackElementID.IsValid() ||
			!effectiveness.DefenseElementID.IsValid() || effectiveness.Denominator == 0 {
			return fmt.Errorf("%w: rules.elementEffectiveness[%d] 无效", ErrInvalidInitialState, index)
		}
		pair := elementPair{attack: effectiveness.AttackElementID, defense: effectiveness.DefenseElementID}
		if _, duplicate := seen[pair]; duplicate {
			return fmt.Errorf("%w: rules.elementEffectiveness[%d] 组合重复", ErrInvalidInitialState, index)
		}
		seen[pair] = struct{}{}
	}
	if rules.NormalizedLevel > 100 {
		return fmt.Errorf("%w: rules.normalizedLevel 必须介于 0 到 100", ErrInvalidInitialState)
	}
	for index, restriction := range rules.StableCodeRestrictions {
		if (restriction.Mode != "allow" && restriction.Mode != "deny") ||
			(restriction.ResourceType != "ability" && restriction.ResourceType != "creature" &&
				restriction.ResourceType != "item" && restriction.ResourceType != "skill") ||
			len(restriction.StableCodes) == 0 {
			return fmt.Errorf("%w: rules.stableCodeRestrictions[%d] 无效", ErrInvalidInitialState, index)
		}
		seenCodes := make(map[string]struct{}, len(restriction.StableCodes))
		for _, code := range restriction.StableCodes {
			if strings.TrimSpace(code) == "" {
				return fmt.Errorf("%w: rules.stableCodeRestrictions[%d] 包含空编码", ErrInvalidInitialState, index)
			}
			if _, duplicate := seenCodes[code]; duplicate {
				return fmt.Errorf("%w: rules.stableCodeRestrictions[%d] 包含重复编码", ErrInvalidInitialState, index)
			}
			seenCodes[code] = struct{}{}
		}
	}
	return nil
}

func validateSide(side SideSnapshot, format FormatSnapshot, rules RuleSnapshot) error {
	if len(side.Members) == 0 || len(side.Members) > int(format.TeamSize) {
		return fmt.Errorf("%w: side=%d members 数量为 %d", ErrInvalidInitialState, side.Side, len(side.Members))
	}
	if len(side.ActiveMembers) != int(format.ActiveSlotsPerSide) {
		return fmt.Errorf("%w: side=%d activeMembers 数量为 %d", ErrInvalidInitialState, side.Side, len(side.ActiveMembers))
	}
	if err := validateSideConditions(side.Conditions); err != nil {
		return fmt.Errorf("%w: side=%d conditions: %v", ErrInvalidInitialState, side.Side, err)
	}
	if side.Conditions.StealthRock && rules.ElementIDs["rock"] == 0 {
		return fmt.Errorf("%w: side=%d 隐形岩需要 rules.elementIds.rock", ErrInvalidInitialState, side.Side)
	}

	members := make(map[MemberPosition]struct{}, len(side.Members))
	for index, member := range side.Members {
		if err := validateMember(member); err != nil {
			return fmt.Errorf("%w: side=%d members[%d]: %v", ErrInvalidInitialState, side.Side, index, err)
		}
		if _, duplicate := members[member.Position]; duplicate {
			return fmt.Errorf("%w: side=%d memberPosition=%d 重复", ErrInvalidInitialState, side.Side, member.Position)
		}
		members[member.Position] = struct{}{}
	}
	active := make(map[MemberPosition]struct{}, len(side.ActiveMembers))
	for _, position := range side.ActiveMembers {
		if _, exists := members[position]; !exists {
			return fmt.Errorf("%w: side=%d active memberPosition=%d 不存在", ErrInvalidInitialState, side.Side, position)
		}
		if _, duplicate := active[position]; duplicate {
			return fmt.Errorf("%w: side=%d active memberPosition=%d 重复", ErrInvalidInitialState, side.Side, position)
		}
		active[position] = struct{}{}
	}
	return nil
}

func validateMember(member MemberSnapshot) error {
	if !member.Position.Valid() || !member.CreatureID.IsValid() {
		return errors.New("成员位置或 creatureId 无效")
	}
	if len(member.GenderCode) > 64 || strings.TrimSpace(member.GenderCode) != member.GenderCode {
		return errors.New("成员冻结性别代码无效")
	}
	if member.Level < 1 || member.Level > 100 || member.MaxHP == 0 || member.CurrentHP > member.MaxHP {
		return errors.New("等级或生命值无效")
	}
	if err := validateWeatherDamageImmunities(member.WeatherDamageImmunities); err != nil {
		return fmt.Errorf("天气伤害免疫无效: %w", err)
	}
	if !validAbilityDamageMultipliers(member) {
		return errors.New("特性攻击方伤害倍率无效")
	}
	if !validAbilityConditionalDamageMultipliers(member) {
		return errors.New("特性条件伤害倍率无效")
	}
	if !validAbilityStatMultipliers(member) {
		return errors.New("特性公式能力倍率无效")
	}
	if err := validateReactiveAbilityRules(member.ReactiveAbilityRules); err != nil {
		return fmt.Errorf("反应型特性规则无效: %w", err)
	}
	if member.SkillWeatherOverride != "" && !member.SkillWeatherOverride.valid() {
		return errors.New("技能天气覆盖无效")
	}
	if err := validateSkillElementConversion(member.SkillElementConversion); err != nil {
		return fmt.Errorf("技能属性转换无效: %w", err)
	}
	if err := validateDamageFraction(member.ContactSkillProtectionBypassDamageMultiplier); err != nil {
		return fmt.Errorf("保护穿透伤害倍率无效: %w", err)
	}
	if member.ContactSkillProtectionBypassDamageMultiplier != nil && !member.ContactSkillProtectionBypass {
		return errors.New("保护穿透伤害倍率要求启用接触技能保护穿透")
	}
	if member.ChargedElementID == 0 {
		if (member.ChargedDamageNumerator != 0 || member.ChargedDamageDenominator != 0) &&
			(member.ChargedDamageNumerator != 1 || member.ChargedDamageDenominator != 1) {
			return errors.New("空受伤充能运行态必须使用 0/0 或 1/1 倍率")
		}
	} else if member.ChargedDamageNumerator == 0 || member.ChargedDamageDenominator == 0 {
		return errors.New("受伤充能运行态倍率不完整")
	}
	if !validAccuracyMultiplier(member.AccuracyMultiplier) || !validAccuracyMultiplier(member.PhysicalSkillAccuracyMultiplier) ||
		!validAccuracyMultiplier(member.OpponentAccuracySandstormMultiplier) || !validAccuracyMultiplier(member.OpponentAccuracySnowMultiplier) ||
		!validAccuracyMultiplier(member.OpponentAccuracyConfusionMultiplier) {
		return errors.New("特性命中倍率无效")
	}
	if member.HeldItemAccuracyMissStatStageBoostDelta < 0 || member.HeldItemAccuracyMissStatStageBoostDelta > 6 ||
		!validOptionalHeldItemStatStageBoost(member.HeldItemAccuracyMissStatStageBoostStat, member.HeldItemAccuracyMissStatStageBoostDelta) ||
		member.HeldItemAdditionalFlinchChancePercent > 100 || member.HeldItemRandomActionOrderBoostChancePercent > 100 {
		return errors.New("触发型持有道具数值无效")
	}
	if (member.LastDeclaredSkillID == 0) != (member.ConsecutiveDeclaredSkillUses == 0) {
		return errors.New("连续技能宣告状态不完整")
	}
	if member.StatusSkillAccuracyCap > 100 {
		return errors.New("特性变化技能命中上限无效")
	}
	if member.CriticalHitStageBoost > 6 {
		return errors.New("特性固定击中要害等级增益无效")
	}
	if err := validateWeatherEndTurnHealing(member.WeatherEndTurnHealing); err != nil {
		return fmt.Errorf("天气回合末回复无效: %w", err)
	}
	if err := validateWeatherSpeedMultipliers(member.WeatherSpeedMultipliers); err != nil {
		return fmt.Errorf("天气速度倍率无效: %w", err)
	}
	if err := validateEnvironmentHighestStatMultiplier(member.EnvironmentHighestStatMultiplier); err != nil {
		return fmt.Errorf("环境最高能力强化无效: %w", err)
	}
	if err := validateTerastallizationStatStageChange(member.TerastallizationStatStageChange); err != nil {
		return fmt.Errorf("太晶化能力阶级变化无效: %w", err)
	}
	if !validOpponentSwitchRestriction(member.OpponentSwitchRestriction) {
		return errors.New("对手主动换人限制规则无效")
	}
	if len(member.HighestStatBoosterAbilityIDs) > 16 {
		return errors.New("最高能力强化道具特性数量超过上限")
	}
	boosterAbilities := make(map[Identifier]struct{}, len(member.HighestStatBoosterAbilityIDs))
	for _, abilityID := range member.HighestStatBoosterAbilityIDs {
		if !abilityID.IsValid() {
			return errors.New("最高能力强化道具包含空特性标识")
		}
		if _, duplicate := boosterAbilities[abilityID]; duplicate {
			return errors.New("最高能力强化道具特性标识重复")
		}
		boosterAbilities[abilityID] = struct{}{}
	}
	if member.BoosterEnergyStat != "" && !validHighestRawBattleStat(member.BoosterEnergyStat) {
		return fmt.Errorf("最高能力强化道具已选能力无效: %q", member.BoosterEnergyStat)
	}
	if member.SwitchInStrongWeather != "" && !member.SwitchInStrongWeather.valid() {
		return fmt.Errorf("入场强天气无效: %q", member.SwitchInStrongWeather)
	}
	if err := validateSwitchInWeather(member.SwitchInWeather); err != nil {
		return err
	}
	if err := validateSwitchInTerrain(member.SwitchInTerrain); err != nil {
		return err
	}
	if err := validateSwitchInStatStageChange(member.SwitchInStatStageChange); err != nil {
		return err
	}
	if err := validateSwitchInAllyHeal(member.SwitchInAllyHeal); err != nil {
		return err
	}
	if member.SwitchInStrongWeather != "" && member.SwitchInWeather != nil {
		return errors.New("同一成员不能同时声明普通与强天气入场效果")
	}
	if member.MajorStatus != "" && !member.MajorStatus.Valid() {
		return errors.New("主要异常状态无效")
	}
	if member.MajorStatus == MajorStatusBadPoison && member.BadPoisonCounter <= 0 {
		return errors.New("剧毒状态必须携带正数伤害计数")
	}
	if member.MajorStatus != MajorStatusBadPoison && member.BadPoisonCounter != 0 {
		return errors.New("非剧毒状态不能携带剧毒伤害计数")
	}
	if member.MajorStatus == MajorStatusSleep && member.SleepTurnsRemaining <= 0 {
		return errors.New("睡眠状态必须携带正数剩余阻止次数")
	}
	if member.MajorStatus != MajorStatusSleep && member.SleepTurnsRemaining != 0 {
		return errors.New("非睡眠状态不能携带睡眠剩余阻止次数")
	}
	if member.FlinchedTurn != 0 {
		return errors.New("初始成员不能携带畏缩回合")
	}
	if member.ConfusionTurnsRemaining != 0 || member.BindingTurnsRemaining != 0 || member.ProtectionTurnsRemaining != 0 || member.ProtectionChain != 0 || member.SubstituteHP != 0 || member.LeechSeedSourceSlot != nil || member.TauntTurnsRemaining != 0 ||
		member.ChargingSkillPosition != 0 || member.ChargingTurnsRemaining != 0 || member.RechargeTurnsRemaining != 0 || member.AccuracyLockTarget != nil || member.AccuracyLockTurnsRemaining != 0 || member.LockedSkillPosition != 0 ||
		member.LockedTurnsRemaining != 0 || member.DisabledSkillPosition != 0 || member.DisabledTurnsRemaining != 0 ||
		member.LastUsedSkillPosition != 0 {
		return errors.New("初始成员不能携带易变状态或最近技能记录")
	}
	for stat, stage := range member.StatStages {
		if !stat.Valid() || stage < -6 || stage > 6 {
			return errors.New("能力阶级必须使用已知能力项且处于 -6 至 6")
		}
	}
	if member.Stats.Attack == 0 || member.Stats.Defense == 0 || member.Stats.SpecialAttack == 0 ||
		member.Stats.SpecialDefense == 0 || member.Stats.Speed == 0 {
		return errors.New("五项战斗能力必须全部大于 0")
	}
	if len(member.ElementIDs) < 1 || len(member.ElementIDs) > 2 || hasBlankOrDuplicate(member.ElementIDs) {
		return errors.New("属性必须包含一至两个不重复稳定 ID")
	}
	if len(member.NaturalElementIDs) != 0 &&
		(len(member.NaturalElementIDs) < 1 || len(member.NaturalElementIDs) > 2 || hasBlankOrDuplicate(member.NaturalElementIDs)) {
		return errors.New("自然属性基线必须包含一至两个不重复稳定 ID")
	}
	if member.Terastallized && (!member.TeraElementID.IsValid() || len(member.ElementIDs) != 1 || member.ElementIDs[0] != member.TeraElementID) {
		return errors.New("已太晶化成员必须保留其单一太晶属性")
	}
	seenFormProfiles := make(map[Identifier]struct{}, len(member.FormProfiles))
	for _, profile := range member.FormProfiles {
		if err := validateFormProfile(profile); err != nil {
			return err
		}
		if _, duplicate := seenFormProfiles[profile.CreatureID]; duplicate {
			return errors.New("形态画像不能包含重复 creatureId")
		}
		seenFormProfiles[profile.CreatureID] = struct{}{}
	}
	if err := validateSwitchInFormChange(member.SwitchInFormChange, member.FormProfiles); err != nil {
		return err
	}
	if err := validateSwitchOutFormChange(member.SwitchOutFormChange, member.FormProfiles); err != nil {
		return err
	}
	if err := validateWeatherFormChange(member.WeatherFormChange, member.FormProfiles); err != nil {
		return err
	}
	if len(member.Skills) < 1 || len(member.Skills) > MaximumSkillsPerMember {
		return errors.New("技能必须包含一至四个槽位")
	}
	seenSkills := make(map[Identifier]struct{}, len(member.Skills))
	for index, skill := range member.Skills {
		if skill.Position != SkillPosition(index+1) || !skill.SkillID.IsValid() ||
			strings.TrimSpace(skill.Name) == "" || !skill.ElementID.IsValid() ||
			!skill.DamageClass.Valid() || skill.RemainingPP > skill.MaxPP {
			return fmt.Errorf("技能槽 %d 无效", index+1)
		}
		if !skill.TargetScope.Valid() {
			return fmt.Errorf("技能槽 %d 的目标范围无效", index+1)
		}
		if skill.DrainPercent < -100 || skill.DrainPercent > 100 || skill.HealingPercent < -100 || skill.HealingPercent > 100 {
			return fmt.Errorf("技能槽 %d 的生命效果百分比无效", index+1)
		}
		if (skill.TargetHealingNumerator == 0) != (skill.TargetHealingDenominator == 0) ||
			skill.TargetHealingNumerator > skill.TargetHealingDenominator {
			return fmt.Errorf("技能槽 %d 的目标回复分数无效", index+1)
		}
		if skill.TargetHealingNumerator != 0 &&
			(skill.DamageClass != DamageClassStatus || skill.targetScope() != SkillTargetScopeSelectedTarget) {
			return fmt.Errorf("技能槽 %d 的目标回复只能由单体目标变化技能执行", index+1)
		}
		if skill.MinHits < 1 || skill.MaxHits < 1 || skill.MinHits > 100 || skill.MaxHits > 100 || skill.MinHits > skill.MaxHits {
			return fmt.Errorf("技能槽 %d 的连续命中次数无效", index+1)
		}
		if skill.CriticalHitStage > 6 {
			return fmt.Errorf("技能槽 %d 的击中要害等级无效", index+1)
		}
		if skill.FlinchChancePercent > 100 {
			return fmt.Errorf("技能槽 %d 的畏缩概率无效", index+1)
		}
		if skill.LeechSeedApplication != nil {
			if skill.DamageClass != DamageClassStatus || skill.targetScope() != SkillTargetScopeSelectedTarget {
				return fmt.Errorf("技能槽 %d 的寄生种子只能由单体目标变化技能施加", index+1)
			}
			if err := validateLeechSeedApplication(*skill.LeechSeedApplication); err != nil {
				return fmt.Errorf("技能槽 %d 的寄生种子规则无效: %w", index+1, err)
			}
		}
		if skill.FieldSpeedOrderApplication != nil {
			if skill.DamageClass != DamageClassStatus || skill.targetScope() != SkillTargetScopeSelf {
				return fmt.Errorf("技能槽 %d 的全场速度顺序效果只能由自身范围的变化技能建立", index+1)
			}
			if err := validateFieldSpeedOrderApplication(*skill.FieldSpeedOrderApplication); err != nil {
				return fmt.Errorf("技能槽 %d 的全场速度顺序效果无效: %w", index+1, err)
			}
		}
		if skill.WeatherApplication != nil {
			if skill.DamageClass != DamageClassStatus || skill.targetScope() != SkillTargetScopeSelf {
				return fmt.Errorf("技能槽 %d 的天气只能由自身范围的变化技能建立", index+1)
			}
			if err := validateWeatherApplication(*skill.WeatherApplication); err != nil {
				return fmt.Errorf("技能槽 %d 的天气规则无效: %w", index+1, err)
			}
		}
		if skill.TerrainApplication != nil {
			if skill.DamageClass != DamageClassStatus || skill.targetScope() != SkillTargetScopeSelf {
				return fmt.Errorf("技能槽 %d 的场地只能由自身范围的变化技能建立", index+1)
			}
			if err := validateTerrainApplication(*skill.TerrainApplication); err != nil {
				return fmt.Errorf("技能槽 %d 的场地规则无效: %w", index+1, err)
			}
		}
		if err := validateWeatherAccuracyOverrides(skill.WeatherAccuracyOverrides); err != nil {
			return fmt.Errorf("技能槽 %d 的天气命中覆盖无效: %w", index+1, err)
		}
		if err := validateWeatherElementOverrides(skill.WeatherElementOverrides); err != nil {
			return fmt.Errorf("技能槽 %d 的天气属性覆盖无效: %w", index+1, err)
		}
		if err := validateWeatherPowerMultipliers(skill.WeatherPowerMultipliers); err != nil {
			return fmt.Errorf("技能槽 %d 的天气威力倍率无效: %w", index+1, err)
		}
		hasChargingApplication := false
		for _, application := range skill.VolatileStatusApplications {
			if application.Status == VolatileStatusCharging {
				hasChargingApplication = true
				break
			}
		}
		if err := validateChargeSkippedWeathers(skill.ChargeSkippedWeathers, hasChargingApplication); err != nil {
			return fmt.Errorf("技能槽 %d 的跳过蓄力天气无效: %w", index+1, err)
		}
		if skill.TailwindApplication != nil {
			if skill.DamageClass != DamageClassStatus || skill.targetScope() != SkillTargetScopeSelf {
				return fmt.Errorf("技能槽 %d 的顺风只能由自身范围的变化技能建立", index+1)
			}
			if err := validateTailwindApplication(*skill.TailwindApplication); err != nil {
				return fmt.Errorf("技能槽 %d 的顺风规则无效: %w", index+1, err)
			}
		}
		if skill.ReflectApplication != nil {
			if skill.DamageClass != DamageClassStatus || skill.targetScope() != SkillTargetScopeSelf {
				return fmt.Errorf("技能槽 %d 的反射壁只能由自身范围的变化技能建立", index+1)
			}
			if err := validateReflectApplication(*skill.ReflectApplication); err != nil {
				return fmt.Errorf("技能槽 %d 的反射壁规则无效: %w", index+1, err)
			}
		}
		if skill.LightScreenApplication != nil {
			if skill.DamageClass != DamageClassStatus || skill.targetScope() != SkillTargetScopeSelf {
				return fmt.Errorf("技能槽 %d 的光墙只能由自身范围的变化技能建立", index+1)
			}
			if err := validateLightScreenApplication(*skill.LightScreenApplication); err != nil {
				return fmt.Errorf("技能槽 %d 的光墙规则无效: %w", index+1, err)
			}
		}
		if skill.AuroraVeilApplication != nil {
			if skill.DamageClass != DamageClassStatus || skill.targetScope() != SkillTargetScopeSelf {
				return fmt.Errorf("技能槽 %d 的极光幕只能由自身范围的变化技能建立", index+1)
			}
			if err := validateAuroraVeilApplication(*skill.AuroraVeilApplication); err != nil {
				return fmt.Errorf("技能槽 %d 的极光幕规则无效: %w", index+1, err)
			}
		}
		if skill.SpikesApplication != nil {
			if skill.DamageClass != DamageClassStatus || skill.targetScope() != SkillTargetScopeSelectedTarget {
				return fmt.Errorf("技能槽 %d 的撒菱只能由单体目标变化技能建立", index+1)
			}
			if err := validateSpikesApplication(*skill.SpikesApplication); err != nil {
				return fmt.Errorf("技能槽 %d 的撒菱规则无效: %w", index+1, err)
			}
		}
		if skill.StealthRockApplication != nil {
			if skill.DamageClass != DamageClassStatus || skill.targetScope() != SkillTargetScopeSelectedTarget {
				return fmt.Errorf("技能槽 %d 的隐形岩只能由单体目标变化技能建立", index+1)
			}
			if err := validateStealthRockApplication(*skill.StealthRockApplication); err != nil {
				return fmt.Errorf("技能槽 %d 的隐形岩规则无效: %w", index+1, err)
			}
		}
		if skill.ToxicSpikesApplication != nil {
			if skill.DamageClass != DamageClassStatus || skill.targetScope() != SkillTargetScopeSelectedTarget {
				return fmt.Errorf("技能槽 %d 的毒菱只能由单体目标变化技能建立", index+1)
			}
			if err := validateToxicSpikesApplication(*skill.ToxicSpikesApplication); err != nil {
				return fmt.Errorf("技能槽 %d 的毒菱规则无效: %w", index+1, err)
			}
		}
		if skill.StickyWebApplication != nil {
			if skill.DamageClass != DamageClassStatus || skill.targetScope() != SkillTargetScopeSelectedTarget {
				return fmt.Errorf("技能槽 %d 的黏黏网只能由单体目标变化技能建立", index+1)
			}
			if err := validateStickyWebApplication(*skill.StickyWebApplication); err != nil {
				return fmt.Errorf("技能槽 %d 的黏黏网规则无效: %w", index+1, err)
			}
		}
		if skill.RapidSpinApplication != nil {
			if skill.DamageClass != DamageClassPhysical || skill.targetScope() != SkillTargetScopeSelectedTarget {
				return fmt.Errorf("技能槽 %d 的快速旋转只能由单体目标物理伤害技能建立", index+1)
			}
		}
		if skill.DefogApplication != nil {
			if skill.DamageClass != DamageClassStatus || skill.targetScope() != SkillTargetScopeSelectedTarget {
				return fmt.Errorf("技能槽 %d 的清除浓雾只能由单体目标变化技能建立", index+1)
			}
		}
		if skill.ForceTargetSwitch && skill.targetScope() != SkillTargetScopeSelectedTarget {
			return fmt.Errorf("技能槽 %d 的强制目标换人只能由单体目标技能建立", index+1)
		}
		if skill.LocksAccuracyOnTarget && (skill.DamageClass != DamageClassStatus || skill.targetScope() != SkillTargetScopeSelectedTarget) {
			return fmt.Errorf("技能槽 %d 的命中锁定只能由单体目标变化技能建立", index+1)
		}
		seenVolatileApplications := make(map[VolatileStatus]struct{}, len(skill.VolatileStatusApplications))
		for effectIndex, application := range skill.VolatileStatusApplications {
			if !application.Status.Valid() || !application.Target.Valid() || application.ChancePercent == 0 ||
				application.ChancePercent > 100 || application.MinTurns == 0 || application.MaxTurns < application.MinTurns {
				return fmt.Errorf("技能槽 %d 的易变状态效果 %d 无效", index+1, effectIndex)
			}
			if _, duplicate := seenVolatileApplications[application.Status]; duplicate {
				return fmt.Errorf("技能槽 %d 不能重复声明易变状态 %s", index+1, application.Status)
			}
			seenVolatileApplications[application.Status] = struct{}{}
			if application.Status == VolatileStatusCharging || application.Status == VolatileStatusLockedMove {
				if application.Target != EffectTargetUser || application.ChancePercent != 100 {
					return fmt.Errorf("技能槽 %d 的 %s 只能必定作用于使用者", index+1, application.Status)
				}
			}
			if application.Status == VolatileStatusCharging && (application.MinTurns != 1 || application.MaxTurns != 1) {
				return fmt.Errorf("技能槽 %d 的蓄力准备期必须为一回合", index+1)
			}
			if application.Status == VolatileStatusDisable && application.Target != EffectTargetSelected {
				return fmt.Errorf("技能槽 %d 的定身只能作用于已选目标", index+1)
			}
			if application.Status == VolatileStatusProtection &&
				(application.Target != EffectTargetUser || application.ChancePercent != 100 || application.MinTurns != 1 || application.MaxTurns != 1) {
				return fmt.Errorf("技能槽 %d 的保护必须必定作用于使用者且持续一回合", index+1)
			}
			if application.Status == VolatileStatusSubstitute {
				if application.Target != EffectTargetUser || application.ChancePercent != 100 || application.MinTurns != 1 || application.MaxTurns != 1 ||
					application.SubstituteCostNumerator == 0 || application.SubstituteCostDenominator == 0 ||
					application.SubstituteCostNumerator > application.SubstituteCostDenominator {
					return fmt.Errorf("技能槽 %d 的替身必须必定作用于使用者并携带有效生命费用", index+1)
				}
			} else if application.SubstituteCostNumerator != 0 || application.SubstituteCostDenominator != 0 {
				return fmt.Errorf("技能槽 %d 的非替身易变状态不能携带替身生命费用", index+1)
			}
		}
		if err := validateSkillDamageRule(skill); err != nil {
			return fmt.Errorf("技能槽 %d 的直接伤害规则无效: %w", index+1, err)
		}
		if err := validateDynamicPowerRule(skill.DynamicPower); err != nil {
			return fmt.Errorf("技能槽 %d 的动态威力规则无效: %w", index+1, err)
		}
		if skill.DynamicPower.active() && (skill.damageMode() != SkillDamageModeFormula || skill.DamageClass == DamageClassStatus) {
			return fmt.Errorf("技能槽 %d 的动态威力只能用于普通物理或特殊伤害", index+1)
		}
		if skill.DamageClass == DamageClassStatus && skill.Power != 0 {
			return fmt.Errorf("变化技能槽 %d 不能设置普通威力", index+1)
		}
		if (skill.CuresUserSideMajorStatuses || skill.CuresUserMajorStatus || skill.CuresUserSideActiveMajorStatuses) &&
			skill.DamageClass != DamageClassStatus {
			return fmt.Errorf("技能槽 %d 的主要异常治愈效果只能由变化技能使用", index+1)
		}
		if skill.damageMode() == SkillDamageModeAverageUserAndTargetCurrentHP && skill.DamageClass != DamageClassStatus {
			return fmt.Errorf("技能槽 %d 的当前生命平均规则只能由变化技能使用", index+1)
		}
		if skill.damageMode() == SkillDamageModeOneHitKnockOut && skill.DamageClass == DamageClassStatus {
			return fmt.Errorf("技能槽 %d 的一击必杀规则不能由变化技能使用", index+1)
		}
		if skill.damageMode() == SkillDamageModeReceivedDamage && skill.DamageClass == DamageClassStatus {
			return fmt.Errorf("技能槽 %d 的伤害记忆规则不能由变化技能使用", index+1)
		}
		for applicationIndex, application := range skill.StatusApplications {
			if !application.Status.Valid() || !application.Target.Valid() || application.ChancePercent > 100 {
				return fmt.Errorf("技能槽 %d 的主要异常效果 %d 无效", index+1, applicationIndex)
			}
		}
		for effectIndex, effect := range skill.StatStageEffects {
			if !effect.Stat.Valid() || !effect.Target.Valid() || effect.StageDelta == 0 ||
				effect.StageDelta < -6 || effect.StageDelta > 6 || effect.ChancePercent > 100 {
				return fmt.Errorf("技能槽 %d 的能力阶级效果 %d 无效", index+1, effectIndex)
			}
		}
		if skill.Accuracy > 100 {
			return fmt.Errorf("技能槽 %d 命中率无效", index+1)
		}
		if _, duplicate := seenSkills[skill.SkillID]; duplicate {
			return fmt.Errorf("技能 ID %q 重复", skill.SkillID)
		}
		seenSkills[skill.SkillID] = struct{}{}
	}
	return nil
}

// targetScope 返回技能执行时使用的显式目标范围。
func (skill SkillSnapshot) targetScope() SkillTargetScope {
	return skill.TargetScope
}

// damageMode 返回技能冻结资料中的显式伤害模式。
func (skill SkillSnapshot) damageMode() SkillDamageMode {
	return skill.DamageMode
}

// validateSkillDamageRule 校验直接伤害各模式的参数组合。所有数值均采用显式字段，避免不同取整、失败
// 条件和副作用相异的规则被压缩进一个不透明的数值或自由文本字段。
func validateSkillDamageRule(skill SkillSnapshot) error {
	mode := skill.damageMode()
	if !mode.Valid() {
		return errors.New("伤害模式未知")
	}
	hasOnlyZeroParameters := skill.DamageAmount == 0 && skill.DamageNumerator == 0 &&
		skill.DamageDenominator == 0 && skill.MinimumDamage == 0
	hasReceivedDamageParameters := skill.ReceivedDamageNumerator != 0 || skill.ReceivedDamageDenominator != 0 ||
		skill.ReceivedDamageAcceptsPhysical || skill.ReceivedDamageAcceptsSpecial ||
		skill.ReceivedDamageIgnoreNonImmuneElementEffectiveness
	switch mode {
	case SkillDamageModeFormula:
		if !hasOnlyZeroParameters {
			return errors.New("普通公式不能携带直接伤害参数")
		}
	case SkillDamageModeFixedAmount:
		if skill.DamageAmount == 0 || skill.DamageNumerator != 0 || skill.DamageDenominator != 0 || skill.MinimumDamage != 0 {
			return errors.New("固定伤害参数不完整")
		}
	case SkillDamageModeUserLevel, SkillDamageModeTargetCurrentHPMinusUserCurrentHP,
		SkillDamageModeUserCurrentHPAndUserFaints, SkillDamageModeAverageUserAndTargetCurrentHP:
		if !hasOnlyZeroParameters {
			return errors.New("当前伤害模式不能携带数值参数")
		}
	case SkillDamageModeOneHitKnockOut:
		if !hasOnlyZeroParameters || skill.OneHitKnockOutBaseAccuracy == 0 || skill.OneHitKnockOutBaseAccuracy > 100 ||
			skill.OneHitKnockOutSameElementUserBaseAccuracy > 100 {
			return errors.New("一击必杀规则参数无效")
		}
	case SkillDamageModeReceivedDamage:
		if !hasOnlyZeroParameters || skill.ReceivedDamageNumerator == 0 || skill.ReceivedDamageDenominator == 0 ||
			(!skill.ReceivedDamageAcceptsPhysical && !skill.ReceivedDamageAcceptsSpecial) {
			return errors.New("伤害记忆规则参数无效")
		}
	case SkillDamageModeTargetCurrentHPFraction:
		if skill.DamageNumerator == 0 || skill.DamageDenominator == 0 ||
			skill.DamageNumerator > skill.DamageDenominator || skill.MinimumDamage == 0 || skill.DamageAmount != 0 {
			return errors.New("当前生命比例伤害参数无效")
		}
	}
	if mode != SkillDamageModeOneHitKnockOut &&
		(skill.OneHitKnockOutBaseAccuracy != 0 || skill.OneHitKnockOutSameElementUserBaseAccuracy != 0 ||
			skill.OneHitKnockOutBlocksSameElementTarget) {
		return errors.New("非一击必杀规则不能携带一击必杀参数")
	}
	if mode != SkillDamageModeReceivedDamage && hasReceivedDamageParameters {
		return errors.New("非伤害记忆规则不能携带伤害记忆参数")
	}
	if mode != SkillDamageModeFormula {
		minimum, maximum := skill.hitRange()
		if minimum != 1 || maximum != 1 {
			return errors.New("直接伤害暂不支持连续命中")
		}
	}
	return nil
}

func hasBlankOrDuplicate(values []Identifier) bool {
	seen := make(map[Identifier]struct{}, len(values))
	for _, value := range values {
		if !value.IsValid() {
			return true
		}
		if _, duplicate := seen[value]; duplicate {
			return true
		}
		seen[value] = struct{}{}
	}
	return false
}

func cloneSides(sides []SideSnapshot) []SideSnapshot {
	cloned := make([]SideSnapshot, len(sides))
	for index, side := range sides {
		cloned[index] = SideSnapshot{
			Side:                 side.Side,
			ActiveMembers:        append([]MemberPosition(nil), side.ActiveMembers...),
			Members:              make([]MemberSnapshot, len(side.Members)),
			Conditions:           cloneSideConditions(side.Conditions),
			TerastallizationUsed: side.TerastallizationUsed,
		}
		for memberIndex, member := range side.Members {
			cloned[index].Members[memberIndex] = cloneMember(member)
		}
	}
	return cloned
}

func cloneMember(member MemberSnapshot) MemberSnapshot {
	originalStatStages := member.StatStages
	member.StatStages = make(map[Stat]int8, len(originalStatStages))
	for stat, stage := range originalStatStages {
		member.StatStages[stat] = stage
	}
	member.ElementIDs = append([]Identifier(nil), member.ElementIDs...)
	member.NaturalElementIDs = append([]Identifier(nil), member.NaturalElementIDs...)
	member.HighestStatBoosterAbilityIDs = append([]Identifier(nil), member.HighestStatBoosterAbilityIDs...)
	member.HeldItemElementIdentityBaseElementIDs = append([]Identifier(nil), member.HeldItemElementIdentityBaseElementIDs...)
	if member.AccuracyLockTarget != nil {
		target := *member.AccuracyLockTarget
		member.AccuracyLockTarget = &target
	}
	member.FormProfiles = cloneFormProfiles(member.FormProfiles)
	member.WeatherDamageImmunities = append([]WeatherKind(nil), member.WeatherDamageImmunities...)
	member.ReactiveAbilityRules = cloneReactiveAbilityRules(member.ReactiveAbilityRules)
	member.ContactSkillProtectionBypassDamageMultiplier = cloneDamageFraction(member.ContactSkillProtectionBypassDamageMultiplier)
	member.SkillElementConversion = cloneSkillElementConversion(member.SkillElementConversion)
	member.BasePowerAtMostDamageBoost = cloneBasePowerAtMostDamageBoost(member.BasePowerAtMostDamageBoost)
	member.RecoilSkillDamageBoost = cloneRecoilSkillDamageBoost(member.RecoilSkillDamageBoost)
	member.LowHPElementDamageBoost = cloneLowHPElementDamageBoost(member.LowHPElementDamageBoost)
	member.WeatherElementDamageBoost = cloneWeatherElementDamageBoost(member.WeatherElementDamageBoost)
	member.ElementSkillDamageBoost = cloneElementSkillDamageBoost(member.ElementSkillDamageBoost)
	member.SameElementBonusOverride = cloneSameElementBonusOverride(member.SameElementBonusOverride)
	member.ContactBasedSkillDamageBoost = cloneContactBasedSkillDamageBoost(member.ContactBasedSkillDamageBoost)
	member.CriticalHitDamageBoost = cloneCriticalHitDamageBoost(member.CriticalHitDamageBoost)
	member.SuperEffectiveDamageBoost = cloneSuperEffectiveDamageBoost(member.SuperEffectiveDamageBoost)
	member.NotVeryEffectiveDamageBoost = cloneNotVeryEffectiveDamageBoost(member.NotVeryEffectiveDamageBoost)
	member.TargetGenderDamageMultiplier = cloneTargetGenderDamageMultiplier(member.TargetGenderDamageMultiplier)
	member.PunchBasedSkillDamageBoost = clonePunchBasedSkillDamageBoost(member.PunchBasedSkillDamageBoost)
	member.SlicingBasedSkillDamageBoost = cloneSlicingBasedSkillDamageBoost(member.SlicingBasedSkillDamageBoost)
	member.SoundBasedSkillDamageBoost = cloneSoundBasedSkillDamageBoost(member.SoundBasedSkillDamageBoost)
	member.PulseBasedSkillDamageBoost = clonePulseBasedSkillDamageBoost(member.PulseBasedSkillDamageBoost)
	member.BiteBasedSkillDamageBoost = cloneBiteBasedSkillDamageBoost(member.BiteBasedSkillDamageBoost)
	member.SecondaryEffectsSuppressedDamageBoost = cloneSecondaryEffectsSuppressedDamageBoost(member.SecondaryEffectsSuppressedDamageBoost)
	member.SoundBasedSkillDamageReduction = cloneSoundBasedSkillDamageReduction(member.SoundBasedSkillDamageReduction)
	member.SuperEffectiveDamageReduction = cloneSuperEffectiveDamageReduction(member.SuperEffectiveDamageReduction)
	member.FullHPDamageReduction = cloneFullHPDamageReduction(member.FullHPDamageReduction)
	member.DamageClassDamageReduction = cloneDamageClassDamageReduction(member.DamageClassDamageReduction)
	member.ElementSkillDamageReduction = cloneElementSkillDamageReduction(member.ElementSkillDamageReduction)
	member.ContactBasedSkillDamageReduction = cloneContactBasedSkillDamageReduction(member.ContactBasedSkillDamageReduction)
	member.AttackingStatMultiplier = cloneAttackingStatMultiplier(member.AttackingStatMultiplier)
	member.OpponentAttackingStatMultiplier = cloneOpponentAttackingStatMultiplier(member.OpponentAttackingStatMultiplier)
	member.DefendingStatMultiplier = cloneDefendingStatMultiplier(member.DefendingStatMultiplier)
	member.OpponentDefendingStatMultiplier = cloneOpponentDefendingStatMultiplier(member.OpponentDefendingStatMultiplier)
	member.AllySkillDamageBoost = cloneAllySkillDamageBoost(member.AllySkillDamageBoost)
	member.AllyReceivedDamageReduction = cloneAllyReceivedDamageReduction(member.AllyReceivedDamageReduction)
	member.AllyAbilityPresenceAttackingStatMultiplier = cloneAllyAbilityPresenceAttackingStatMultiplier(member.AllyAbilityPresenceAttackingStatMultiplier)
	member.AccuracyMultiplier = cloneAccuracyMultiplier(member.AccuracyMultiplier)
	member.PhysicalSkillAccuracyMultiplier = cloneAccuracyMultiplier(member.PhysicalSkillAccuracyMultiplier)
	member.OpponentAccuracySandstormMultiplier = cloneAccuracyMultiplier(member.OpponentAccuracySandstormMultiplier)
	member.OpponentAccuracySnowMultiplier = cloneAccuracyMultiplier(member.OpponentAccuracySnowMultiplier)
	member.OpponentAccuracyConfusionMultiplier = cloneAccuracyMultiplier(member.OpponentAccuracyConfusionMultiplier)
	member.WeatherEndTurnHealing = cloneWeatherEndTurnHealing(member.WeatherEndTurnHealing)
	member.WeatherSpeedMultipliers = append([]WeatherSpeedMultiplier(nil), member.WeatherSpeedMultipliers...)
	member.OpponentSwitchRestriction = cloneOpponentSwitchRestriction(member.OpponentSwitchRestriction)
	member.EnvironmentHighestStatMultiplier = cloneEnvironmentHighestStatMultiplier(member.EnvironmentHighestStatMultiplier)
	member.TerastallizationStatStageChange = cloneTerastallizationStatStageChange(member.TerastallizationStatStageChange)
	member.SwitchInWeather = cloneSwitchInWeather(member.SwitchInWeather)
	member.SwitchInTerrain = cloneSwitchInTerrain(member.SwitchInTerrain)
	member.SwitchInStatStageChange = cloneSwitchInStatStageChange(member.SwitchInStatStageChange)
	member.SwitchInAllyHeal = cloneSwitchInAllyHeal(member.SwitchInAllyHeal)
	member.SwitchInFormChange = cloneSwitchInFormChange(member.SwitchInFormChange)
	member.SwitchOutFormChange = cloneSwitchOutFormChange(member.SwitchOutFormChange)
	member.WeatherFormChange = cloneWeatherFormChange(member.WeatherFormChange)
	member.TransformSnapshot = cloneMemberTransformSnapshot(member.TransformSnapshot)
	member.Skills = append([]SkillSnapshot(nil), member.Skills...)
	for index := range member.Skills {
		member.Skills[index].StatusApplications = append(
			[]MajorStatusApplication(nil), member.Skills[index].StatusApplications...,
		)
		member.Skills[index].StatStageEffects = append(
			[]StatStageEffect(nil), member.Skills[index].StatStageEffects...,
		)
		member.Skills[index].VolatileStatusApplications = append(
			[]VolatileStatusApplication(nil), member.Skills[index].VolatileStatusApplications...,
		)
		member.Skills[index].WeatherAccuracyOverrides = append(
			[]WeatherAccuracyOverride(nil), member.Skills[index].WeatherAccuracyOverrides...,
		)
		member.Skills[index].WeatherElementOverrides = append(
			[]WeatherElementOverride(nil), member.Skills[index].WeatherElementOverrides...,
		)
		member.Skills[index].WeatherPowerMultipliers = append(
			[]WeatherPowerMultiplier(nil), member.Skills[index].WeatherPowerMultipliers...,
		)
		member.Skills[index].ChargeSkippedWeathers = append(
			[]WeatherKind(nil), member.Skills[index].ChargeSkippedWeathers...,
		)
		member.Skills[index].DynamicPower = cloneDynamicPowerRule(member.Skills[index].DynamicPower)
		if member.Skills[index].LeechSeedApplication != nil {
			application := *member.Skills[index].LeechSeedApplication
			member.Skills[index].LeechSeedApplication = &application
		}
		if member.Skills[index].FieldSpeedOrderApplication != nil {
			application := *member.Skills[index].FieldSpeedOrderApplication
			member.Skills[index].FieldSpeedOrderApplication = &application
		}
		if member.Skills[index].WeatherApplication != nil {
			application := *member.Skills[index].WeatherApplication
			member.Skills[index].WeatherApplication = &application
		}
		if member.Skills[index].TerrainApplication != nil {
			application := *member.Skills[index].TerrainApplication
			member.Skills[index].TerrainApplication = &application
		}
		if member.Skills[index].TailwindApplication != nil {
			application := *member.Skills[index].TailwindApplication
			member.Skills[index].TailwindApplication = &application
		}
		if member.Skills[index].ReflectApplication != nil {
			application := *member.Skills[index].ReflectApplication
			member.Skills[index].ReflectApplication = &application
		}
		if member.Skills[index].LightScreenApplication != nil {
			application := *member.Skills[index].LightScreenApplication
			member.Skills[index].LightScreenApplication = &application
		}
		if member.Skills[index].AuroraVeilApplication != nil {
			application := *member.Skills[index].AuroraVeilApplication
			member.Skills[index].AuroraVeilApplication = &application
		}
		if member.Skills[index].SpikesApplication != nil {
			application := *member.Skills[index].SpikesApplication
			member.Skills[index].SpikesApplication = &application
		}
		if member.Skills[index].StealthRockApplication != nil {
			application := *member.Skills[index].StealthRockApplication
			member.Skills[index].StealthRockApplication = &application
		}
		if member.Skills[index].ToxicSpikesApplication != nil {
			application := *member.Skills[index].ToxicSpikesApplication
			member.Skills[index].ToxicSpikesApplication = &application
		}
		if member.Skills[index].StickyWebApplication != nil {
			application := *member.Skills[index].StickyWebApplication
			member.Skills[index].StickyWebApplication = &application
		}
		if member.Skills[index].RapidSpinApplication != nil {
			application := *member.Skills[index].RapidSpinApplication
			member.Skills[index].RapidSpinApplication = &application
		}
		if member.Skills[index].DefogApplication != nil {
			application := *member.Skills[index].DefogApplication
			member.Skills[index].DefogApplication = &application
		}
	}
	if member.LeechSeedSourceSlot != nil {
		sourceSlot := *member.LeechSeedSourceSlot
		member.LeechSeedSourceSlot = &sourceSlot
	}
	return member
}

// cloneMemberTransformSnapshot 深复制变身前画像，保证 State 的读取与后续状态转换不会共享切片或指针。
func cloneMemberTransformSnapshot(snapshot *MemberTransformSnapshot) *MemberTransformSnapshot {
	if snapshot == nil {
		return nil
	}
	cloned := *snapshot
	cloned.ElementIDs = append([]Identifier(nil), snapshot.ElementIDs...)
	cloned.NaturalElementIDs = append([]Identifier(nil), snapshot.NaturalElementIDs...)
	cloned.HeldItemElementIdentityBaseElementIDs = append([]Identifier(nil), snapshot.HeldItemElementIdentityBaseElementIDs...)
	cloned.Skills = cloneSkillSnapshots(snapshot.Skills)
	cloned.WeatherDamageImmunities = append([]WeatherKind(nil), snapshot.WeatherDamageImmunities...)
	cloned.ReactiveAbilityRules = cloneReactiveAbilityRules(snapshot.ReactiveAbilityRules)
	cloned.ContactSkillProtectionBypassDamageMultiplier = cloneDamageFraction(snapshot.ContactSkillProtectionBypassDamageMultiplier)
	cloned.SkillElementConversion = cloneSkillElementConversion(snapshot.SkillElementConversion)
	cloned.BasePowerAtMostDamageBoost = cloneBasePowerAtMostDamageBoost(snapshot.BasePowerAtMostDamageBoost)
	cloned.RecoilSkillDamageBoost = cloneRecoilSkillDamageBoost(snapshot.RecoilSkillDamageBoost)
	cloned.LowHPElementDamageBoost = cloneLowHPElementDamageBoost(snapshot.LowHPElementDamageBoost)
	cloned.WeatherElementDamageBoost = cloneWeatherElementDamageBoost(snapshot.WeatherElementDamageBoost)
	cloned.ElementSkillDamageBoost = cloneElementSkillDamageBoost(snapshot.ElementSkillDamageBoost)
	cloned.SameElementBonusOverride = cloneSameElementBonusOverride(snapshot.SameElementBonusOverride)
	cloned.ContactBasedSkillDamageBoost = cloneContactBasedSkillDamageBoost(snapshot.ContactBasedSkillDamageBoost)
	cloned.CriticalHitDamageBoost = cloneCriticalHitDamageBoost(snapshot.CriticalHitDamageBoost)
	cloned.SuperEffectiveDamageBoost = cloneSuperEffectiveDamageBoost(snapshot.SuperEffectiveDamageBoost)
	cloned.NotVeryEffectiveDamageBoost = cloneNotVeryEffectiveDamageBoost(snapshot.NotVeryEffectiveDamageBoost)
	cloned.TargetGenderDamageMultiplier = cloneTargetGenderDamageMultiplier(snapshot.TargetGenderDamageMultiplier)
	cloned.PunchBasedSkillDamageBoost = clonePunchBasedSkillDamageBoost(snapshot.PunchBasedSkillDamageBoost)
	cloned.SlicingBasedSkillDamageBoost = cloneSlicingBasedSkillDamageBoost(snapshot.SlicingBasedSkillDamageBoost)
	cloned.SoundBasedSkillDamageBoost = cloneSoundBasedSkillDamageBoost(snapshot.SoundBasedSkillDamageBoost)
	cloned.PulseBasedSkillDamageBoost = clonePulseBasedSkillDamageBoost(snapshot.PulseBasedSkillDamageBoost)
	cloned.BiteBasedSkillDamageBoost = cloneBiteBasedSkillDamageBoost(snapshot.BiteBasedSkillDamageBoost)
	cloned.SecondaryEffectsSuppressedDamageBoost = cloneSecondaryEffectsSuppressedDamageBoost(snapshot.SecondaryEffectsSuppressedDamageBoost)
	cloned.SoundBasedSkillDamageReduction = cloneSoundBasedSkillDamageReduction(snapshot.SoundBasedSkillDamageReduction)
	cloned.SuperEffectiveDamageReduction = cloneSuperEffectiveDamageReduction(snapshot.SuperEffectiveDamageReduction)
	cloned.FullHPDamageReduction = cloneFullHPDamageReduction(snapshot.FullHPDamageReduction)
	cloned.DamageClassDamageReduction = cloneDamageClassDamageReduction(snapshot.DamageClassDamageReduction)
	cloned.ElementSkillDamageReduction = cloneElementSkillDamageReduction(snapshot.ElementSkillDamageReduction)
	cloned.ContactBasedSkillDamageReduction = cloneContactBasedSkillDamageReduction(snapshot.ContactBasedSkillDamageReduction)
	cloned.AttackingStatMultiplier = cloneAttackingStatMultiplier(snapshot.AttackingStatMultiplier)
	cloned.OpponentAttackingStatMultiplier = cloneOpponentAttackingStatMultiplier(snapshot.OpponentAttackingStatMultiplier)
	cloned.DefendingStatMultiplier = cloneDefendingStatMultiplier(snapshot.DefendingStatMultiplier)
	cloned.OpponentDefendingStatMultiplier = cloneOpponentDefendingStatMultiplier(snapshot.OpponentDefendingStatMultiplier)
	cloned.AllySkillDamageBoost = cloneAllySkillDamageBoost(snapshot.AllySkillDamageBoost)
	cloned.AllyReceivedDamageReduction = cloneAllyReceivedDamageReduction(snapshot.AllyReceivedDamageReduction)
	cloned.AllyAbilityPresenceAttackingStatMultiplier = cloneAllyAbilityPresenceAttackingStatMultiplier(snapshot.AllyAbilityPresenceAttackingStatMultiplier)
	cloned.AccuracyMultiplier = cloneAccuracyMultiplier(snapshot.AccuracyMultiplier)
	cloned.PhysicalSkillAccuracyMultiplier = cloneAccuracyMultiplier(snapshot.PhysicalSkillAccuracyMultiplier)
	cloned.OpponentAccuracySandstormMultiplier = cloneAccuracyMultiplier(snapshot.OpponentAccuracySandstormMultiplier)
	cloned.OpponentAccuracySnowMultiplier = cloneAccuracyMultiplier(snapshot.OpponentAccuracySnowMultiplier)
	cloned.OpponentAccuracyConfusionMultiplier = cloneAccuracyMultiplier(snapshot.OpponentAccuracyConfusionMultiplier)
	cloned.WeatherEndTurnHealing = cloneWeatherEndTurnHealing(snapshot.WeatherEndTurnHealing)
	cloned.WeatherSpeedMultipliers = append([]WeatherSpeedMultiplier(nil), snapshot.WeatherSpeedMultipliers...)
	cloned.OpponentSwitchRestriction = cloneOpponentSwitchRestriction(snapshot.OpponentSwitchRestriction)
	cloned.EnvironmentHighestStatMultiplier = cloneEnvironmentHighestStatMultiplier(snapshot.EnvironmentHighestStatMultiplier)
	cloned.TerastallizationStatStageChange = cloneTerastallizationStatStageChange(snapshot.TerastallizationStatStageChange)
	cloned.SwitchInWeather = cloneSwitchInWeather(snapshot.SwitchInWeather)
	cloned.SwitchInTerrain = cloneSwitchInTerrain(snapshot.SwitchInTerrain)
	cloned.SwitchInStatStageChange = cloneSwitchInStatStageChange(snapshot.SwitchInStatStageChange)
	cloned.SwitchInAllyHeal = cloneSwitchInAllyHeal(snapshot.SwitchInAllyHeal)
	cloned.SwitchInFormChange = cloneSwitchInFormChange(snapshot.SwitchInFormChange)
	cloned.SwitchOutFormChange = cloneSwitchOutFormChange(snapshot.SwitchOutFormChange)
	cloned.WeatherFormChange = cloneWeatherFormChange(snapshot.WeatherFormChange)
	return &cloned
}

func cloneRuleSnapshot(rules RuleSnapshot) RuleSnapshot {
	originalElementIDs := rules.ElementIDs
	rules.ElementIDs = make(map[string]Identifier, len(rules.ElementIDs))
	for code, elementID := range originalElementIDs {
		rules.ElementIDs[code] = elementID
	}
	rules.ElementEffectiveness = append([]ElementEffectiveness(nil), rules.ElementEffectiveness...)
	originalStableCodeRestrictions := rules.StableCodeRestrictions
	rules.StableCodeRestrictions = make([]StableCodeRestriction, len(originalStableCodeRestrictions))
	for index, restriction := range originalStableCodeRestrictions {
		rules.StableCodeRestrictions[index] = restriction
		rules.StableCodeRestrictions[index].StableCodes = append([]string(nil), restriction.StableCodes...)
	}
	return rules
}
