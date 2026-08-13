// Package abilitydetail 定义 Ability rules 使用的强类型规则值及校验。
package abilitydetail

import (
	"errors"
	"strings"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/battleengine"
)

var (
	// ErrInvalidAbilityDetail 表示特性规则值未通过边界校验。
	ErrInvalidAbilityDetail = errors.New("特性详情无效")
)

// OptionalValues 包含特性详情中可选的显示文本和可执行天气规则。
type OptionalValues struct {
	// Effect 是面向资料维护者的完整效果说明；它不参与战斗结算。
	Effect *string `json:"effect,omitempty"`
	// ShortEffect 是面向列表与快速查阅的简短效果说明；它不参与战斗结算。
	ShortEffect *string `json:"shortEffect,omitempty"`
	// Introduction 是面向玩家展示的特性简介；它不替代完整机制说明或结构化规则。
	Introduction *string `json:"introduction,omitempty"`
	// WeatherDamageImmunities 是该特性赋予持有成员的普通天气回合末伤害免疫集合。
	// 它以独立 JSONB 字段持久化，不能与文字说明或未来其它特性效果共用泛型效果数组。
	WeatherDamageImmunities []WeatherKind `json:"weatherDamageImmunities,omitempty"`
	// WeatherEffectsSuppressed 表示持有成员在场且存活时会封锁普通天气的可执行效果。
	// 天气本身仍会在引擎环境中持续和到期；该布尔值不能与天气伤害免疫合并，因为两者作用的结算阶段不同。
	WeatherEffectsSuppressed bool `json:"weatherEffectsSuppressed,omitempty"`
	// ReactiveAbilityRules 是回合末、受伤和倒下窗口使用的完整强类型特性规则。
	// nil 表示没有这些规则；服务只校验并持有资料事实，实际结算由 Battle 深复制后交给纯 Battle Engine。
	ReactiveAbilityRules *battleengine.ReactiveAbilityRules `json:"reactiveAbilityRules,omitempty"`
	// BasePowerAtMostDamageBoost 是按技能原始基础威力上限触发的最终伤害倍率；nil 表示没有该规则。
	BasePowerAtMostDamageBoost *BasePowerAtMostDamageBoost `json:"basePowerAtMostDamageBoost,omitempty"`
	// RecoilSkillDamageBoost 是按实际伤害产生反作用的技能最终伤害倍率；nil 表示没有该规则。
	RecoilSkillDamageBoost *RecoilSkillDamageBoost `json:"recoilSkillDamageBoost,omitempty"`
	// LowHPElementDamageBoost 是低生命阈值下指定有效属性的最终伤害倍率；nil 表示没有该规则。
	LowHPElementDamageBoost *LowHPElementDamageBoost `json:"lowHPElementDamageBoost,omitempty"`
	// WeatherElementDamageBoost 是指定有效天气和属性集合共同触发的最终伤害倍率；nil 表示没有该规则。
	WeatherElementDamageBoost *WeatherElementDamageBoost `json:"weatherElementDamageBoost,omitempty"`
	// ElementSkillDamageBoost 是一组技能有效属性触发的最终伤害倍率；nil 表示没有该规则。
	ElementSkillDamageBoost *ElementSkillDamageBoost `json:"elementSkillDamageBoost,omitempty"`
	// SameElementBonusOverride 是替换默认属性一致加成的精确倍率；nil 表示继续使用默认倍率。
	SameElementBonusOverride *SameElementBonusOverride `json:"sameElementBonusOverride,omitempty"`
	// ContactBasedSkillDamageBoost 是技能本次仍构成有效接触时的最终伤害倍率；nil 表示没有该规则。
	ContactBasedSkillDamageBoost *ContactBasedSkillDamageBoost `json:"contactBasedSkillDamageBoost,omitempty"`
	// CriticalHitDamageBoost 是技能本次实际击中要害时的额外最终伤害倍率；nil 表示没有该规则。
	CriticalHitDamageBoost *CriticalHitDamageBoost `json:"criticalHitDamageBoost,omitempty"`
	// SuperEffectiveDamageBoost 是最终属性相性严格大于一时的最终伤害倍率；nil 表示没有该规则。
	SuperEffectiveDamageBoost *SuperEffectiveDamageBoost `json:"superEffectiveDamageBoost,omitempty"`
	// NotVeryEffectiveDamageBoost 是最终属性相性位于零与一之间时的最终伤害倍率；nil 表示没有该规则。
	NotVeryEffectiveDamageBoost *NotVeryEffectiveDamageBoost `json:"notVeryEffectiveDamageBoost,omitempty"`
	// TargetGenderDamageMultiplier 是按双方非空性别关系选择同性或异性倍率的独立规则。
	TargetGenderDamageMultiplier *TargetGenderDamageMultiplier `json:"targetGenderDamageMultiplier,omitempty"`
	// PunchBasedSkillDamageBoost 是拳击类普通公式伤害的独立最终倍率规则。
	PunchBasedSkillDamageBoost *PunchBasedSkillDamageBoost `json:"punchBasedSkillDamageBoost,omitempty"`
	// SlicingBasedSkillDamageBoost 是切割类普通公式伤害的独立最终倍率规则。
	SlicingBasedSkillDamageBoost *SlicingBasedSkillDamageBoost `json:"slicingBasedSkillDamageBoost,omitempty"`
	// SoundBasedSkillDamageBoost 是声音类普通公式伤害的独立攻击方倍率规则。
	SoundBasedSkillDamageBoost *SoundBasedSkillDamageBoost `json:"soundBasedSkillDamageBoost,omitempty"`
	// PulseBasedSkillDamageBoost 是波动类普通伤害及目标回复共享的独立精确倍率规则。
	PulseBasedSkillDamageBoost *PulseBasedSkillDamageBoost `json:"pulseBasedSkillDamageBoost,omitempty"`
	// BiteBasedSkillDamageBoost 是啃咬类普通公式伤害的独立最终倍率规则。
	BiteBasedSkillDamageBoost *BiteBasedSkillDamageBoost `json:"biteBasedSkillDamageBoost,omitempty"`
	// SecondaryEffectsSuppressedDamageBoost 是抑制技能追加效果后提供最终伤害倍率的规则。
	SecondaryEffectsSuppressedDamageBoost *SecondaryEffectsSuppressedDamageBoost `json:"secondaryEffectsSuppressedDamageBoost,omitempty"`
	// SoundBasedSkillDamageReduction 是目标对声音类普通公式伤害使用的独立防守倍率。
	SoundBasedSkillDamageReduction *SoundBasedSkillDamageReduction `json:"soundBasedSkillDamageReduction,omitempty"`
	// SuperEffectiveDamageReduction 是目标仅在最终属性相性严格大于一时使用的防守倍率。
	SuperEffectiveDamageReduction *SuperEffectiveDamageReduction `json:"superEffectiveDamageReduction,omitempty"`
	// FullHPDamageReduction 是目标在每段公式伤害前仍为满生命时使用的防守倍率。
	FullHPDamageReduction *FullHPDamageReduction `json:"fullHPDamageReduction,omitempty"`
	// DamageClassDamageReduction 是目标按物理或特殊分类匹配的独立防守倍率。
	DamageClassDamageReduction *DamageClassDamageReduction `json:"damageClassDamageReduction,omitempty"`
	// ElementSkillDamageReduction 是目标按技能当前有效属性集合匹配的独立防守倍率。
	ElementSkillDamageReduction *ElementSkillDamageReduction `json:"elementSkillDamageReduction,omitempty"`
	// ContactBasedSkillDamageReduction 是目标按本次有效接触事实匹配的独立防守倍率。
	ContactBasedSkillDamageReduction *ContactBasedSkillDamageReduction `json:"contactBasedSkillDamageReduction,omitempty"`
	// AttackingStatMultiplier 是持有成员自身在普通公式中使用的条件攻击能力倍率。
	AttackingStatMultiplier *AttackingStatMultiplier `json:"attackingStatMultiplier,omitempty"`
	// OpponentAttackingStatMultiplier 是目标特性对攻击者公式能力施加的倍率。
	OpponentAttackingStatMultiplier *OpponentAttackingStatMultiplier `json:"opponentAttackingStatMultiplier,omitempty"`
	// DefendingStatMultiplier 是持有成员自身在普通公式中使用的条件防守能力倍率。
	DefendingStatMultiplier *DefendingStatMultiplier `json:"defendingStatMultiplier,omitempty"`
	// OpponentDefendingStatMultiplier 是攻击方特性对目标公式防守能力施加的倍率。
	OpponentDefendingStatMultiplier *OpponentDefendingStatMultiplier `json:"opponentDefendingStatMultiplier,omitempty"`
	// AllySkillDamageBoost 是伙伴按伤害分类为同侧其它上场成员提供的最终伤害倍率。
	AllySkillDamageBoost *AllySkillDamageBoost `json:"allySkillDamageBoost,omitempty"`
	// AllyReceivedDamageReduction 是伙伴为同侧其它上场目标提供的最终伤害倍率。
	AllyReceivedDamageReduction *AllyReceivedDamageReduction `json:"allyReceivedDamageReduction,omitempty"`
	// AllyAbilityPresenceAttackingStatMultiplier 是匹配互助组伙伴在场时启用的攻击能力倍率。
	AllyAbilityPresenceAttackingStatMultiplier *AllyAbilityPresenceAttackingStatMultiplier `json:"allyAbilityPresenceAttackingStatMultiplier,omitempty"`
	// AllyAbilityGroupCode 是当前特性所属的可选互助组稳定代码；空字符串表示不属于任何组。
	AllyAbilityGroupCode string `json:"allyAbilityGroupCode,omitempty"`
	// AccuracyMultiplier 是持有成员使用任意技能时的命中率整数分数修正。
	// nil 表示没有使用者命中修正规则；它不与物理技能专用修正、对手命中修正或能力阶级混用。
	AccuracyMultiplier *AccuracyMultiplier `json:"accuracyMultiplier,omitempty"`
	// PhysicalSkillAccuracyMultiplier 是持有成员仅使用物理技能时的命中率整数分数修正。
	// nil 表示没有物理技能专用规则；变化与特殊技能不能误用该倍率。
	PhysicalSkillAccuracyMultiplier *AccuracyMultiplier `json:"physicalSkillAccuracyMultiplier,omitempty"`
	// OpponentAccuracySandstormMultiplier 是普通沙暴中对手以持有成员为目标时的命中率整数分数修正。
	// nil 表示没有沙暴规则；天气封锁时该规则不会被 Battle Engine 读取。
	OpponentAccuracySandstormMultiplier *AccuracyMultiplier `json:"opponentAccuracySandstormMultiplier,omitempty"`
	// OpponentAccuracySnowMultiplier 是普通降雪中对手以持有成员为目标时的命中率整数分数修正。
	// nil 表示没有降雪规则；它不能借用沙暴字段，因为两种天气有独立的资料与触发生命周期。
	OpponentAccuracySnowMultiplier *AccuracyMultiplier `json:"opponentAccuracySnowMultiplier,omitempty"`
	// OpponentAccuracyConfusionMultiplier 是持有成员处于混乱时对手以其为目标的命中率整数分数修正。
	// nil 表示没有混乱规则；混乱结束后该倍率立即不再参与命中公式。
	OpponentAccuracyConfusionMultiplier *AccuracyMultiplier `json:"opponentAccuracyConfusionMultiplier,omitempty"`
	// AccuracyAlwaysHits 表示持有成员使用技能或成为技能目标时跳过普通命中判定。
	// 一击必杀拥有独立命中公式，不能被此开关绕过。
	AccuracyAlwaysHits bool `json:"accuracyAlwaysHits,omitempty"`
	// StatusSkillAccuracyCap 限制对手以持有成员为目标的变化技能最终命中率。
	// 0 表示没有上限；正值为 1 至 100，且只在普通命中判定的最终阶段应用。
	StatusSkillAccuracyCap int32 `json:"statusSkillAccuracyCap,omitempty"`
	// IgnoreOpponentAccuracyStatStages 表示持有成员在命中判定时忽略对手的命中或闪避能力阶级。
	// 作为使用者时忽略目标闪避，作为目标时忽略使用者命中；真实阶级不被修改。
	IgnoreOpponentAccuracyStatStages bool `json:"ignoreOpponentAccuracyStatStages,omitempty"`
	// CriticalHitImmunity 表示持有成员免疫对手技能造成的击中要害。
	// 它只将已经判定成功的要害降为普通伤害，不跳过原本需要消费的要害随机数，也不阻止技能造成伤害。
	CriticalHitImmunity bool `json:"criticalHitImmunity,omitempty"`
	// SkillRecoilDamageImmunity 表示持有成员免疫按实际造成伤害计算的技能反作用。
	// 它不免疫按最大生命支付的技能代价、天气、异常、道具反伤或其它间接伤害，避免把来源不同的伤害合并。
	SkillRecoilDamageImmunity bool `json:"skillRecoilDamageImmunity,omitempty"`
	// IndirectDamageImmunity 表示持有成员免疫非技能直接伤害。
	// 它覆盖天气、异常、束缚、寄生种子和入场危害，但不影响技能本体伤害、按实际伤害计算的反作用或强制生命代价。
	IndirectDamageImmunity bool `json:"indirectDamageImmunity,omitempty"`
	// ContactDamageToAttackerDenominator 表示目标承受有效接触本体伤害后反伤攻击者最大生命的正分母。
	// 0 表示没有此规则；正值按攻击者最大生命向下取整且至少造成 1 点伤害，不读取目标本次实际损失生命。
	ContactDamageToAttackerDenominator int32 `json:"contactDamageToAttackerDenominator,omitempty"`
	// IgnoreOpponentDamageStatStages 表示持有成员在普通伤害公式中无视对手相关能力阶级。
	// 使用技能时无视目标防御或特防，作为目标时无视使用者攻击或特攻；真实阶级不会被清除或写回。
	IgnoreOpponentDamageStatStages bool `json:"ignoreOpponentDamageStatStages,omitempty"`
	// IgnoreTargetAbilityEffects 表示持有成员使用技能时无视目标侧已实现的防守特性。
	// 它只改变本次技能链路的目标特性读取，不会删除目标规则、阻止目标行动或影响入场与回合末生命周期。
	IgnoreTargetAbilityEffects bool `json:"ignoreTargetAbilityEffects,omitempty"`
	// SurviveFatalDamageAtFullHP 表示持有成员满生命时承受会使其倒下的对手技能伤害后保留 1 HP。
	// 它只处理本体受到的对手技能直接伤害，不阻止替身承伤、天气、异常、陷阱和其它间接伤害。
	SurviveFatalDamageAtFullHP bool `json:"surviveFatalDamageAtFullHP,omitempty"`
	// OpponentStatusSkillImmunity 表示持有成员免疫对手使用的变化技能。
	// 它不阻止同侧辅助、自身目标或伤害技能，且攻击方明确无视目标特性时不参与本次技能结算。
	OpponentStatusSkillImmunity bool `json:"opponentStatusSkillImmunity,omitempty"`
	// NonSuperEffectiveDamageImmunity 表示持有成员免疫属性相性不克制的对手伤害技能。
	// 相性以本次技能有效属性和冻结规则表计算；变化技能、克制伤害和攻击方无视目标特性的技能不受影响。
	NonSuperEffectiveDamageImmunity bool `json:"nonSuperEffectiveDamageImmunity,omitempty"`
	// CriticalHitStageBoost 是持有成员固定获得的击中要害等级。
	// 0 表示没有规则；正值会在每段伤害的要害判定前与技能自身等级相加，不能与技能的临时增益混用。
	CriticalHitStageBoost int32 `json:"criticalHitStageBoost,omitempty"`
	// MultiHitMaximum 表示持有成员使用可变连续命中技能时固定采用资料声明的最大段数。
	// 它不改写技能资料的最小段数，也不影响本来就是固定段数的技能。
	MultiHitMaximum bool `json:"multiHitMaximum,omitempty"`
	// DamagingSkillSecondaryEffectImmunity 表示持有成员免疫对手伤害技能施加的追加效果。
	// 它只阻止目标侧的主要异常、能力阶级、畏缩和易变状态，不阻止伤害本体、强制换人或使用者自身效果。
	DamagingSkillSecondaryEffectImmunity bool `json:"damagingSkillSecondaryEffectImmunity,omitempty"`
	// PriorityMoveImmunityForSideEnabled 表示持有成员的特性阻止对手正优先度技能影响本方当前上场成员。
	// 规则在命中、伤害和附加效果随机数之前生效；它不阻止同侧技能，也不会阻止没有目标指向本方成员的技能。
	PriorityMoveImmunityForSideEnabled bool `json:"priorityMoveImmunityForSideEnabled,omitempty"`
	// PriorityMoveImmunityForSideProtectsAllies 表示先制技能侧免疫是否扩展到持有成员当前上场的同侧伙伴。
	// 该字段只有启用规则时才有意义；单独为 true 会产生无法解释的资料，因此由领域校验拒绝。
	PriorityMoveImmunityForSideProtectsAllies bool `json:"priorityMoveImmunityForSideProtectsAllies,omitempty"`
	// StatusSkillMovesLastAndIgnoresTargetAbility 表示持有成员的变化技能在相同优先度内最后行动。
	// 该变化技能结算期间也无视对手防守特性；物理与特殊技能既不会后置，也不会获得特性穿透。
	StatusSkillMovesLastAndIgnoresTargetAbility bool `json:"statusSkillMovesLastAndIgnoresTargetAbility,omitempty"`
	// ContactSkillProtectionBypass 表示持有成员以接触类技能攻击对手时绕过目标个人保护。
	// 该规则不会清除保护、重置连续保护计数或绕过同侧辅助与自身目标；实际接触事实由技能快照保存。
	ContactSkillProtectionBypass bool `json:"contactSkillProtectionBypass,omitempty"`
	// ContactSkillProtectionBypassDamageMultiplier 是成功穿透个人保护时使用的独立伤害倍率。
	// nil 表示穿透后保持完整伤害；没有发生保护穿透时该倍率不参与普通伤害公式。
	ContactSkillProtectionBypassDamageMultiplier *battleengine.DamageFraction `json:"contactSkillProtectionBypassDamageMultiplier,omitempty"`
	// SkillWeatherOverride 是持有成员使用技能时观察到的普通天气；空值表示读取真实有效环境天气。
	// 它不建立环境天气，也不改变其它成员或回合末环境规则。
	SkillWeatherOverride battleengine.WeatherKind `json:"skillWeatherOverride,omitempty"`
	// SkillElementConversion 是持有成员使用技能时执行的单向属性转换及转换专属基础威力倍率。
	// nil 表示没有规则；原生目标属性技能不会获得转换倍率。
	SkillElementConversion *battleengine.SkillElementConversion `json:"skillElementConversion,omitempty"`
	// ContactSuppression 表示持有成员的接触类技能在本次结算中不再构成有效接触。
	// 它只改写运行时接触事实，绝不改写技能资料的静态标签；所有接触相关规则必须通过统一入口读取该结果。
	ContactSuppression bool `json:"contactSuppression,omitempty"`
	// ReceivedContactDamageHalved 表示持有成员受到有效接触伤害时最终伤害减半。
	// 无视目标特性的攻击会跳过该防守特性；非接触、变化技能和不造成伤害的命中不读取该字段。
	ReceivedContactDamageHalved bool `json:"receivedContactDamageHalved,omitempty"`
	// ReceivedFireDamageDoubled 表示持有成员受到火属性技能伤害时最终伤害翻倍。
	// 它只比较技能当前有效属性，天气或其它规则改写属性后应按改写结果判断；无视目标特性会跳过该防守规则。
	ReceivedFireDamageDoubled bool `json:"receivedFireDamageDoubled,omitempty"`
	// ForcedSwitchImmunity 表示持有成员免疫技能及道具触发的强制换人。
	// 它只阻止当前成员被替换，不阻止该成员主动换人、倒下后的强制补位或其它成员的换人效果；因此不能复用
	// 天气免疫、替身或笼统的状态免疫字段。
	ForcedSwitchImmunity bool `json:"forcedSwitchImmunity,omitempty"`
	// OpponentSwitchRestriction 是持有成员限制敌方主动换人的独立规则。
	// nil 表示不限制对手；规则存在性必须由指针保存，避免把“不带条件的全目标限制”误判为没有规则。
	OpponentSwitchRestriction *OpponentSwitchRestriction `json:"opponentSwitchRestriction,omitempty"`
	// DamageCrossedHalfHPForceSelfSwitch 表示持有成员的生命首次从高于二分之一降至二分之一或以下时强制自身换下。
	//
	// 该阈值是“受伤跨越半血”的固定特性语义，不与道具受伤换人、技能强制目标换人或任意生命比例效果共用字段。
	DamageCrossedHalfHPForceSelfSwitch bool `json:"damageCrossedHalfHPForceSelfSwitch,omitempty"`
	// WeatherEndTurnHeal 是持有成员在匹配普通天气的回合末按最大生命固定比例回复的独立规则。
	// nil 表示该特性不提供天气回复；它不能与天气伤害免疫、天气封锁或道具回复合并。
	WeatherEndTurnHeal *WeatherEndTurnHeal `json:"weatherEndTurnHeal,omitempty"`
	// WeatherSpeedMultipliers 是持有成员在匹配普通天气的行动速度整数分数倍率集合。
	// 它只影响回合排序，不能与伤害、命中或天气回复规则共用无语义效果数组。
	WeatherSpeedMultipliers []WeatherSpeedMultiplier `json:"weatherSpeedMultipliers,omitempty"`
	// EnvironmentHighestStatMultiplier 是持有成员在指定普通天气或普通场地下强化最高原始能力的独立规则。
	// nil 表示没有该规则；它不记录资料名称、能力项或浮点倍率，全部运行期语义由冻结的强类型规则表达。
	EnvironmentHighestStatMultiplier *EnvironmentHighestStatMultiplier `json:"environmentHighestStatMultiplier,omitempty"`
	// SwitchInStrongWeather 是持有成员进入场地时建立的独立强天气规则。
	// nil 表示没有入场强天气；强天气没有普通天气的持续回合，且不能与任意普通天气规则字段混用。
	SwitchInStrongWeather *SwitchInStrongWeather `json:"switchInStrongWeather,omitempty"`
	// SwitchInWeather 是持有成员进入场地时建立的独立普通天气规则。
	// nil 表示没有入场普通天气；它与技能天气和强天气拥有不同的来源与触发生命周期。
	SwitchInWeather *SwitchInWeather `json:"switchInWeather,omitempty"`
	// SwitchInTerrain 是持有成员进入场地时建立的独立普通场地规则。
	// nil 表示没有入场普通场地；它与技能场地和天气规则拥有独立的来源与持续生命周期。
	SwitchInTerrain *SwitchInTerrain `json:"switchInTerrain,omitempty"`
	// SwitchInStatStageChange 是持有成员进入场地时立即执行的独立能力阶级变化规则。
	// nil 表示没有该规则；它没有技能概率，也不能通过技能能力变化或自由文本推断目标集合。
	SwitchInStatStageChange *SwitchInStatStageChange `json:"switchInStatStageChange,omitempty"`
	// SwitchInAllyHeal 是持有成员进入场地时为同侧其它上场成员回复生命的独立规则。
	// nil 表示没有该规则；它与技能、天气和场地回复的目标集合及触发生命周期均不相同。
	SwitchInAllyHeal *SwitchInAllyHeal `json:"switchInAllyHeal,omitempty"`
	// SwitchInOpponentDefenseComparisonBoost 表示持有成员入场时比较对手防御并提升自身攻击或特攻。
	// 它只读取场上对手的基础防御资料，不与技能能力变化或伤害倍率共享结构。
	SwitchInOpponentDefenseComparisonBoost bool `json:"switchInOpponentDefenseComparisonBoost,omitempty"`
	// SwitchInAllyStatStageCopy 表示持有成员进入场地时复制同侧其它上场成员的全部能力阶级。
	// false 表示没有该规则；它与定向能力阶级变化及防御比较强化具有不同目标选择和覆盖语义。
	SwitchInAllyStatStageCopy bool `json:"switchInAllyStatStageCopy,omitempty"`
	// SwitchInAllyStatStageReset 表示持有成员进入场地时将同侧其它上场成员的全部能力阶级重置为零。
	// false 表示没有该规则；它不影响触发者自身或后备成员，且与复制规则具有独立目标和写入语义。
	SwitchInAllyStatStageReset bool `json:"switchInAllyStatStageReset,omitempty"`
	// SwitchInClearAllSideDamageReductions 表示持有成员进入场地时清除双方阵营的全部减伤屏障。
	// false 表示没有该规则；它只包含反射壁、光墙和极光幕，不与顺风、入场危害或技能清除浓雾混用。
	SwitchInClearAllSideDamageReductions bool `json:"switchInClearAllSideDamageReductions,omitempty"`
	// SwitchInCopyOpponentAbility 表示持有成员进入场地时复制一名存活上场对手的当前特性及其冻结规则。
	// false 表示没有该规则；复制在引擎快照内完成，不读取实时资料，也不复制技能、道具或基础属性。
	SwitchInCopyOpponentAbility bool `json:"switchInCopyOpponentAbility,omitempty"`
	// SwitchInRevealOpponentHeldItems 表示持有成员进入场地时公开所有存活上场对手的持有道具。
	// false 表示没有该规则；公开只产生事件，不改变对手的道具或运行态。
	SwitchInRevealOpponentHeldItems bool `json:"switchInRevealOpponentHeldItems,omitempty"`
	// SwitchInRevealOpponentHighestPowerSkill 表示入场时公开对手当前最高威力技能；选择依据冻结快照。
	SwitchInRevealOpponentHighestPowerSkill bool `json:"switchInRevealOpponentHighestPowerSkill,omitempty"`
	// SwitchInTransformIntoOpponent 表示入场时复制一名存活上场对手的完整战斗画像。
	// 变身只存在于当前连续上场周期，成员离场时由战斗引擎恢复原始画像。
	SwitchInTransformIntoOpponent bool `json:"switchInTransformIntoOpponent,omitempty"`
	// SwitchInDetectDangerousOpponentSkill 表示入场时侦测一项对自身危险的对手技能。
	// 侦测只产生结构化公开事件，不改变对手技能、PP 或成员运行态。
	SwitchInDetectDangerousOpponentSkill bool `json:"switchInDetectDangerousOpponentSkill,omitempty"`
	// SwitchInDisguiseAsLastHealthyAlly 表示入场时伪装为同侧最后一名可战斗队友。
	// 它只影响对外披露身份，不能改变真实种类或战斗计算。
	SwitchInDisguiseAsLastHealthyAlly bool `json:"switchInDisguiseAsLastHealthyAlly,omitempty"`
	// SwitchInHeldItemElementIdentity 表示入场时按所持道具的属性伤害强化身份替换自身属性。
	//
	// 特性只声明“允许解释”的开关；道具的属性身份仍独立保存在 Item Metadata，并在 Battle 启动时冻结，
	// 防止特性文本、道具说明或运行中的资料修改改变已经开始的对局。
	SwitchInHeldItemElementIdentity bool `json:"switchInHeldItemElementIdentity,omitempty"`
	// SwitchOutMajorStatusCure 表示持有成员成功离场时清除自身主要异常状态。
	// 倒下后的补位不属于成功离场，不能借由该开关恢复倒下成员。
	SwitchOutMajorStatusCure bool `json:"switchOutMajorStatusCure,omitempty"`
	// SwitchOutHealDenominator 是持有成员成功离场时按最大生命回复的正分母。
	// 0 表示没有离场回复规则；它与入场同侧回复、天气回复及技能生命效果具有不同的目标与结算窗口。
	SwitchOutHealDenominator int32 `json:"switchOutHealDenominator,omitempty"`
	// SwitchOutFormChange 是持有成员成功离场时触发的确定形态切换规则。
	// nil 表示没有该规则；它不与入场或天气形态切换复用同一个生命周期。
	SwitchOutFormChange *SwitchOutFormChange `json:"switchOutFormChange,omitempty"`
	// SwitchInFormChange 是持有成员进入场地时立即切换到指定形态的独立规则。
	// nil 表示没有该规则；它同天气形态、太晶和道具形态保持不同的触发与生命语义。
	SwitchInFormChange *SwitchInFormChange `json:"switchInFormChange,omitempty"`
	// WeatherFormChange 是持有成员按当前有效普通天气同步形态的独立规则。
	// nil 表示没有该规则；天气封锁时该规则会回到明确默认形态，不能把天气生命周期编码到本结构。
	WeatherFormChange *WeatherFormChange `json:"weatherFormChange,omitempty"`
	// TerastallizationStatStageChange 是持有成员完成太晶化后立即改变自身能力阶级的独立规则。
	// nil 表示没有该规则；它仅在太晶化实际成功时触发，不能与入场或技能能力阶级变化混用。
	TerastallizationStatStageChange *TerastallizationStatStageChange `json:"terastallizationStatStageChange,omitempty"`
	// TerastallizationEnvironmentClear 表示持有成员完成太晶化后清除普通天气和普通场地。
	// false 表示没有该规则；强天气不属于清除范围，且环境清除不能由文本说明推导。
	TerastallizationEnvironmentClear bool `json:"terastallizationEnvironmentClear,omitempty"`
}

