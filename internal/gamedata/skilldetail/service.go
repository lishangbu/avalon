// Package skilldetail 定义 Skill rules 使用的强类型规则值及校验。
package skilldetail

import (
	"errors"
	"strings"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

var (
	// ErrInvalidSkillDetail 表示技能规则值未通过边界校验。
	ErrInvalidSkillDetail = errors.New("技能详情无效")
)

// DamageMode 是实时技能详情中直接伤害规则的稳定资料代码。
//
// 公式伤害仍由 game_skill 的威力、伤害分类和纯引擎普通公式结算；其余模式会在命中后直接从当前战斗生命
// 快照推导伤害。该类型刻意不复用自由文本 effect，确保管理端资料能够在写入时被完整校验。
type DamageMode string

const (
	// DamageModeFormula 表示使用普通物理或特殊伤害公式。
	DamageModeFormula DamageMode = "formula"
	// DamageModeFixedAmount 表示造成固定的正伤害数值。
	DamageModeFixedAmount DamageMode = "fixed-amount"
	// DamageModeUserLevel 表示造成等于使用者等级的伤害。
	DamageModeUserLevel DamageMode = "user-level"
	// DamageModeTargetCurrentHPFraction 表示按目标当前生命的指定分数造成伤害。
	DamageModeTargetCurrentHPFraction DamageMode = "target-current-hp-fraction"
	// DamageModeTargetCurrentHPMinusUserCurrentHP 表示造成双方当前生命差值伤害。
	DamageModeTargetCurrentHPMinusUserCurrentHP DamageMode = "target-current-hp-minus-user-current-hp"
	// DamageModeUserCurrentHPAndUserFaints 表示以使用者当前生命造成伤害并使使用者倒下。
	DamageModeUserCurrentHPAndUserFaints DamageMode = "user-current-hp-and-user-faints"
	// DamageModeAverageUserAndTargetCurrentHP 表示把使用者与目标当前生命分别重设为双方当前生命的平均值。
	// 它不属于伤害、回复或生命交换，而是独立的生命重分配规则。
	DamageModeAverageUserAndTargetCurrentHP DamageMode = "average-user-and-target-current-hp"
	// DamageModeOneHitKnockOut 表示使用一击必杀专用等级与命中率规则，命中后直接造成目标当前生命的伤害。
	DamageModeOneHitKnockOut DamageMode = "one-hit-knock-out"
	// DamageModeReceivedDamage 表示按本回合最后一段合格已受伤害的明确倍率反打实际伤害来源。
	DamageModeReceivedDamage DamageMode = "received-damage"
)

// Valid 报告伤害模式是否为管理端和纯引擎共同支持的稳定资料代码。
func (mode DamageMode) Valid() bool {
	return mode == DamageModeFormula || mode == DamageModeFixedAmount || mode == DamageModeUserLevel ||
		mode == DamageModeTargetCurrentHPFraction || mode == DamageModeTargetCurrentHPMinusUserCurrentHP ||
		mode == DamageModeUserCurrentHPAndUserFaints || mode == DamageModeAverageUserAndTargetCurrentHP ||
		mode == DamageModeOneHitKnockOut || mode == DamageModeReceivedDamage
}

// VolatileStatus 是技能详情可配置、并由资料编译器转换为纯战斗引擎快照的易变状态稳定代码。
//
// 它只允许封闭枚举值，避免 Effect、ShortEffect 或技能名称等展示字段意外变成运行时规则。
type VolatileStatus string

const (
	// VolatileStatusConfusion 表示目标行动前可能伤害自身的混乱。
	VolatileStatusConfusion VolatileStatus = "confusion"
	// VolatileStatusBinding 表示目标持续受伤且不能主动换人的束缚。
	VolatileStatusBinding VolatileStatus = "binding"
	// VolatileStatusTaunt 表示目标暂时不能使用变化技能的挑衅。
	VolatileStatusTaunt VolatileStatus = "taunt"
	// VolatileStatusCharging 表示使用者先蓄力一回合、下一回合完成同一技能。
	VolatileStatusCharging VolatileStatus = "charging"
	// VolatileStatusLockedMove 表示使用者在若干回合内重复同一技能的锁招。
	VolatileStatusLockedMove VolatileStatus = "lockedMove"
	// VolatileStatusDisable 表示目标最近实际使用技能暂时不可再次使用的定身。
	VolatileStatusDisable VolatileStatus = "disable"
	// VolatileStatusProtection 表示使用者在本回合剩余行动中完全阻止对方技能影响自身的保护。
	VolatileStatusProtection VolatileStatus = "protection"
	// VolatileStatusSubstitute 表示使用者支付生命建立、并由独立生命值承受对方效果的替身。
	VolatileStatusSubstitute VolatileStatus = "substitute"
)

// Valid 报告易变状态代码是否可被当前资料服务与纯战斗引擎共同解释。
func (status VolatileStatus) Valid() bool {
	return status == VolatileStatusConfusion || status == VolatileStatusBinding || status == VolatileStatusTaunt ||
		status == VolatileStatusCharging || status == VolatileStatusLockedMove || status == VolatileStatusDisable ||
		status == VolatileStatusProtection || status == VolatileStatusSubstitute
}

// VolatileEffectTarget 是易变状态相对本次技能使用者的稳定目标类型。
type VolatileEffectTarget string

const (
	// VolatileEffectTargetSelectedTarget 表示状态写入本次解析到的目标成员。
	VolatileEffectTargetSelectedTarget VolatileEffectTarget = "selectedTarget"
	// VolatileEffectTargetUser 表示状态写入技能使用者自身。
	VolatileEffectTargetUser VolatileEffectTarget = "user"
)

// Valid 报告易变状态目标是否为资料契约允许的稳定值。
func (target VolatileEffectTarget) Valid() bool {
	return target == VolatileEffectTargetSelectedTarget || target == VolatileEffectTargetUser
}

// VolatileEffect 是一项技能成功后可能施加的强类型易变状态资料。
//
// MinTurns 与 MaxTurns 使用闭区间；资料服务会拒绝范围外、重复或与状态语义冲突的组合，纯引擎不需要
// 再解释任意 JSON 或自由文本。
type VolatileEffect struct {
	// Status 是封闭的易变状态稳定代码。
	Status VolatileStatus `json:"status"`
	// Target 是状态相对于技能使用者的落点。
	Target VolatileEffectTarget `json:"target"`
	// ChancePercent 是本项状态独立触发的百分比，取值 1 至 100。
	ChancePercent int32 `json:"chancePercent"`
	// MinTurns 是持续时间下界，取值 1 至 100。
	MinTurns int32 `json:"minTurns"`
	// MaxTurns 是持续时间上界，取值不小于 MinTurns 且不超过 100。
	MaxTurns int32 `json:"maxTurns"`
	// SubstituteCostNumerator 是 substitute 建立时支付使用者最大生命值的分子；仅 substitute 使用，
	// 其它易变状态必须为 0。实际费用按向下取整且至少为 1 点计算。
	SubstituteCostNumerator int32 `json:"substituteCostNumerator"`
	// SubstituteCostDenominator 是 substitute 建立生命费用的正分母；仅 substitute 使用，且不得小于分子。
	SubstituteCostDenominator int32 `json:"substituteCostDenominator"`
}

// OptionalValues 包含技能详情中允许为空的依赖、数值和简体中文文本。
type OptionalValues struct {
	AilmentID  *snowflake.ID `json:"ailmentID,omitempty"`
	CategoryID *snowflake.ID `json:"categoryID,omitempty"`
	TargetID   *snowflake.ID `json:"targetID,omitempty"`
	MinHits    *int32        `json:"minHits,omitempty"`
	MaxHits    *int32        `json:"maxHits,omitempty"`
	MinTurns   *int32        `json:"minTurns,omitempty"`
	MaxTurns   *int32        `json:"maxTurns,omitempty"`
	Drain      *int32        `json:"drain,omitempty"`
	Healing    *int32        `json:"healing,omitempty"`
	// TargetHealingNumerator 是技能按目标最大生命值回复目标的正分子；nil 表示没有目标回复效果。
	// 它必须与 TargetHealingDenominator 同时存在，且不得大于分母。
	TargetHealingNumerator *int32 `json:"targetHealingNumerator,omitempty"`
	// TargetHealingDenominator 是技能按目标最大生命值回复目标的正分母；nil 表示没有目标回复效果。
	TargetHealingDenominator *int32 `json:"targetHealingDenominator,omitempty"`
	CritRate                 *int32 `json:"critRate,omitempty"`
	// DamageMode 是命中后选择普通公式或某一类直接伤害的稳定资料代码；空值会在写入边界规范为 formula。
	DamageMode DamageMode `json:"damageMode,omitempty"`
	// DamageAmount 是 fixed-amount 规则的正固定伤害；其它规则必须为空。
	DamageAmount *int32 `json:"damageAmount,omitempty"`
	// DamageNumerator 是 target-current-hp-fraction 规则的正分子；其它规则必须为空。
	DamageNumerator *int32 `json:"damageNumerator,omitempty"`
	// DamageDenominator 是 target-current-hp-fraction 规则的正分母；其它规则必须为空。
	DamageDenominator *int32 `json:"damageDenominator,omitempty"`
	// MinimumDamage 是比例伤害向下取整后仍要造成的最小正伤害；其它规则必须为空。
	MinimumDamage *int32 `json:"minimumDamage,omitempty"`
	// OneHitKnockOutBaseAccuracy 是一击必杀规则的基础命中率，取值 1 至 100；仅 one-hit-knock-out 使用。
	// 引擎会在该基础上叠加双方等级差，不读取普通技能命中率或命中/闪避能力阶级。
	OneHitKnockOutBaseAccuracy *int32 `json:"oneHitKnockOutBaseAccuracy,omitempty"`
	// OneHitKnockOutSameElementUserBaseAccuracy 是使用者拥有本次技能属性时替换基础命中率的可选值。
	// 仅 one-hit-knock-out 使用，空值表示不存在同属性使用者例外。
	OneHitKnockOutSameElementUserBaseAccuracy *int32 `json:"oneHitKnockOutSameElementUserBaseAccuracy,omitempty"`
	// OneHitKnockOutBlocksSameElementTarget 表示拥有本次技能属性的目标会在命中掷骰前阻止一击必杀。
	// 非 one-hit-knock-out 规则必须保持 false。
	OneHitKnockOutBlocksSameElementTarget bool `json:"oneHitKnockOutBlocksSameElementTarget,omitempty"`
	// ReceivedDamageNumerator 是伤害记忆把最近一段合格已受伤害放大的正分子；其它规则必须为空。
	ReceivedDamageNumerator *int32 `json:"receivedDamageNumerator,omitempty"`
	// ReceivedDamageDenominator 是伤害记忆倍率的正分母；其它规则必须为空。
	ReceivedDamageDenominator *int32 `json:"receivedDamageDenominator,omitempty"`
	// ReceivedDamageAcceptsPhysical 表示伤害记忆是否可以记录物理伤害来源；物理和特殊至少必须接受一种。
	ReceivedDamageAcceptsPhysical bool `json:"receivedDamageAcceptsPhysical,omitempty"`
	// ReceivedDamageAcceptsSpecial 表示伤害记忆是否可以记录特殊伤害来源；物理和特殊至少必须接受一种。
	ReceivedDamageAcceptsSpecial bool `json:"receivedDamageAcceptsSpecial,omitempty"`
	// ReceivedDamageIgnoreNonImmuneElementEffectiveness 表示反打只保留完全免疫判定而不重复叠加非零属性倍率。
	// 该规则事实会被完整冻结到引擎快照，禁止由效果文本或技能名称隐式推断。
	ReceivedDamageIgnoreNonImmuneElementEffectiveness bool `json:"receivedDamageIgnoreNonImmuneElementEffectiveness,omitempty"`
	// WeakenedByGrassyTerrain 表示技能是否属于会被青草场地削弱的震动类地面效果。它是资料明确给出的规则
	// 标签，不能由技能名称、属性文本或管理端展示说明推断。
	WeakenedByGrassyTerrain bool `json:"weakenedByGrassyTerrain,omitempty"`
	// WeatherAccuracyOverrides 是技能在指定普通天气下替换基础命中率的独立规则集合；0 表示必中。
	WeatherAccuracyOverrides []WeatherAccuracyOverride `json:"weatherAccuracyOverrides,omitempty"`
	// WeatherElementOverrides 是技能在指定普通天气下替换基础属性的独立规则集合。
	// 每项必须引用启用的属性资料；其有效属性会被 Battle 编译器冻结到对局，不能由技能名称或说明文本推断。
	WeatherElementOverrides []WeatherElementOverride `json:"weatherElementOverrides,omitempty"`
	// WeatherPowerMultipliers 是技能在指定普通天气下调整普通伤害基础威力的独立规则集合。
	// 它以整数分数持久化并在 Battle 启动时冻结，不能与属性覆盖、命中覆盖或天气建立共用效果字段。
	WeatherPowerMultipliers []WeatherPowerMultiplier `json:"weatherPowerMultipliers,omitempty"`
	// ChargeSkippedWeathers 是技能可在指定普通天气下跳过首次蓄力等待的独立规则集合。
	// 非空集合要求同一技能已声明 charging 易变状态，不能将天气特例误配置给普通技能。
	ChargeSkippedWeathers []WeatherKind `json:"chargeSkippedWeathers,omitempty"`
	// DynamicPower 是公式伤害命中后、进入普通伤害公式前读取的可选动态基础威力规则。
	// 空规则表示读取 game_skill 的静态威力；非空规则必须完整地使用六种已支持强类型之一表达。
	DynamicPower DynamicPower `json:"dynamicPower,omitempty"`
	// FieldSpeedOrder 是技能成功后尝试建立的可选全场速度顺序效果；空值表示不改变全场行动排序。
	// 它独立于易变状态和伤害规则持久化，避免把全场效果误表示为某个成员或目标的局部状态。
	FieldSpeedOrder *FieldSpeedOrder `json:"fieldSpeedOrder,omitempty"`
	// LeechSeed 是技能命中后尝试种下的可选寄生种子规则；空值表示不写入目标的寄生状态。
	// 它的来源槽位、换人清除与回合末回复语义都由战斗引擎固定解释，不能复用易变状态或吸血字段。
	LeechSeed *LeechSeed `json:"leechSeed,omitempty"`
	// Weather 是技能成功后尝试建立的可选普通全场天气；空值表示不改变当前天气。
	// 它与全场速度顺序、易变状态及侧状态各自拥有明确的回合结算语义，不能合并为无约束环境效果。
	Weather *Weather `json:"weather,omitempty"`
	// Terrain 是技能成功后尝试建立的可选普通全场场地；空值表示不改变当前场地。
	// 场地以接地成员为边界影响伤害、异常和回合末结算，不能与天气或其它环境规则复用同一字段。
	Terrain *Terrain `json:"terrain,omitempty"`
	// Tailwind 是技能成功后尝试在使用者一方建立的可选顺风资料；空值表示不改变本方侧状态。
	// 顺风影响之后回合的同优先度速度排序，成员换下后仍持续作用于该方，不能复用天气或易变状态字段。
	Tailwind *Tailwind `json:"tailwind,omitempty"`
	// Reflect 是技能成功后尝试在使用者一方建立的可选反射壁资料；空值表示不改变本方物理屏障。
	// 它只减免普通物理伤害，必须与光墙、极光幕和能力阶级分开持久化，避免资料边界扩大减伤范围。
	Reflect *Reflect `json:"reflect,omitempty"`
	// LightScreen 是技能成功后尝试在使用者一方建立的可选光墙资料；空值表示不改变本方特殊屏障。
	// 它只减免普通特殊伤害，不能与反射壁或极光幕使用同一无语义的“屏障”载荷。
	LightScreen *LightScreen `json:"lightScreen,omitempty"`
	// AuroraVeil 是技能成功后尝试在使用者一方建立的可选极光幕资料；空值表示不改变本方双防屏障。
	// 极光幕同时减免普通物理和特殊伤害，必须保留独立强类型资料以供引擎准确冻结。
	AuroraVeil *AuroraVeil `json:"auroraVeil,omitempty"`
	// Spikes 是技能成功后尝试在被选中对手一方增加一层撒菱的可选资料；空值表示不布置撒菱。
	// 撒菱只伤害接地换入成员且最多三层，不能与其他入场危害复用同一资料字段。
	Spikes *Spikes `json:"spikes,omitempty"`
	// StealthRock 是技能成功后尝试在被选中对手一方布置隐形岩的可选资料；空值表示不布置隐形岩。
	// 隐形岩按岩石属性相性影响所有换入成员，不具有撒菱和毒菱的叠层语义。
	StealthRock *StealthRock `json:"stealthRock,omitempty"`
	// ToxicSpikes 是技能成功后尝试在被选中对手一方增加一层毒菱的可选资料；空值表示不布置毒菱。
	// 毒菱最多两层并会施加主要异常或被毒属性成员吸收，必须独立保存。
	ToxicSpikes *ToxicSpikes `json:"toxicSpikes,omitempty"`
	// StickyWeb 是技能成功后尝试在被选中对手一方布置黏黏网的可选资料；空值表示不布置黏黏网。
	// 黏黏网只会降低接地换入成员的速度能力阶级，不产生伤害或异常。
	StickyWeb *StickyWeb `json:"stickyWeb,omitempty"`
	// RapidSpin 是成功造成伤害后清除使用者一方入场危害的可选固定规则；空值表示没有快速旋转后效。
	// 它不清除屏障、顺风或对方危害，故不能用 Defog 或通用清除字段代替。
	RapidSpin *RapidSpin `json:"rapidSpin,omitempty"`
	// Defog 是成功后清除目标一方屏障、入场危害和普通场地的可选固定规则；空值表示没有清除浓雾后效。
	// 清除浓雾保留顺风，且作用范围与快速旋转不同，必须独立持久化。
	Defog *Defog `json:"defog,omitempty"`
	// ForceTargetSwitch 表示技能在目标完成普通伤害及其它目标向后效后，尝试强制目标从健康后备成员中替换。
	// 它只允许 Battle 编译为单个被选中对手的技能；替身、目标倒下或没有健康后备时由纯引擎明确不执行换人。
	ForceTargetSwitch bool `json:"forceTargetSwitch,omitempty"`
	// RechargesAfterUse 表示技能成功扣除目标本体生命后，使用者下一次技能行动前必须休整。
	// 替身承伤、免疫、保护、未命中及没有实际生命扣减时均不触发；该状态在成员离场时清除。
	RechargesAfterUse bool `json:"rechargesAfterUse,omitempty"`
	// LocksAccuracyOnTarget 表示技能命中后使使用者在下一回合内锁定当前具体目标的命中。
	// 该规则只能由单体目标变化技能使用；替身、保护、重复锁定和目标离场会阻止或清除运行态。
	LocksAccuracyOnTarget bool `json:"locksAccuracyOnTarget,omitempty"`
	// MakesContact 表示技能在没有后续规则动态改写时属于接触类技能。
	// 它是供保护穿透和未来接触反制读取的资料事实，不能从伤害类别、名称或说明文本推断。
	MakesContact bool `json:"makesContact,omitempty"`
	// PunchBased 表示技能是否属于拳击类技能。
	// 它独立于 MakesContact，供持有道具和特性在冻结后的战斗结算中读取，不能从技能名称或伤害类别推断。
	PunchBased bool `json:"punchBased,omitempty"`
	// SlicingBased 表示技能是否属于切割类技能；该事实独立于接触与伤害分类。
	SlicingBased bool `json:"slicingBased,omitempty"`
	// SoundBased 表示技能是否属于声音类技能；该事实不能从名称或说明文本推断。
	SoundBased bool `json:"soundBased,omitempty"`
	// PulseBased 表示技能是否属于波动类技能；伤害与目标回复会读取同一冻结标签。
	PulseBased bool `json:"pulseBased,omitempty"`
	// BiteBased 表示技能是否属于啃咬类技能；该事实不能由招式名称推断。
	BiteBased bool `json:"biteBased,omitempty"`
	// PowderBased 表示技能是否属于粉末或孢子类技能。
	// 它供持有道具和特性在命中前判断免疫，不能从主要异常、技能名称或说明文本推断。
	PowderBased bool `json:"powderBased,omitempty"`
	// VolatileEffects 是已完成结构校验的强类型易变状态数组；空数组表示技能没有易变状态效果。
	VolatileEffects []VolatileEffect `json:"volatileEffects,omitempty"`
	// CuresUserSideMajorStatuses 表示技能成功后清除使用者同侧整支队伍（含后备成员）的主要异常状态。
	// 该资料事实与只清除自身、只清除当前上场成员分别持久化，禁止由名称或说明文本推断范围。
	CuresUserSideMajorStatuses bool `json:"curesUserSideMajorStatuses,omitempty"`
	// CuresUserMajorStatus 表示技能成功后仅清除使用者自身的主要异常状态。
	CuresUserMajorStatus bool `json:"curesUserMajorStatus,omitempty"`
	// CuresUserSideActiveMajorStatuses 表示技能成功后仅清除使用者同侧当前上场成员的主要异常状态。
	CuresUserSideActiveMajorStatuses bool    `json:"curesUserSideActiveMajorStatuses,omitempty"`
	AilmentChance                    *int32  `json:"ailmentChance,omitempty"`
	FlinchChance                     *int32  `json:"flinchChance,omitempty"`
	StatChance                       *int32  `json:"statChance,omitempty"`
	Effect                           *string `json:"effect,omitempty"`
	ShortEffect                      *string `json:"shortEffect,omitempty"`
	// Description 是面向玩家展示的招式描述；它不作为战斗结算规则来源。
	Description *string `json:"description,omitempty"`
}

// RuleSet 是从 Skill 主资源规则文档解出的强类型规则集合。
// SkillID 和 Version 仅标识规则来源，不代表独立持久化实体。
type RuleSet struct {
	ID      snowflake.ID
	SkillID snowflake.ID
	OptionalValues
	Version int64
}

// Change 表示规则消息更新映射对一个可空字段的省略、清空或替换意图。
type Change[T any] struct {
	Specified bool
	Value     *T
}

func normalizeValues(values OptionalValues) OptionalValues {
	if values.DamageMode == "" {
		values.DamageMode = DamageModeFormula
	}
	values.Effect = normalizeText(values.Effect)
	values.ShortEffect = normalizeText(values.ShortEffect)
	values.Description = normalizeText(values.Description)
	values.DynamicPower = cloneDynamicPower(values.DynamicPower)
	values.FieldSpeedOrder = cloneFieldSpeedOrder(values.FieldSpeedOrder)
	values.LeechSeed = cloneLeechSeed(values.LeechSeed)
	values.Weather = cloneWeather(values.Weather)
	values.Terrain = cloneTerrain(values.Terrain)
	values.Tailwind = cloneTailwind(values.Tailwind)
	values.Reflect = cloneReflect(values.Reflect)
	values.LightScreen = cloneLightScreen(values.LightScreen)
	values.AuroraVeil = cloneAuroraVeil(values.AuroraVeil)
	values.Spikes = cloneSpikes(values.Spikes)
	values.StealthRock = cloneStealthRock(values.StealthRock)
	values.ToxicSpikes = cloneToxicSpikes(values.ToxicSpikes)
	values.StickyWeb = cloneStickyWeb(values.StickyWeb)
	values.RapidSpin = cloneRapidSpin(values.RapidSpin)
	values.Defog = cloneDefog(values.Defog)
	values.WeatherAccuracyOverrides = cloneWeatherAccuracyOverrides(values.WeatherAccuracyOverrides)
	values.WeatherElementOverrides = cloneWeatherElementOverrides(values.WeatherElementOverrides)
	values.WeatherPowerMultipliers = cloneWeatherPowerMultipliers(values.WeatherPowerMultipliers)
	values.ChargeSkippedWeathers = cloneChargeSkippedWeathers(values.ChargeSkippedWeathers)
	values.VolatileEffects = cloneVolatileEffects(values.VolatileEffects)
	return values
}

// NormalizeForRules 规范化并校验从 Skill rules 文档读取的完整战斗规则。
// 返回 false 表示规则不能安全进入 Battle Engine 的 Battle 冻结流程。
func NormalizeForRules(values OptionalValues) (OptionalValues, bool) {
	values = normalizeValues(values)
	return values, validValues(values)
}

func normalizeText(value *string) *string {
	if value == nil {
		return nil
	}
	normalized := strings.TrimSpace(*value)
	if normalized == "" {
		return nil
	}
	return &normalized
}

func validValues(values OptionalValues) bool {
	// 完整效果承载资料源的 additional_effect 长文；应用层保留较高上限以拒绝异常请求，
	// 但不能沿用旧数据库的两千字符限制截断合法招式说明。
	return !invalidOptionalIdentifier(values.AilmentID) && !invalidOptionalIdentifier(values.CategoryID) &&
		!invalidOptionalIdentifier(values.TargetID) && validRangePair(values.MinHits, values.MaxHits, 1, 100) &&
		validRangePair(values.MinTurns, values.MaxTurns, 1, 100) && validNumber(values.Drain, -100, 100) &&
		validNumber(values.Healing, -100, 100) && validTargetHealing(values) && validNumber(values.CritRate, 0, 6) &&
		validDamageRule(values) &&
		validDynamicPower(values.DynamicPower) &&
		validFieldSpeedOrder(values.FieldSpeedOrder) &&
		validLeechSeed(values.LeechSeed) &&
		validWeather(values.Weather) &&
		validTerrain(values.Terrain) &&
		validTailwind(values.Tailwind) &&
		validReflect(values.Reflect) &&
		validLightScreen(values.LightScreen) &&
		validAuroraVeil(values.AuroraVeil) &&
		validSpikes(values.Spikes) &&
		validStealthRock(values.StealthRock) &&
		validToxicSpikes(values.ToxicSpikes) &&
		validStickyWeb(values.StickyWeb) &&
		validRapidSpin(values.RapidSpin) &&
		validDefog(values.Defog) &&
		validWeatherAccuracyOverrides(values.WeatherAccuracyOverrides) &&
		validWeatherElementOverrides(values.WeatherElementOverrides) &&
		validWeatherPowerMultipliers(values.WeatherPowerMultipliers) &&
		validVolatileEffects(values.VolatileEffects) &&
		validChargeSkippedWeathers(values.ChargeSkippedWeathers) &&
		chargeSkippedWeathersRequireCharging(values.ChargeSkippedWeathers, values.VolatileEffects) &&
		validNumber(values.AilmentChance, 0, 100) && validNumber(values.FlinchChance, 0, 100) &&
		validNumber(values.StatChance, 0, 100) && validText(values.Effect, 20_000) &&
		validText(values.ShortEffect, 500) && validText(values.Description, 500)
}

func validTargetHealing(values OptionalValues) bool {
	if values.TargetHealingNumerator == nil && values.TargetHealingDenominator == nil {
		return true
	}
	return values.TargetHealingNumerator != nil && values.TargetHealingDenominator != nil &&
		*values.TargetHealingNumerator > 0 && *values.TargetHealingDenominator > 0 &&
		*values.TargetHealingNumerator <= *values.TargetHealingDenominator && *values.TargetHealingDenominator <= 65_535
}

// changeBoolValue 返回布尔更新字段的候选值。未指定字段不会进入本函数生成的完整值语义，且最终仍由 Repository
// 的 specified 标记保留旧值；这里将 nil 规范为 false，避免无效零指针穿透领域校验。
func validVolatileEffects(values []VolatileEffect) bool {
	if len(values) > 8 {
		return false
	}
	seen := make(map[VolatileStatus]struct{}, len(values))
	for _, value := range values {
		if !value.Status.Valid() || !value.Target.Valid() || value.ChancePercent < 1 || value.ChancePercent > 100 ||
			value.MinTurns < 1 || value.MaxTurns < value.MinTurns || value.MaxTurns > 100 {
			return false
		}
		if _, duplicate := seen[value.Status]; duplicate {
			return false
		}
		seen[value.Status] = struct{}{}
		if value.Status == VolatileStatusCharging || value.Status == VolatileStatusLockedMove {
			if value.Target != VolatileEffectTargetUser || value.ChancePercent != 100 {
				return false
			}
		}
		if value.Status == VolatileStatusCharging && (value.MinTurns != 1 || value.MaxTurns != 1) {
			return false
		}
		if value.Status == VolatileStatusDisable && value.Target != VolatileEffectTargetSelectedTarget {
			return false
		}
		if value.Status == VolatileStatusProtection &&
			(value.Target != VolatileEffectTargetUser || value.ChancePercent != 100 || value.MinTurns != 1 || value.MaxTurns != 1) {
			return false
		}
		if value.Status == VolatileStatusSubstitute {
			if value.Target != VolatileEffectTargetUser || value.ChancePercent != 100 || value.MinTurns != 1 || value.MaxTurns != 1 ||
				value.SubstituteCostNumerator < 1 || value.SubstituteCostNumerator > 100 || value.SubstituteCostDenominator < 1 ||
				value.SubstituteCostDenominator > 100 || value.SubstituteCostNumerator > value.SubstituteCostDenominator {
				return false
			}
		} else if value.SubstituteCostNumerator != 0 || value.SubstituteCostDenominator != 0 {
			return false
		}
	}
	return true
}

// cloneVolatileEffects 复制可变切片，避免调用方在命令创建后改变已经通过校验的资料事实。
func cloneVolatileEffects(values []VolatileEffect) []VolatileEffect {
	if values == nil {
		return []VolatileEffect{}
	}
	return append([]VolatileEffect(nil), values...)
}

// validDamageRule 校验直接伤害模式与其专属参数的完整组合。所有模式都将未使用的参数固定为空，使审计、
// API 和冻结快照不会出现“看似可用、实际被引擎忽略”的残留数值。
func validDamageRule(values OptionalValues) bool {
	if !values.DamageMode.Valid() {
		return false
	}
	if values.DamageMode != DamageModeFormula &&
		(values.MinHits != nil && *values.MinHits != 1 || values.MaxHits != nil && *values.MaxHits != 1) {
		return false
	}
	noParameters := values.DamageAmount == nil && values.DamageNumerator == nil &&
		values.DamageDenominator == nil && values.MinimumDamage == nil
	noOneHitKnockOutParameters := values.OneHitKnockOutBaseAccuracy == nil &&
		values.OneHitKnockOutSameElementUserBaseAccuracy == nil && !values.OneHitKnockOutBlocksSameElementTarget
	noReceivedDamageParameters := values.ReceivedDamageNumerator == nil && values.ReceivedDamageDenominator == nil &&
		!values.ReceivedDamageAcceptsPhysical && !values.ReceivedDamageAcceptsSpecial &&
		!values.ReceivedDamageIgnoreNonImmuneElementEffectiveness
	switch values.DamageMode {
	case DamageModeFormula:
		return noParameters && noOneHitKnockOutParameters && noReceivedDamageParameters
	case DamageModeUserLevel, DamageModeTargetCurrentHPMinusUserCurrentHP,
		DamageModeUserCurrentHPAndUserFaints, DamageModeAverageUserAndTargetCurrentHP:
		return noParameters && noOneHitKnockOutParameters && noReceivedDamageParameters && !values.DynamicPower.Active()
	case DamageModeFixedAmount:
		return values.DamageAmount != nil && *values.DamageAmount > 0 && values.DamageNumerator == nil &&
			values.DamageDenominator == nil && values.MinimumDamage == nil && noOneHitKnockOutParameters && noReceivedDamageParameters && !values.DynamicPower.Active()
	case DamageModeTargetCurrentHPFraction:
		return values.DamageAmount == nil && values.DamageNumerator != nil && *values.DamageNumerator > 0 &&
			values.DamageDenominator != nil && *values.DamageDenominator > 0 &&
			*values.DamageNumerator <= *values.DamageDenominator && values.MinimumDamage != nil && *values.MinimumDamage > 0 &&
			noOneHitKnockOutParameters && noReceivedDamageParameters && !values.DynamicPower.Active()
	case DamageModeOneHitKnockOut:
		return noParameters && values.OneHitKnockOutBaseAccuracy != nil &&
			*values.OneHitKnockOutBaseAccuracy >= 1 && *values.OneHitKnockOutBaseAccuracy <= 100 &&
			validNumber(values.OneHitKnockOutSameElementUserBaseAccuracy, 1, 100) && noReceivedDamageParameters && !values.DynamicPower.Active()
	case DamageModeReceivedDamage:
		return noParameters && noOneHitKnockOutParameters && values.ReceivedDamageNumerator != nil &&
			*values.ReceivedDamageNumerator > 0 && values.ReceivedDamageDenominator != nil &&
			*values.ReceivedDamageDenominator > 0 &&
			(values.ReceivedDamageAcceptsPhysical || values.ReceivedDamageAcceptsSpecial) && !values.DynamicPower.Active()
	default:
		return false
	}
}

// damageModePointer 返回用于全量替换更新的伤害模式指针，避免调用方共享可变局部变量地址。
func damageModePointer(mode DamageMode) *DamageMode {
	return &mode
}

func invalidOptionalIdentifier(value *snowflake.ID) bool {
	return value != nil && *value == snowflake.ID(0)
}

func validRangePair(minimum, maximum *int32, lower, upper int32) bool {
	return validNumber(minimum, lower, upper) && validNumber(maximum, lower, upper) &&
		(minimum == nil || maximum == nil || *minimum <= *maximum)
}

func validNumber(value *int32, minimum, maximum int32) bool {
	return value == nil || *value >= minimum && *value <= maximum
}

func validText(value *string, maximum int) bool {
	return value == nil || len([]rune(*value)) <= maximum
}
