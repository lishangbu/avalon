// Package itemrules 定义从规范化关系表组装的道具战斗规则只读投影。
package itemrules

import (
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// Detail 是与一个已有道具一一对应的简体中文详情。
type Detail struct {
	// ID 是道具详情在该完整资料聚合内的稳定 Identifier。
	ID snowflake.ID `json:"id"`
	// ItemID 是该详情所属的已启用道具稳定 Identifier。
	ItemID snowflake.ID `json:"itemId"`
	// FlingEffectID 是投掷该道具时采用的可选投掷效果稳定 Identifier。
	FlingEffectID *snowflake.ID `json:"flingEffectId"`
	// ElementDamageBoostElementID 是道具提供属性伤害强化时对应的可选属性稳定 Identifier。
	//
	// 它只描述道具自身的属性身份；实际伤害倍率和特性导致的属性替换各自由独立的战斗规则解释，不能将
	// 两种语义混进效果文本或无类型 JSON。
	ElementDamageBoostElementID *snowflake.ID `json:"elementDamageBoostElementId"`
	// HighestStatBoosterAbilityIDs 是允许消耗本道具并获得最高原始能力强化的特性稳定 Identifier 集合。
	//
	// 空集合表示道具不是这类消耗道具。集合只表达“哪些特性可以触发”，不保存任意效果文本、能力项或倍率：
	// Battle Engine 会在入场时按成员冻结的五项原始能力选取能力项，并使用固定整数倍率结算。
	HighestStatBoosterAbilityIDs []snowflake.ID `json:"highestStatBoosterAbilityIds"`
	// DamagedForceSelfSwitch 表示持有成员受到来自对手技能的实际伤害后，会消耗本道具并强制自身换下。
	// 它只描述受伤触发的自我换人；攻击者换人和能力下降触发换人各有独立字段，不能合并为无类型效果数组。
	DamagedForceSelfSwitch bool `json:"damagedForceSelfSwitch"`
	// DamagedForceAttackerSwitch 表示持有成员受到来自对手技能的实际伤害后，会消耗本道具并强制攻击者换下。
	// 目标的特性强制换人免疫可以阻止该规则，确保该反制效果不绕过明确的特性免疫边界。
	DamagedForceAttackerSwitch bool `json:"damagedForceAttackerSwitch"`
	// NegativeStatStageForceSelfSwitch 表示持有成员实际被降低任一能力阶级后，会消耗本道具并强制自身换下。
	// 仅声明尝试但被能力阶级下限阻止的下降不触发，避免把无状态变化的技能误记录为道具消耗。
	NegativeStatStageForceSelfSwitch bool `json:"negativeStatStageForceSelfSwitch"`
	// SwitchRestrictionImmunity 表示持有成员可以绕过敌方特性造成的主动换人限制。
	// 它不影响技能、道具造成的强制换人，也不替代束缚或锁招等易变状态的主动换人限制。
	SwitchRestrictionImmunity bool `json:"switchRestrictionImmunity"`
	// ContactSideEffectImmunity 表示攻击方携带本道具时免疫目标因本次有效接触触发的反制副作用。
	// 它不将接触技能改写为非接触技能，因此接触保护穿透、接触伤害倍率和技能自身附加效果仍按原规则结算。
	ContactSideEffectImmunity bool `json:"contactSideEffectImmunity"`
	// ContactDamageToAttackerDenominator 表示持有者受到有效接触本体伤害后反伤攻击者最大生命的分母。
	// 0 表示本道具不声明接触反伤；正值必须处于战斗引擎冻结 uint16 可表达的范围内。
	ContactDamageToAttackerDenominator int32 `json:"contactDamageToAttackerDenominator"`
	// EndTurnHealDenominator 表示持有者在回合末按最大生命固定比例回复的分母。
	// 0 表示本道具不提供回复规则；正值会由引擎至少回复 1 点、按缺失生命封顶，且不消费道具或随机数。
	EndTurnHealDenominator int32 `json:"endTurnHealDenominator"`
	// EndTurnHealForElementID 是道具在回合末条件回复所要求的当前有效属性稳定 Identifier。
	// nil 表示本道具不提供该规则；非 nil 时必须与 EndTurnHealForElementDenominator 同时存在并引用启用属性。
	EndTurnHealForElementID *snowflake.ID `json:"endTurnHealForElementId"`
	// EndTurnHealForElementDenominator 是属性匹配时按最大生命固定比例回复的分母。
	// 0 表示本道具不提供属性条件回复；正值由引擎至少回复 1 点并按缺失生命封顶。
	EndTurnHealForElementDenominator int32 `json:"endTurnHealForElementDenominator"`
	// EndTurnDamageDenominator 表示持有者在回合末按最大生命固定比例受到间接伤害的分母。
	// 0 表示本道具不提供自伤规则；正值会由引擎至少造成 1 点、按当前生命封顶，并可被间接伤害免疫阻止。
	EndTurnDamageDenominator int32 `json:"endTurnDamageDenominator"`
	// EndTurnDamageWithoutElementID 是道具在回合末条件自伤所排除的当前有效属性稳定 Identifier。
	// nil 表示本道具不提供该规则；非 nil 时必须与 EndTurnDamageWithoutElementDenominator 同时存在并引用启用属性。
	EndTurnDamageWithoutElementID *snowflake.ID `json:"endTurnDamageWithoutElementId"`
	// EndTurnDamageWithoutElementDenominator 是当前属性不包含指定属性时按最大生命固定比例自伤的分母。
	// 0 表示本道具不提供属性条件自伤；正值仍属于间接伤害，会被对应免疫规则阻止。
	EndTurnDamageWithoutElementDenominator int32 `json:"endTurnDamageWithoutElementDenominator"`
	// ConsumableElementDamageBoostElementID 是本道具一次性威力强化所匹配的技能有效属性稳定 Identifier。
	// nil 表示本道具不提供该规则；非 nil 时必须与正分子、正分母同时存在并引用启用属性。
	ConsumableElementDamageBoostElementID *snowflake.ID `json:"consumableElementDamageBoostElementId"`
	// ConsumableElementDamageBoostNumerator 是一次性属性威力强化倍率的正分子。
	// 0 表示本道具不提供该规则；引擎只在匹配技能造成真实本体伤害后消费这组冻结事实。
	ConsumableElementDamageBoostNumerator int32 `json:"consumableElementDamageBoostNumerator"`
	// ConsumableElementDamageBoostDenominator 是一次性属性威力强化倍率的正分母。
	// 它不能与普通属性伤害强化混用，确保替身承伤、未命中和免疫均不会错误消耗道具。
	ConsumableElementDamageBoostDenominator int32 `json:"consumableElementDamageBoostDenominator"`
	// PhysicalDamagePowerBoost 表示本道具是否把普通物理直接伤害技能的有效威力固定提高 10%。
	// 它不影响特殊、变化、固定伤害、比例伤害和间接伤害，也不消费道具。
	PhysicalDamagePowerBoost bool `json:"physicalDamagePowerBoost"`
	// SpecialDamagePowerBoost 表示本道具是否把普通特殊直接伤害技能的有效威力固定提高 10%。
	// 它不影响物理、变化、固定伤害、比例伤害和间接伤害，也不消费道具。
	SpecialDamagePowerBoost bool `json:"specialDamagePowerBoost"`
	// ElementDamageReductionElementID 是本道具一次性减免伤害所匹配的技能有效属性稳定 Identifier。
	// nil 表示没有该规则；匹配技能对本体造成伤害后按固定二分之一倍率结算并消费道具。
	ElementDamageReductionElementID *snowflake.ID `json:"elementDamageReductionElementId"`
	// ElementDamageReductionRequiresSuperEffective 表示减伤是否还要求技能对当前目标严格克制。
	ElementDamageReductionRequiresSuperEffective bool `json:"elementDamageReductionRequiresSuperEffective"`
	// SuperEffectiveDamageBoost 表示本道具是否固定强化效果绝佳的普通直接伤害 20%。
	SuperEffectiveDamageBoost bool `json:"superEffectiveDamageBoost"`
	// DamageBoostWithRecoil 表示本道具是否固定强化普通直接伤害 30%，并在造成伤害后反伤最大生命的十分之一。
	DamageBoostWithRecoil bool `json:"damageBoostWithRecoil"`
	// DamageDealtHeal 表示本道具在持有者造成实际技能伤害后回复伤害量的八分之一。
	DamageDealtHeal bool `json:"damageDealtHeal"`
	// DrainHealingBoost 表示本道具把吸取技能回复量固定提高 30%。
	DrainHealingBoost bool `json:"drainHealingBoost"`
	// AccuracyBoost 表示本道具把持有者普通技能命中率乘以 11/10。
	AccuracyBoost bool `json:"accuracyBoost"`
	// OpponentAccuracyReduction 表示本道具把对手针对持有者的普通技能命中率乘以 9/10。
	OpponentAccuracyReduction bool `json:"opponentAccuracyReduction"`
	// CriticalHitStageBoost 表示本道具为普通要害判定增加一级。
	CriticalHitStageBoost bool `json:"criticalHitStageBoost"`
	// AirborneUntilDamaged 表示本道具让持有者视为空中，直到首次承受真实本体伤害。
	AirborneUntilDamaged bool `json:"airborneUntilDamaged"`
	// ForceGrounded 表示本道具强制持有者视为接地。
	ForceGrounded bool `json:"forceGrounded"`
	// SpeedHalf 表示本道具把持有者行动排序速度减半。
	SpeedHalf bool `json:"speedHalf"`
	// SpecialDefenseBoost 表示本道具把持有者参与普通特殊伤害公式的特防乘以 3/2。
	SpecialDefenseBoost bool `json:"specialDefenseBoost"`
	// StatusSkillRestriction 表示本道具禁止持有者选择变化技能。
	StatusSkillRestriction bool `json:"statusSkillRestriction"`
	// PhysicalDamagePowerBoost50 表示讲究头带把普通物理技能威力提高 50%。
	PhysicalDamagePowerBoost50 bool `json:"physicalDamagePowerBoost50"`
	// SpecialDamagePowerBoost50 表示讲究眼镜把普通特殊技能威力提高 50%。
	SpecialDamagePowerBoost50 bool `json:"specialDamagePowerBoost50"`
	// ChoiceSkillLock 表示首次实际宣告技能后限制继续选择同一技能槽。
	ChoiceSkillLock bool `json:"choiceSkillLock"`
	// SpeedBoost50 表示讲究围巾把持有者有效速度提高 50%。
	SpeedBoost50 bool `json:"speedBoost50"`
	// AccuracyAfterTargetActedBoost 表示目标已行动时把普通命中率提高 20%。
	AccuracyAfterTargetActedBoost bool `json:"accuracyAfterTargetActedBoost"`
	// TypeImmunitySuppression 表示持有者自身属性提供的伤害免疫失效。
	TypeImmunitySuppression bool `json:"typeImmunitySuppression"`
	// OpponentStatStageReductionImmunity 表示阻止对手降低持有者能力阶级。
	OpponentStatStageReductionImmunity bool `json:"opponentStatStageReductionImmunity"`
	// NegativeStatStageReset 表示实际降阶后清零全部负阶级并消费道具。
	NegativeStatStageReset bool `json:"negativeStatStageReset"`
	// AbilityStatReductionSpeedBoost 表示对手入场特性降阶后提升速度并消费道具。
	AbilityStatReductionSpeedBoost bool `json:"abilityStatReductionSpeedBoost"`
	// OpponentPositiveStatStageCopy 表示复制对手技能产生的全部正能力阶级并消费道具。
	OpponentPositiveStatStageCopy bool `json:"opponentPositiveStatStageCopy"`
	// DamagingSkillSecondaryEffectImmunity 表示道具阻止对手伤害技能产生的目标侧追加效果。
	// 该效果不属于特性，因此攻击方的特性穿透不能绕过，且不会阻止技能本体伤害。
	DamagingSkillSecondaryEffectImmunity bool `json:"damagingSkillSecondaryEffectImmunity"`
	// BindingTurns 表示道具把新建立束缚覆盖为固定持续次数；0 表示沿用技能资料区间。
	// 正值必须能无损冻结为 Battle Engine 的 uint8 运行态字段。
	BindingTurns int32 `json:"bindingTurns"`
	// BindingDamageDenominator 表示道具把新建立束缚的回合末伤害覆盖为最大生命固定比例分母。
	// 0 表示沿用默认八分之一，正值必须能无损冻结为 uint16。
	BindingDamageDenominator int32 `json:"bindingDamageDenominator"`
	// AccuracyMissStatStageBoostStat 是技能因命中判定落空后提升的能力项；空值表示无规则。
	AccuracyMissStatStageBoostStat battleengine.Stat `json:"accuracyMissStatStageBoostStat"`
	// AccuracyMissStatStageBoostDelta 是命中落空后应用的正能力阶级变化量；必须与能力项同时存在。
	AccuracyMissStatStageBoostDelta int32 `json:"accuracyMissStatStageBoostDelta"`
	// WeaknessPolicy 表示承受效果绝佳真实本体技能伤害后攻击、特攻各提升两级并消费道具。
	WeaknessPolicy bool `json:"weaknessPolicy"`
	// WaterDamageSpecialAttackBoostElementID 是触发球根类特攻强化的水属性稳定 Identifier。
	WaterDamageSpecialAttackBoostElementID *snowflake.ID `json:"waterDamageSpecialAttackBoostElementId"`
	// ElectricDamageAttackBoostElementID 是触发充电电池类攻击强化的电属性稳定 Identifier。
	ElectricDamageAttackBoostElementID *snowflake.ID `json:"electricDamageAttackBoostElementId"`
	// WaterDamageSpecialDefenseBoostElementID 是触发光苔类特防强化的水属性稳定 Identifier。
	WaterDamageSpecialDefenseBoostElementID *snowflake.ID `json:"waterDamageSpecialDefenseBoostElementId"`
	// IceDamageAttackBoostElementID 是触发雪球类攻击强化的冰属性稳定 Identifier。
	IceDamageAttackBoostElementID *snowflake.ID `json:"iceDamageAttackBoostElementId"`
	// AdditionalFlinchChancePercent 是伤害技能命中后由道具追加的畏缩概率。
	AdditionalFlinchChancePercent int32 `json:"additionalFlinchChancePercent"`
	// RandomActionOrderBoostChancePercent 是同优先度技能行动随机先行的概率。
	RandomActionOrderBoostChancePercent int32 `json:"randomActionOrderBoostChancePercent"`
	// ForcedLastActionOrder 表示持有者技能行动在同优先度内强制最后行动。
	ForcedLastActionOrder bool `json:"forcedLastActionOrder"`
	// LowHPActionOrderBoost 表示生命不高于四分之一时消费本道具并进入先行层。
	LowHPActionOrderBoost bool `json:"lowHpActionOrderBoost"`
	// FieldSpeedOrderSpeedStageDrop 表示戏法空间成功建立后速度下降一级并在实际下降后消费道具。
	FieldSpeedOrderSpeedStageDrop bool `json:"fieldSpeedOrderSpeedStageDrop"`
	// ConsecutiveSkillDamageBoost 表示连续成功使用同一技能时按连续次数提高伤害。
	ConsecutiveSkillDamageBoost bool `json:"consecutiveSkillDamageBoost"`
	// ContactTransferToAttacker 表示持有者被无道具攻击方以有效接触造成真实本体伤害后，转移当前持有道具。
	// 它只声明转移资格，实际道具、运行时效果和所有权仍由 Battle Engine 的成员快照原子迁移。
	ContactTransferToAttacker bool `json:"contactTransferToAttacker"`
	// ChargeSkipOnce 表示持有者首次使用需要蓄力的技能时消耗本道具，并跳过本次蓄力等待。
	// 它是一次性道具规则，实际消费由 Battle Engine 在技能 PP 已合法消耗后执行。
	ChargeSkipOnce bool `json:"chargeSkipOnce"`
	// SurviveFatalDamageAtFullHP 表示持有者满生命承受致命对手技能伤害时消费本道具并保留 1 HP。
	// 它不属于特性效果，因此攻击方无视目标特性时仍会由 Battle Engine 独立判定。
	SurviveFatalDamageAtFullHP bool `json:"surviveFatalDamageAtFullHp"`
	// ReflectTurnsRemaining 表示持有者成功建立反射壁时允许的最大初始持续回合；0 表示不延长。
	// 它仅与同一道具的反射壁规则对应，不能被解释为其它侧状态的通用持续时间。
	ReflectTurnsRemaining int32 `json:"reflectTurnsRemaining"`
	// LightScreenTurnsRemaining 表示持有者成功建立光墙时允许的最大初始持续回合；0 表示不延长。
	// 正值会与技能声明值比较取大，避免维护资料意外缩短技能原有持续时间。
	LightScreenTurnsRemaining int32 `json:"lightScreenTurnsRemaining"`
	// AuroraVeilTurnsRemaining 表示持有者成功建立极光幕时允许的最大初始持续回合；0 表示不延长。
	// 三种屏障使用独立字段，以保持资料配置和引擎状态的一一对应关系。
	AuroraVeilTurnsRemaining int32 `json:"auroraVeilTurnsRemaining"`
	// RainTurnsRemaining 表示持有者成功建立普通降雨时允许的最大初始持续回合；0 表示不延长。
	// 它不会影响其它普通天气、强天气、无来源环境或已存在的降雨。
	RainTurnsRemaining int32 `json:"rainTurnsRemaining"`
	// SandstormTurnsRemaining 表示持有者成功建立普通沙暴时允许的最大初始持续回合；0 表示不延长。
	// 它与降雨使用独立资料字段，避免道具的适用天气范围在维护时被意外扩大。
	SandstormTurnsRemaining int32 `json:"sandstormTurnsRemaining"`
	// SnowTurnsRemaining 表示持有者成功建立普通降雪时允许的最大初始持续回合；0 表示不延长。
	// 它不与其它天气共享值，确保资料只对显式声明的降雪效果生效。
	SnowTurnsRemaining int32 `json:"snowTurnsRemaining"`
	// SunTurnsRemaining 表示持有者成功建立普通日照时允许的最大初始持续回合；0 表示不延长。
	// 它仅服务日照建立，避免资料维护时将一个道具错误用于多个普通天气。
	SunTurnsRemaining int32 `json:"sunTurnsRemaining"`
	// TerrainTurnsRemaining 表示持有者成功建立任一普通场地时允许的最大初始持续回合；0 表示不延长。
	// 此字段对应单一“全部普通场地”道具规则，不表示可任意配置的通用环境集合。
	TerrainTurnsRemaining int32 `json:"terrainTurnsRemaining"`
	// SandstormDamageImmunity 表示持有者是否免疫回合末普通沙暴造成的间接伤害。
	// 它不取消环境、不影响其它天气或成员，也不能替代通用间接伤害免疫规则。
	SandstormDamageImmunity bool `json:"sandstormDamageImmunity"`
	// EntryHazardImmunity 表示持有者是否免疫自身换入时的隐形岩、撒菱、毒菱和黏黏网。
	// 它不清除己方侧状态，也不保护其它成员，且不影响非入场的伤害或异常。
	EntryHazardImmunity bool `json:"entryHazardImmunity"`
	// WeightHalf 表示持有者参与体重规则计算时的有效体重是否减半。
	// 它不会改写生物资料中的权威体重，仅供对战开始时冻结给动态威力等运行期规则。
	WeightHalf bool `json:"weightHalf"`
	// CuresParalysis 表示持有者成功获得麻痹后是否立即消耗道具并解除该异常。
	// 它仅对应麻痹，不能隐式扩大为对其它主要异常的通用净化效果。
	CuresParalysis bool `json:"curesParalysis"`
	// CuresSleep 表示持有者成功获得睡眠后是否立即消耗道具并解除该异常。
	// 它会清空睡眠回合计数，但不能隐式扩大为麻痹或其它主要异常的通用净化效果。
	CuresSleep bool `json:"curesSleep"`
	// CuresPoison 表示持有者成功获得普通中毒或剧毒后是否立即消耗道具并解除该异常。
	// 它会清空剧毒计数，但不能隐式扩大为麻痹、睡眠、灼伤或冰冻的通用净化效果。
	CuresPoison bool `json:"curesPoison"`
	// CuresBurn 表示持有者成功获得灼伤后是否立即消耗道具并解除该异常。
	// 它只对应灼伤，不能隐式扩大为麻痹、睡眠、中毒、剧毒或冰冻的通用净化效果。
	CuresBurn bool `json:"curesBurn"`
	// CuresFreeze 表示持有者成功获得冰冻后是否立即消耗道具并解除该异常。
	// 它只对应冰冻，不能隐式扩大为麻痹、睡眠、中毒、剧毒或灼伤的通用净化效果。
	CuresFreeze bool `json:"curesFreeze"`
	// CuresAllMajorStatuses 表示持有者成功获得任一种主要异常后是否立即消耗道具并解除该异常。
	// 它覆盖灼伤、麻痹、中毒、剧毒、睡眠和冰冻，并会同步清除睡眠和剧毒的附属运行时计数。
	CuresAllMajorStatuses bool `json:"curesAllMajorStatuses"`
	// CuresConfusion 表示持有者成功获得混乱后是否立即消耗道具并解除该易变状态。
	// 它只清除混乱持续回合，不能隐式扩大为其它易变状态或主要异常的通用净化效果。
	CuresConfusion bool `json:"curesConfusion"`
	// PunchBasedSkillPowerBoost 表示持有者的拳击类技能是否在普通直接伤害的威力阶段获得固定 10% 强化。
	// 它不影响非拳击、固定伤害、比例伤害和间接伤害，也不改写技能资料自身的拳击标签。
	PunchBasedSkillPowerBoost bool `json:"punchBasedSkillPowerBoost"`
	// PunchBasedContactSuppression 表示持有者使用拳击类接触技能时是否动态取消本次有效接触。
	// 它不改写技能的静态 MakesContact 或 PunchBased 标签，非拳击技能仍按原有接触规则结算。
	PunchBasedContactSuppression bool `json:"punchBasedContactSuppression"`
	// PowderSkillImmunity 表示持有者是否在命中前免疫带粉末或孢子标签的技能。
	// 它不取消使用者的 PP 消耗，也不随攻击方无视目标特性规则失效；失去道具后立即停止提供免疫。
	PowderSkillImmunity bool `json:"powderSkillImmunity"`
	// MultiHitCountMinimum 是本道具对匹配连续命中技能应用后的实际段数下界；四个连续命中字段均为 0 时表示不覆盖。
	// 正常配置必须同时声明实际区间和原始技能区间，避免单个数值被误解为固定段数或全局效果。
	MultiHitCountMinimum int32 `json:"multiHitCountMinimum"`
	// MultiHitCountMaximum 是本道具对匹配连续命中技能应用后的实际段数上界。
	MultiHitCountMaximum int32 `json:"multiHitCountMaximum"`
	// MultiHitRequiredMinimum 是本道具能够覆盖的技能原始段数下界。
	MultiHitRequiredMinimum int32 `json:"multiHitRequiredMinimum"`
	// MultiHitRequiredMaximum 是本道具能够覆盖的技能原始段数上界。
	MultiHitRequiredMaximum int32 `json:"multiHitRequiredMaximum"`
	// Effect 是面向资料维护者的完整效果说明，不参与引擎执行。
	Effect *string `json:"effect"`
	// ShortEffect 是面向列表和快速查阅的简短效果说明，不参与引擎执行。
	ShortEffect *string `json:"shortEffect"`
	// Description 是面向玩家展示的物品描述；它不替代结构化道具规则。
	Description *string `json:"description"`
}

// Projection 是 Battle 启动时从规范化关系表组装的只读道具规则投影。
type Projection struct {
	// Details 按 Item Identifier 保存基础资料和各规则族。
	Details []Detail
}