// RuleSet 是从 Ability 主资源规则文档解出的强类型规则集合。
// AbilityID 和 Version 仅标识规则来源，不代表独立持久化实体。
type RuleSet struct {
	ID        snowflake.ID
	AbilityID snowflake.ID
	OptionalValues
	Version int64
}

// Change 表示规则消息更新映射对一个可空字段的省略、清空或替换意图。
type Change[T any] struct {
	Specified bool
	Value     *T
}

func normalizeValues(values OptionalValues) OptionalValues {
	values.Effect = normalizeText(values.Effect)
	values.ShortEffect = normalizeText(values.ShortEffect)
	values.Introduction = normalizeText(values.Introduction)
	values.WeatherDamageImmunities = cloneWeatherDamageImmunities(values.WeatherDamageImmunities)
	values.ReactiveAbilityRules = battleengine.CloneReactiveAbilityRules(values.ReactiveAbilityRules)
	values.ContactSkillProtectionBypassDamageMultiplier = cloneProtectionBypassDamageMultiplier(values.ContactSkillProtectionBypassDamageMultiplier)
	values.SkillElementConversion = cloneSkillElementConversion(values.SkillElementConversion)
	values.BasePowerAtMostDamageBoost = cloneBasePowerAtMostDamageBoost(values.BasePowerAtMostDamageBoost)
	values.RecoilSkillDamageBoost = cloneRecoilSkillDamageBoost(values.RecoilSkillDamageBoost)
	values.LowHPElementDamageBoost = cloneLowHPElementDamageBoost(values.LowHPElementDamageBoost)
	values.WeatherElementDamageBoost = cloneWeatherElementDamageBoost(values.WeatherElementDamageBoost)
	values.ElementSkillDamageBoost = cloneElementSkillDamageBoost(values.ElementSkillDamageBoost)
	values.SameElementBonusOverride = cloneSameElementBonusOverride(values.SameElementBonusOverride)
	values.ContactBasedSkillDamageBoost = cloneContactBasedSkillDamageBoost(values.ContactBasedSkillDamageBoost)
	values.CriticalHitDamageBoost = cloneCriticalHitDamageBoost(values.CriticalHitDamageBoost)
	values.SuperEffectiveDamageBoost = cloneSuperEffectiveDamageBoost(values.SuperEffectiveDamageBoost)
	values.NotVeryEffectiveDamageBoost = cloneNotVeryEffectiveDamageBoost(values.NotVeryEffectiveDamageBoost)
	values = cloneFormulaMultiplierValues(values)
	values.AccuracyMultiplier = cloneAccuracyMultiplier(values.AccuracyMultiplier)
	values.PhysicalSkillAccuracyMultiplier = cloneAccuracyMultiplier(values.PhysicalSkillAccuracyMultiplier)
	values.OpponentAccuracySandstormMultiplier = cloneAccuracyMultiplier(values.OpponentAccuracySandstormMultiplier)
	values.OpponentAccuracySnowMultiplier = cloneAccuracyMultiplier(values.OpponentAccuracySnowMultiplier)
	values.OpponentAccuracyConfusionMultiplier = cloneAccuracyMultiplier(values.OpponentAccuracyConfusionMultiplier)
	values.OpponentSwitchRestriction = cloneOpponentSwitchRestriction(values.OpponentSwitchRestriction)
	values.WeatherEndTurnHeal = cloneWeatherEndTurnHeal(values.WeatherEndTurnHeal)
	values.WeatherSpeedMultipliers = cloneWeatherSpeedMultipliers(values.WeatherSpeedMultipliers)
	values.EnvironmentHighestStatMultiplier = cloneEnvironmentHighestStatMultiplier(values.EnvironmentHighestStatMultiplier)
	values.SwitchInStrongWeather = cloneSwitchInStrongWeather(values.SwitchInStrongWeather)
	values.SwitchInWeather = cloneSwitchInWeather(values.SwitchInWeather)
	values.SwitchInTerrain = cloneSwitchInTerrain(values.SwitchInTerrain)
	values.SwitchInStatStageChange = cloneSwitchInStatStageChange(values.SwitchInStatStageChange)
	values.SwitchInAllyHeal = cloneSwitchInAllyHeal(values.SwitchInAllyHeal)
	values.SwitchInFormChange = cloneSwitchInFormChange(values.SwitchInFormChange)
	values.WeatherFormChange = cloneWeatherFormChange(values.WeatherFormChange)
	values.TerastallizationStatStageChange = cloneTerastallizationStatStageChange(values.TerastallizationStatStageChange)
	values.SwitchOutFormChange = cloneSwitchOutFormChange(values.SwitchOutFormChange)
	return values
}

// NormalizeForRules 规范化并校验从 Ability rules 文档读取的完整战斗规则。
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
	// 详细效果承载资料源的完整规则说明；保留较高的应用层上限用于阻止异常请求，
	// 但不能再以旧数据库的两千字符限制截断合法资料。
	return validText(values.Effect, 20_000) && validText(values.ShortEffect, 500) &&
		validText(values.Introduction, 500) && validWeatherDamageImmunities(values.WeatherDamageImmunities) &&
		battleengine.ValidateReactiveAbilityRules(values.ReactiveAbilityRules) == nil &&
		validSkillUseAbilityRules(values) &&
		validAbilityDamageMultiplierValues(values) &&
		validFormulaMultiplierValues(values) &&
		validAccuracyMultiplier(values.AccuracyMultiplier) && validAccuracyMultiplier(values.PhysicalSkillAccuracyMultiplier) &&
		validAccuracyMultiplier(values.OpponentAccuracySandstormMultiplier) && validAccuracyMultiplier(values.OpponentAccuracySnowMultiplier) &&
		validAccuracyMultiplier(values.OpponentAccuracyConfusionMultiplier) &&
		values.StatusSkillAccuracyCap >= 0 && values.StatusSkillAccuracyCap <= 100 &&
		values.CriticalHitStageBoost >= 0 && values.CriticalHitStageBoost <= 6 &&
		(!values.PriorityMoveImmunityForSideProtectsAllies || values.PriorityMoveImmunityForSideEnabled) &&
		validOpponentSwitchRestriction(values.OpponentSwitchRestriction) &&
		validWeatherEndTurnHeal(values.WeatherEndTurnHeal) && validWeatherSpeedMultipliers(values.WeatherSpeedMultipliers) &&
		validEnvironmentHighestStatMultiplier(values.EnvironmentHighestStatMultiplier) &&
		validSwitchInStrongWeather(values.SwitchInStrongWeather) && validSwitchInWeather(values.SwitchInWeather) &&
		validSwitchInTerrain(values.SwitchInTerrain) && validSwitchInStatStageChange(values.SwitchInStatStageChange) &&
		validSwitchInAllyHeal(values.SwitchInAllyHeal) && validSwitchInFormChange(values.SwitchInFormChange) &&
		validWeatherFormChange(values.WeatherFormChange) &&
		validTerastallizationStatStageChange(values.TerastallizationStatStageChange) &&
		validContactDamageToAttackerDenominator(values.ContactDamageToAttackerDenominator) &&
		validSwitchOutHealDenominator(values.SwitchOutHealDenominator) && validSwitchOutFormChange(values.SwitchOutFormChange) &&
		(values.SwitchInStrongWeather == nil || values.SwitchInWeather == nil)
}

// ValidForBattle 报告这一组特性可选资料能否安全进入新的 Battle。
//
// 管理写入服务和 Battle 冻结边界共同调用同一套严格校验，防止手工数据库修改、测试替身或未来新增写入入口
// 绕过领域命令后，把损坏的分数、重复集合或未知枚举静默解释为无效果。
func (values OptionalValues) ValidForBattle() bool {
	return validValues(values)
}

// cloneProtectionBypassDamageMultiplier 深复制保护穿透伤害倍率，避免调用者在命令提交后修改已接收资料。
func cloneProtectionBypassDamageMultiplier(value *battleengine.DamageFraction) *battleengine.DamageFraction {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneSkillElementConversion 深复制技能属性转换规则，隔离管理命令、持久化记录和 Battle 快照的所有权。
func cloneSkillElementConversion(value *battleengine.SkillElementConversion) *battleengine.SkillElementConversion {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// validSkillUseAbilityRules 校验三项技能使用型特性规则及其跨字段约束。
func validSkillUseAbilityRules(values OptionalValues) bool {
	if battleengine.ValidateDamageFraction(values.ContactSkillProtectionBypassDamageMultiplier) != nil ||
		(values.ContactSkillProtectionBypassDamageMultiplier != nil && !values.ContactSkillProtectionBypass) {
		return false
	}
	switch values.SkillWeatherOverride {
	case "", battleengine.WeatherKindSun, battleengine.WeatherKindRain, battleengine.WeatherKindSandstorm, battleengine.WeatherKindSnow:
	default:
		return false
	}
	conversion := values.SkillElementConversion
	if battleengine.ValidateSkillElementConversion(conversion) != nil {
		return false
	}
	if conversion == nil {
		return true
	}
	return conversion.SourceElementID.IsValid() && conversion.TargetElementID.IsValid()
}

// changeSkillWeatherOverrideValue 提取技能局部天气完整替换值；未指定或显式清除都表示不覆盖真实天气。
func validContactDamageToAttackerDenominator(value int32) bool {
	return value >= 0 && value <= 65_535
}

// changeIgnoreOpponentDamageStatStagesValue 提取无视对手伤害能力阶级开关的完整替换值。
func validText(value *string, maximum int) bool {
	return value == nil || len([]rune(*value)) <= maximum
}
