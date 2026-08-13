package battle

import (
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// ErrInitialStateCompilation 表示冻结的 Battle、Preview 或实时资料事实无法组成合法的战斗初始状态。
var ErrInitialStateCompilation = errors.New("对战初始状态编译失败")

// BattleMemberFacts 是实时资料解析完成后进入战斗引擎的一名成员完整事实。
//
// 它与 Team.Member 保持分离：Team 只保存玩家选择的稳定引用和培养输入；本结构保存本场开始时
// 已解析的数值、技能效果、属性和资源。二者的变化节奏与所有权不同，不能合并为通用资料结构。
type BattleMemberFacts struct {
	// Position 是成员在冻结 Team Snapshot 中的稳定位置，不因 Preview 筛选或换人重新编号。
	Position battleengine.MemberPosition
	// CreatureID 是解析后的精灵稳定 Identifier。
	CreatureID snowflake.ID
	// NatureID 是本场已应用到最终能力且冻结的 Nature 稳定 Identifier。
	NatureID snowflake.ID
	// GenderCode 是 Team 选择并由 Current Game Data 验证后冻结的性别稳定代码；空字符串表示无性别。
	GenderCode string
	// Level 是应用赛制等级规则后的本场固定等级。
	Level uint8
	// MaxHP 是包含等级、基础属性和培养值计算结果的最大生命值。
	MaxHP uint32
	// Stats 是包含等级、基础属性和培养值计算结果的其余五项战斗能力。
	Stats battleengine.StatBlock
	// Weight 是从启用精灵资料冻结的体重整数刻度；动态体重威力规则与引擎 MemberSnapshot 使用同一口径。
	Weight uint32
	// ElementIDs 是成员在本场战斗生效的一至两个属性稳定 Identifier。
	ElementIDs []battleengine.Identifier
	// TeraElementID 是 Team 为该成员选择并由实时属性资料验证过的太晶属性稳定 Identifier。
	// 它不由玩家回合请求提供；只有赛制冻结了太晶化机制时，Battle Engine 才允许消耗该成员所属阵营的机会。
	TeraElementID snowflake.ID
	// FormProfiles 是本成员本场可切换到的完整冻结形态画像。
	// 仅当特性声明入场形态或天气形态规则时才会填充；每一项均在 Battle 启动时使用本成员的培养值、等级和
	// 对应精灵资料计算，运行中的 Battle Engine 不会再次访问游戏资料。
	FormProfiles []battleengine.FormProfile
	// Skills 是按 Team 技能槽位解析的完整战斗技能快照。
	Skills []battleengine.SkillSnapshot
	// AbilityID 是本场开始时冻结的特性稳定 Identifier；没有特性时为零值。
	AbilityID snowflake.ID
	// WeatherDamageImmunities 是特性在本场开始时冻结的普通天气回合末伤害免疫集合。
	// 它与 AbilityID 一同由实时特性详情编译而来，不能在引擎内再查询或根据特性名称推断。
	WeatherDamageImmunities []battleengine.WeatherKind
	// WeatherEffectsSuppressed 是特性在本场开始时冻结的普通天气封锁开关。
	// 它不删除天气环境，而是在引擎内只暂停普通天气的可执行效果。
	WeatherEffectsSuppressed bool
	// ReactiveAbilityRules 是 Current Game Data 在开战时冻结的回合末、受伤与倒下触发规则。
	// Battle 只负责深复制；规则执行和运行态推进完全属于纯 Battle Engine。
	ReactiveAbilityRules *battleengine.ReactiveAbilityRules
	// AccuracyMultiplier 是特性在本场开始时冻结的任意技能命中率精确分数。
	// nil 表示没有该规则；它只影响普通命中判定，不能替代技能原生必中或一击必杀规则。
	AccuracyMultiplier *battleengine.AccuracyMultiplier
	// PhysicalSkillAccuracyMultiplier 是特性在本场开始时冻结的物理技能命中率精确分数。
	// nil 表示没有该规则，特殊与变化技能不会读取它。
	PhysicalSkillAccuracyMultiplier *battleengine.AccuracyMultiplier
	// BasePowerAtMostDamageBoost 是特性按技能原始基础威力上限触发的最终伤害倍率。
	BasePowerAtMostDamageBoost *battleengine.BasePowerAtMostDamageBoost
	// RecoilSkillDamageBoost 是特性对按实际伤害产生反作用的技能提供的最终伤害倍率。
	RecoilSkillDamageBoost *battleengine.RecoilSkillDamageBoost
	// LowHPElementDamageBoost 是特性在低生命阈值下对指定有效属性提供的最终伤害倍率。
	LowHPElementDamageBoost *battleengine.LowHPElementDamageBoost
	// WeatherElementDamageBoost 是特性在指定天气下对一组有效属性提供的最终伤害倍率。
	WeatherElementDamageBoost *battleengine.WeatherElementDamageBoost
	// ElementSkillDamageBoost 是特性对一组技能有效属性提供的最终伤害倍率。
	ElementSkillDamageBoost *battleengine.ElementSkillDamageBoost
	// SameElementBonusOverride 是特性替换默认属性一致加成的精确倍率。
	SameElementBonusOverride *battleengine.SameElementBonusOverride
	// ContactBasedSkillDamageBoost 是特性对本次仍构成有效接触的技能提供的最终伤害倍率。
	ContactBasedSkillDamageBoost *battleengine.ContactBasedSkillDamageBoost
	// CriticalHitDamageBoost 是特性对本次实际击中要害提供的额外最终伤害倍率。
	CriticalHitDamageBoost *battleengine.CriticalHitDamageBoost
	// SuperEffectiveDamageBoost 是特性对最终严格克制的技能提供的最终伤害倍率。
	SuperEffectiveDamageBoost *battleengine.SuperEffectiveDamageBoost
	// NotVeryEffectiveDamageBoost 是特性对最终非零抗性技能提供的最终伤害倍率。
	NotVeryEffectiveDamageBoost *battleengine.NotVeryEffectiveDamageBoost
	// TargetGenderDamageMultiplier 是本场冻结的性别关系最终伤害倍率。
	TargetGenderDamageMultiplier *battleengine.TargetGenderDamageMultiplier
	// PunchBasedSkillDamageBoost 是本场冻结的拳击类技能最终伤害倍率。
	PunchBasedSkillDamageBoost *battleengine.PunchBasedSkillDamageBoost
	// SlicingBasedSkillDamageBoost 是本场冻结的切割类技能最终伤害倍率。
	SlicingBasedSkillDamageBoost *battleengine.SlicingBasedSkillDamageBoost
	// SoundBasedSkillDamageBoost 是本场冻结的声音类技能攻击方伤害倍率。
	SoundBasedSkillDamageBoost *battleengine.SoundBasedSkillDamageBoost
	// PulseBasedSkillDamageBoost 是本场冻结的波动类技能伤害及回复倍率。
	PulseBasedSkillDamageBoost *battleengine.PulseBasedSkillDamageBoost
	// BiteBasedSkillDamageBoost 是本场冻结的啃咬类技能最终伤害倍率。
	BiteBasedSkillDamageBoost *battleengine.BiteBasedSkillDamageBoost
	// SecondaryEffectsSuppressedDamageBoost 是本场冻结的追加效果抑制增伤倍率。
	SecondaryEffectsSuppressedDamageBoost *battleengine.SecondaryEffectsSuppressedDamageBoost
	// SoundBasedSkillDamageReduction 是本场冻结的声音类技能防守倍率。
	SoundBasedSkillDamageReduction *battleengine.SoundBasedSkillDamageReduction
	// SuperEffectiveDamageReduction 是本场冻结的严格克制防守倍率。
	SuperEffectiveDamageReduction *battleengine.SuperEffectiveDamageReduction
	// FullHPDamageReduction 是本场冻结的逐段满生命防守倍率。
	FullHPDamageReduction *battleengine.FullHPDamageReduction
	// DamageClassDamageReduction 是本场冻结的伤害分类防守倍率。
	DamageClassDamageReduction *battleengine.DamageClassDamageReduction
	// ElementSkillDamageReduction 是本场冻结的有效属性防守倍率。
	ElementSkillDamageReduction *battleengine.ElementSkillDamageReduction
	// ContactBasedSkillDamageReduction 是本场冻结的有效接触防守倍率。
	ContactBasedSkillDamageReduction *battleengine.ContactBasedSkillDamageReduction
	// AttackingStatMultiplier 是本场冻结的持有者攻击侧条件能力倍率。
	AttackingStatMultiplier *battleengine.AttackingStatMultiplier
	// OpponentAttackingStatMultiplier 是本场冻结的目标特性攻击能力倍率。
	OpponentAttackingStatMultiplier *battleengine.OpponentAttackingStatMultiplier
	// DefendingStatMultiplier 是本场冻结的持有者防守侧条件能力倍率。
	DefendingStatMultiplier *battleengine.DefendingStatMultiplier
	// OpponentDefendingStatMultiplier 是本场冻结的攻击方特性目标防守能力倍率。
	OpponentDefendingStatMultiplier *battleengine.OpponentDefendingStatMultiplier
	// AllySkillDamageBoost 是本场冻结的伙伴分类增伤倍率。
	AllySkillDamageBoost *battleengine.AllySkillDamageBoost
	// AllyReceivedDamageReduction 是本场冻结的伙伴承伤倍率。
	AllyReceivedDamageReduction *battleengine.AllyReceivedDamageReduction
	// AllyAbilityGroupCode 是本场冻结的互助组成员资格代码。
	AllyAbilityGroupCode string
	// AllyAbilityPresenceAttackingStatMultiplier 是本场冻结的互助组伙伴攻击能力倍率。
	AllyAbilityPresenceAttackingStatMultiplier *battleengine.AllyAbilityPresenceAttackingStatMultiplier
	// OpponentAccuracySandstormMultiplier 是普通沙暴中以本成员为目标的对手技能命中率精确分数。
	OpponentAccuracySandstormMultiplier *battleengine.AccuracyMultiplier
	// OpponentAccuracySnowMultiplier 是普通降雪中以本成员为目标的对手技能命中率精确分数。
	OpponentAccuracySnowMultiplier *battleengine.AccuracyMultiplier
	// OpponentAccuracyConfusionMultiplier 是本成员混乱时以其为目标的对手技能命中率精确分数。
	OpponentAccuracyConfusionMultiplier *battleengine.AccuracyMultiplier
	// AccuracyAlwaysHits 表示特性在本场开始时冻结的普通命中跳过开关。
	AccuracyAlwaysHits bool
	// StatusSkillAccuracyCap 是特性冻结的对手变化技能最终命中上限；0 表示没有规则。
	StatusSkillAccuracyCap uint8
	// IgnoreOpponentAccuracyStatStages 表示特性在命中判定时无视对手命中或闪避阶级。
	IgnoreOpponentAccuracyStatStages bool
	// CriticalHitImmunity 表示特性在本场开始时冻结的击中要害免疫开关。
	// 它只将已判定的要害降为普通伤害，保持随机轨迹与非要害伤害语义不变。
	CriticalHitImmunity bool
	// SkillRecoilDamageImmunity 表示特性在本场开始时冻结的技能反作用伤害免疫开关。
	// 它只阻止按实际伤害计算的技能反作用，不影响其它间接伤害或按最大生命支付的技能代价。
	SkillRecoilDamageImmunity bool
	// IndirectDamageImmunity 表示特性在本场开始时冻结的非技能直接伤害免疫开关。
	// 它覆盖天气、异常、束缚、寄生种子和入场危害，但不影响技能本体、按实际伤害计算的反作用或强制生命代价。
	IndirectDamageImmunity bool
	// ContactDamageToAttackerDenominator 表示成员受到有效接触技能本体伤害后，对攻击者造成最大生命固定比例伤害的分母。
	// 0 表示未声明该特性规则；正值会由战斗引擎以攻击者最大生命除以该分母计算反制伤害。
	ContactDamageToAttackerDenominator uint16
	// IgnoreOpponentDamageStatStages 表示特性在本场开始时冻结的无视对手伤害能力阶级开关。
	// 它只影响当前普通物理或特殊伤害公式，不会修改成员权威状态保存的真实能力阶级。
	IgnoreOpponentDamageStatStages bool
	// IgnoreTargetAbilityEffects 表示特性在本场开始时冻结的无视目标防守特性开关。
	// 它只在持有成员使用技能时读取，不能关闭目标在自身行动、入场或回合末应执行的规则。
	IgnoreTargetAbilityEffects bool
	// SurviveFatalDamageAtFullHP 表示特性在本场开始时冻结的满生命致命伤害保留 1 HP 开关。
	// 它仅在对手技能将满生命成员本体降为 0 前读取，不能替代替身或间接伤害的结算规则。
	SurviveFatalDamageAtFullHP bool
	// OpponentStatusSkillImmunity 表示特性在本场开始时冻结的免疫对手变化技能开关。
	// 它只在对手变化技能开始影响当前成员前读取，不会阻止同侧辅助、自身目标或伤害技能。
	OpponentStatusSkillImmunity bool
	// NonSuperEffectiveDamageImmunity 表示特性在本场开始时冻结的免疫非克制伤害技能开关。
	// 引擎会按冻结相性表和当前有效环境判断，不会把变化技能或克制伤害错误阻止。
	NonSuperEffectiveDamageImmunity bool
	// CriticalHitStageBoost 是特性在本场开始时冻结的固定击中要害等级增益；0 表示没有规则。
	// 它只影响持有成员作为技能使用者时的要害概率，不修改技能资料或目标方的要害免疫。
	CriticalHitStageBoost uint8
	// MultiHitMaximum 表示特性在本场开始时冻结的可变连续命中取最大段数开关。
	// 它仅改变持有成员作为使用者时的段数选择，不改变已冻结技能声明的合法范围。
	MultiHitMaximum bool
	// DamagingSkillSecondaryEffectImmunity 表示特性在本场开始时冻结的伤害技能追加效果免疫开关。
	// 它只阻止对手伤害技能施加给当前成员的追加状态和能力变化，不阻止伤害本体或使用者自身效果。
	DamagingSkillSecondaryEffectImmunity bool
	// PriorityMoveImmunityForSideEnabled 表示特性在本场开始时冻结的先制技能侧免疫开关。
	// 它在目标命中前阻止对手正优先度技能，且只读取当前上场成员，避免后备成员错误参与场上规则。
	PriorityMoveImmunityForSideEnabled bool
	// PriorityMoveImmunityForSideProtectsAllies 表示冻结的先制技能侧免疫是否也保护当前上场的同侧伙伴。
	// false 保持“仅保护持有成员”语义，不能与规则启用状态折叠为一个布尔字段。
	PriorityMoveImmunityForSideProtectsAllies bool
	// StatusSkillMovesLastAndIgnoresTargetAbility 表示冻结的变化技能后置及特性穿透规则。
	// 运行时只在持有成员实际使用变化技能时读取它，防止物理或特殊技能意外获得后置或防守特性穿透。
	StatusSkillMovesLastAndIgnoresTargetAbility bool
	// ContactSkillProtectionBypass 表示冻结的接触技能穿透目标个人保护规则。
	// 它只在成员以接触类技能攻击对手时读取，不能移除保护状态或被解释为对侧防护的通用穿透。
	ContactSkillProtectionBypass bool
	// ContactSkillProtectionBypassDamageMultiplier 是成功穿透个人保护时冻结的独立伤害倍率。
	// nil 表示保持完整伤害；没有实际穿透保护时不能读取该倍率。
	ContactSkillProtectionBypassDamageMultiplier *battleengine.DamageFraction
	// SkillWeatherOverride 是成员使用技能时冻结的普通天气语义；空值表示读取真实有效环境天气。
	SkillWeatherOverride battleengine.WeatherKind
	// SkillElementConversion 是成员使用技能时冻结的单向属性转换及转换专属基础威力倍率。
	SkillElementConversion *battleengine.SkillElementConversion
	// ContactSuppression 表示冻结的特性接触抑制规则。
	// 它只改写本次结算的有效接触事实，不能写回技能资料或被解释为对非接触效果的通用免疫。
	ContactSuppression bool
	// ReceivedContactDamageHalved 表示冻结的特性承受接触伤害减半规则。
	// 它只对目标侧受到的有效接触伤害生效，攻击者无视目标特性时必须跳过该规则。
	ReceivedContactDamageHalved bool
	// ReceivedFireDamageDoubled 表示冻结的特性承受火属性伤害翻倍规则。
	// 它只对目标侧受到的当前有效火属性伤害生效，攻击者无视目标特性时必须跳过该规则。
	ReceivedFireDamageDoubled bool
	// ForcedSwitchImmunity 是特性在本场开始时冻结的强制换人免疫开关。
	// 它只阻止该成员被技能或道具强制换下，不能替代主动换人、倒下补位或笼统的状态免疫规则。
	ForcedSwitchImmunity bool
	// OpponentSwitchRestriction 是特性在本场开始时冻结的敌方主动换人限制规则。
	// nil 表示该成员不会限制敌方主动换人；非 nil 的零值规则仍合法表示无条件限制，不能以字段零值推断规则不存在。
	OpponentSwitchRestriction *battleengine.OpponentSwitchRestriction
	// DamageCrossedHalfHPForceSelfSwitch 表示特性在本场开始时冻结的半血跨越强制自换开关。
	// 引擎只会在成员本体生命首次从高于二分之一降至二分之一或以下时读取它，替身伤害不会触发该规则。
	DamageCrossedHalfHPForceSelfSwitch bool
	// SwitchOutMajorStatusCure 表示特性在本场开始时冻结的成功离场主要异常净化开关。
	// 它只在仍存活成员完成实际换出时结算，倒下后的补位不会触发。
	SwitchOutMajorStatusCure bool
	// SwitchOutHealDenominator 是特性在本场开始时冻结的成功离场固定比例回复分母。
	// 0 表示没有规则；Battle 会在编译资料时限制范围，Battle Engine 不再访问实时特性资料。
	SwitchOutHealDenominator uint16
	// WeatherEndTurnHealing 是特性在本场开始时冻结的普通天气回合末固定比例回复规则。
	// nil 表示没有该规则；它必须由实时特性详情显式编译，不能通过特性名称或说明文本推断。
	WeatherEndTurnHealing *battleengine.WeatherEndTurnHealing
	// WeatherSpeedMultipliers 是特性在本场开始时冻结的普通天气行动速度整数分数倍率集合。
	// 它只参与同优先度行动排序，不能用作伤害或天气生命周期规则。
	WeatherSpeedMultipliers []battleengine.WeatherSpeedMultiplier
	// EnvironmentHighestStatMultiplier 是特性在本场开始时冻结的环境最高原始能力强化规则。
	// nil 表示没有该规则；它只保存天气或场地触发条件，具体能力项和倍率完全由 Battle Engine 的封闭规则结算。
	EnvironmentHighestStatMultiplier *battleengine.EnvironmentHighestStatMultiplier
	// SwitchInStrongWeather 是特性在成员进入场地时建立的封闭强天气种类；空值表示没有该规则。
	// 强天气来源和生命周期属于引擎环境，不能由 Battle 通过普通天气或持续回合模拟。
	SwitchInStrongWeather battleengine.StrongWeatherKind
	// SwitchInWeather 是特性在成员进入场地时建立的普通天气规则；nil 表示没有该规则。
	// 它只在实际换入后由引擎结算，不能合并到技能天气或强天气来源中。
	SwitchInWeather *battleengine.SwitchInWeather
	// SwitchInTerrain 是特性在成员进入场地时建立的普通场地规则；nil 表示没有该规则。
	// 它只在实际换入后由引擎结算，不能合并到技能场地、天气或侧状态来源中。
	SwitchInTerrain *battleengine.SwitchInTerrain
	// SwitchInStatStageChange 是特性在成员进入场地时立即执行的能力阶级变化规则；nil 表示没有该规则。
	// 它不复用技能命中能力变化，也不保留资料层 Identifier，必须在 Battle 读取时转换为引擎封闭 Stat。
	SwitchInStatStageChange *battleengine.SwitchInStatStageChange
	// SwitchInAllyHeal 是特性在成员进入场地时为同侧其它上场成员回复生命的规则；nil 表示没有该规则。
	// 它不复用技能、天气或场地回复，且只由引擎在成员实际成功入场后执行。
	SwitchInAllyHeal *battleengine.SwitchInAllyHeal
	// SwitchInOpponentDefenseComparisonBoost 表示特性在成员进入场地时比较对手防御并强化自身。
	// 它只读取引擎已冻结的基础能力，不需要在战斗中查询实时特性资料。
	SwitchInOpponentDefenseComparisonBoost bool
	// SwitchInAllyStatStageCopy 表示特性在成员进入场地时复制同侧其它上场成员的全部能力阶级。
	// 它由引擎按稳定槽位选择来源，Battle 只冻结独立开关而不推断队友或覆盖结果。
	SwitchInAllyStatStageCopy bool
	// SwitchInAllyStatStageReset 表示特性在成员进入场地时将同侧其它上场成员的全部能力阶级重置为零。
	// 它由引擎按当前活跃槽位确定目标，Battle 只冻结独立开关而不合并到复制或技能能力变化规则。
	SwitchInAllyStatStageReset bool
	// SwitchInClearAllSideDamageReductions 表示特性在成员进入场地时清除双方阵营的全部减伤屏障。
	// Battle 只冻结独立开关；屏障范围和事件由引擎按阵营侧状态结算，不能并入入场危害或技能清场资料。
	SwitchInClearAllSideDamageReductions bool
	// SwitchInCopyOpponentAbility 表示特性在成员进入场地时复制一名存活上场对手的当前特性及其冻结规则。
	// Battle 只冻结独立开关；来源选择和逐字段复制由引擎按当前场上快照结算。
	SwitchInCopyOpponentAbility bool
	// SwitchInRevealOpponentHeldItems 表示特性在成员进入场地时公开所有存活上场对手的持有道具。
	// Battle 只冻结独立开关；公开范围与事件顺序由引擎按当前场上快照结算。
	SwitchInRevealOpponentHeldItems bool
	// SwitchInRevealOpponentHighestPowerSkill 表示特性在成员进入场地时公开对手最高基础威力技能。
	// Battle 只冻结独立开关；候选筛选与并列决胜均由引擎基于当前冻结快照结算。
	SwitchInRevealOpponentHighestPowerSkill bool
	// SwitchInTransformIntoOpponent 表示特性在成员进入场地时复制一名存活上场对手的完整战斗画像。
	// Battle 只冻结独立开关；来源选择、PP 限制和离场恢复由引擎基于冻结快照结算。
	SwitchInTransformIntoOpponent bool
	// SwitchInDetectDangerousOpponentSkill 表示特性在成员进入场地时侦测一项危险对手技能。
	SwitchInDetectDangerousOpponentSkill bool
	// SwitchInDisguiseAsLastHealthyAlly 表示特性在成员进入场地时设置同侧队友的视觉伪装。
	SwitchInDisguiseAsLastHealthyAlly bool
	// SwitchInFormChange 是特性在成员进入场地时触发的一次确定形态切换规则。
	// nil 表示没有该规则；目标画像必须已包含于 FormProfiles，不能在实际换入时查询实时资料。
	SwitchInFormChange *battleengine.SwitchInFormChange
	// SwitchOutFormChange 是特性在成员成功离场时触发的一次确定形态切换规则。
	// nil 表示没有该规则；基础和目标形态画像必须已包含于 FormProfiles，不能在实际换出时查询实时资料。
	SwitchOutFormChange *battleengine.SwitchOutFormChange
	// WeatherFormChange 是特性按当前有效普通天气同步成员形态的规则。
	// nil 表示没有该规则；默认形态和每个天气目标画像均必须已包含于 FormProfiles。
	WeatherFormChange *battleengine.WeatherFormChange
	// TerastallizationStatStageChange 是特性在成员完成太晶化后立即执行的能力阶级变化规则。
	// nil 表示没有该规则；它必须由实时特性详情将资料 Identifier 映射为引擎封闭 Stat，不能由运行中读取资料推断。
	TerastallizationStatStageChange *battleengine.TerastallizationStatStageChange
	// TerastallizationEnvironmentClear 表示特性在成员完成太晶化后清除普通天气和普通场地。
	// 强天气不属于清除范围；Battle 只冻结该独立开关，实际环境状态和事件由 Battle Engine 结算。
	TerastallizationEnvironmentClear bool
	// ItemID 是本场开始时冻结的持有道具稳定 Identifier；没有道具时为零值。
	ItemID snowflake.ID
	// HighestStatBoosterAbilityIDs 是当前持有道具允许消耗并激活最高原始能力强化的特性 Identifier 集合。
	// Battle 已校验每个 Identifier 对应启用特性；Battle Engine 仅比较成员当前实际特性，不再访问 Item Metadata。
	HighestStatBoosterAbilityIDs []battleengine.Identifier
	// DamagedForceSelfSwitch 表示当前持有道具会在成员受到对手技能实际伤害后消耗自身并强制成员换下。
	// 它与攻击者换人和能力下降换人保持独立，以便引擎严格按各自触发窗口结算。
	DamagedForceSelfSwitch bool
	// DamagedForceAttackerSwitch 表示当前持有道具会在成员受到对手技能实际伤害后消耗自身并强制攻击者换下。
	// 引擎会在真正强制攻击者换人前检查攻击者自身的特性免疫。
	DamagedForceAttackerSwitch bool
	// NegativeStatStageForceSelfSwitch 表示当前持有道具会在成员实际被降低能力阶级后消耗自身并强制成员换下。
	// 它不对被阶段下限阻止的声明性下降触发，避免错误消耗道具。
	NegativeStatStageForceSelfSwitch bool
	// SwitchRestrictionImmunity 表示当前持有道具提供敌方特性主动换人限制的明确豁免。
	// 它不影响技能、道具造成的强制换人，也不替代束缚、锁招等易变状态的主动换人限制。
	SwitchRestrictionImmunity bool
	// ContactSideEffectImmunity 表示当前持有道具使攻击方免疫目标因有效接触施加的反制副作用。
	// 接触事实本身仍保留，故该字段不能被用于接触保护穿透或接触伤害倍率的判定。
	ContactSideEffectImmunity bool
	// HeldItemContactDamageToAttackerDenominator 表示当前持有道具在成员受到有效接触本体伤害后反伤攻击者的分母。
	// 0 表示当前道具不提供该规则；它与特性来源的接触反伤保持独立，以支持两者按固定顺序分别结算。
	HeldItemContactDamageToAttackerDenominator uint16
	// HeldItemEndTurnHealDenominator 表示当前持有道具在回合末按最大生命回复的固定比例分母。
	// 0 表示没有回复规则；正值由 Battle Engine 至少回复 1 点并按缺失生命封顶，且不读取随机源。
	HeldItemEndTurnHealDenominator uint16
	// HeldItemEndTurnHealForElementID 是当前持有道具要求成员具备的有效属性稳定 Identifier。
	// 空字符串表示没有属性条件回复；Battle Engine 会读取成员当前属性，使太晶化和形态变化即时影响触发条件。
	HeldItemEndTurnHealForElementID snowflake.ID
	// HeldItemEndTurnHealForElementDenominator 表示属性匹配时按最大生命固定比例回复的分母。
	// 0 表示没有属性条件回复；它仅与非空属性 Identifier 一同出现，避免引擎在运行期解释残缺资料。
	HeldItemEndTurnHealForElementDenominator uint16
	// HeldItemEndTurnDamageDenominator 表示当前持有道具在回合末按最大生命造成间接伤害的固定比例分母。
	// 0 表示没有自伤规则；正值由 Battle Engine 至少造成 1 点并按当前生命封顶，且会被间接伤害免疫阻止。
	HeldItemEndTurnDamageDenominator uint16
	// HeldItemEndTurnDamageWithoutElementID 是当前持有道具要求成员不具备的有效属性稳定 Identifier。
	// 空字符串表示没有属性条件自伤；Battle Engine 读取当前属性，所以太晶化和形态变化会即时改变触发条件。
	HeldItemEndTurnDamageWithoutElementID snowflake.ID
	// HeldItemEndTurnDamageWithoutElementDenominator 表示当前属性不包含指定属性时按最大生命固定比例自伤的分母。
	// 0 表示没有属性条件自伤；它仅与非空属性 Identifier 一同出现，避免引擎在运行期解释残缺资料。
	HeldItemEndTurnDamageWithoutElementDenominator uint16
	// HeldItemConsumableElementDamageBoostElementID 是当前持有的一次性威力强化道具要求的技能有效属性稳定 Identifier。
	// 空字符串表示没有该规则；它只参与普通伤害的威力阶段，实际消费仍必须等到成功造成真实本体伤害后发生。
	HeldItemConsumableElementDamageBoostElementID snowflake.ID
	// HeldItemConsumableElementDamageBoostNumerator 是一次性属性威力强化倍率的正分子。
	// 零仅表示规则未声明；它必须与属性 Identifier、正分母成组冻结，不能让引擎猜测不完整资料。
	HeldItemConsumableElementDamageBoostNumerator uint16
	// HeldItemConsumableElementDamageBoostDenominator 是一次性属性威力强化倍率的正分母。
	// 成员受替身保护、技能未命中或属性免疫时，即使该倍率参与了前置判定也绝不会消费道具。
	HeldItemConsumableElementDamageBoostDenominator uint16
	// ContactTransferToAttacker 表示当前持有道具会在成员受到有效接触本体伤害后转移给无道具攻击者。
	// 它只冻结触发资格；实际转移由引擎在本段伤害后原子移动当前道具及其全部运行时投影。
	ContactTransferToAttacker bool
	// ChargeSkipOnce 表示当前持有道具可在首次蓄力技能行动时被消耗，并跳过本次蓄力等待。
	// 天气跳过蓄力仍由技能资料独立控制；两者不共享道具消费或事件语义。
	ChargeSkipOnce bool
	// HeldItemSurviveFatalDamageAtFullHP 表示当前持有道具能在成员满生命承受致命对手技能伤害时保留 1 HP。
	// 它在触发后消费当前道具，且与特性来源保命保持独立的无视特性和来源事件语义。
	HeldItemSurviveFatalDamageAtFullHP bool
	// HeldItemReflectTurnsRemaining 表示当前持有道具建立反射壁时允许的最大初始持续回合；0 表示不延长。
	// Battle 只冻结经过资料校验的正值，实际建立时由引擎与技能资料值取大。
	HeldItemReflectTurnsRemaining uint8
	// HeldItemLightScreenTurnsRemaining 表示当前持有道具建立光墙时允许的最大初始持续回合；0 表示不延长。
	// 它不能替代反射壁或极光幕字段，确保三种屏障的资料适用范围独立。
	HeldItemLightScreenTurnsRemaining uint8
	// HeldItemAuroraVeilTurnsRemaining 表示当前持有道具建立极光幕时允许的最大初始持续回合；0 表示不延长。
	// 道具消失后引擎只依赖当前 ItemID 和该冻结投影判断，绝不回查实时资料。
	HeldItemAuroraVeilTurnsRemaining uint8
	// HeldItemRainTurnsRemaining 表示当前持有道具建立普通降雨时允许的最大初始持续回合；0 表示不延长。
	// 它只适用于降雨，技能和入场特性建立天气时均使用该同一冻结事实。
	HeldItemRainTurnsRemaining uint8
	// HeldItemSandstormTurnsRemaining 表示当前持有道具建立普通沙暴时允许的最大初始持续回合；0 表示不延长。
	// 沙暴与降雨不共享资料投影，确保运行期只能应用道具明确声明的天气效果。
	HeldItemSandstormTurnsRemaining uint8
	// HeldItemSnowTurnsRemaining 表示当前持有道具建立普通降雪时允许的最大初始持续回合；0 表示不延长。
	// 此冻结事实只供降雪建立入口读取，资料变更不会影响已经开始的对战。
	HeldItemSnowTurnsRemaining uint8
	// HeldItemSunTurnsRemaining 表示当前持有道具建立普通日照时允许的最大初始持续回合；0 表示不延长。
	// 它只能在当前持有道具的成员建立日照时生效，不能从实时资料重新计算。
	HeldItemSunTurnsRemaining uint8
	// HeldItemTerrainTurnsRemaining 表示当前持有道具建立任一普通场地时允许的最大初始持续回合；0 表示不延长。
	// 它冻结资料定义的全场地规则，运行期不使用资料名称或文本推断适用范围。
	HeldItemTerrainTurnsRemaining uint8
	// HeldItemSandstormDamageImmunity 表示当前持有道具是否使成员免疫回合末普通沙暴伤害。
	// 它只影响本道具持有者的沙暴扣血，不会投影为通用间接伤害免疫。
	HeldItemSandstormDamageImmunity bool
	// HeldItemEntryHazardImmunity 表示当前持有道具是否使成员免疫自身换入时的四类入场危害。
	// 它不会删除侧状态，且仅在成员当前仍持有该道具时被引擎读取。
	HeldItemEntryHazardImmunity bool
	// HeldItemWeightHalf 表示当前持有道具是否使成员参与体重规则时的有效体重减半。
	// 它冻结为运行期事实，不会改写成员的权威 Weight，也不需要再次查询实时道具资料。
	HeldItemWeightHalf bool
	// HeldItemCuresParalysis 表示当前持有道具是否在成员成功获得麻痹后立即消耗并解除该异常。
	// 它仅对麻痹的实际写入生效，Battle 冻结后不再读取或推断实时道具资料。
	HeldItemCuresParalysis bool
	// HeldItemCuresSleep 表示当前持有道具是否在成员成功获得睡眠后立即消耗并解除该异常。
	// 它会随道具消费清空睡眠回合计数，不能依据后续实时资料改变本场冻结的行为。
	HeldItemCuresSleep bool
	// HeldItemCuresPoison 表示当前持有道具是否在成员成功获得普通中毒或剧毒后立即消耗并解除该异常。
	// 它会随消费清空剧毒计数，Battle 冻结后不读取实时资料决定道具是否触发。
	HeldItemCuresPoison bool
	// HeldItemCuresBurn 表示当前持有道具是否在成员成功获得灼伤后立即消耗并解除该异常。
	// 它只对应灼伤的实际写入，Battle 冻结后不再读取或推断实时道具资料。
	HeldItemCuresBurn bool
	// HeldItemCuresFreeze 表示当前持有道具是否在成员成功获得冰冻后立即消耗并解除该异常。
	// 它只对应冰冻的实际写入，Battle 冻结后不再读取或推断实时道具资料。
	HeldItemCuresFreeze bool
	// HeldItemCuresAllMajorStatuses 表示当前持有道具是否在成员成功获得任一种主要异常后立即消耗并解除该异常。
	// 它冻结完整状态集合，且道具消费时会清除睡眠和剧毒的附属计数，不会回查实时资料。
	HeldItemCuresAllMajorStatuses bool
	// HeldItemCuresConfusion 表示当前持有道具是否在成员成功获得混乱后立即消耗并解除该易变状态。
	// 它只清空混乱持续回合，Battle 冻结后不再读取或推断实时道具资料。
	HeldItemCuresConfusion bool
	// HeldItemPunchBasedSkillPowerBoost 表示当前持有道具是否在普通直接伤害威力阶段强化拳击类技能。
	HeldItemPunchBasedSkillPowerBoost bool
	// HeldItemPhysicalDamagePowerBoost 表示当前持有道具是否固定强化普通物理直接伤害的有效威力。
	HeldItemPhysicalDamagePowerBoost bool
	// HeldItemSpecialDamagePowerBoost 表示当前持有道具是否固定强化普通特殊直接伤害的有效威力。
	HeldItemSpecialDamagePowerBoost bool
	// HeldItemElementDamageReductionElementID 是一次性抗性道具匹配的技能属性稳定 Identifier。
	HeldItemElementDamageReductionElementID snowflake.ID
	// HeldItemElementDamageReductionRequiresSuperEffective 表示抗性道具是否要求技能严格克制目标。
	HeldItemElementDamageReductionRequiresSuperEffective bool
	// HeldItemSuperEffectiveDamageBoost 表示持有道具是否强化效果绝佳的普通直接伤害。
	HeldItemSuperEffectiveDamageBoost bool
	// HeldItemDamageBoostWithRecoil 表示持有道具是否强化普通直接伤害并在造成伤害后反伤。
	HeldItemDamageBoostWithRecoil bool
	// HeldItemDamageDealtHeal 表示道具在成员造成伤害后回复生命。
	HeldItemDamageDealtHeal bool
	// HeldItemDrainHealingBoost 表示道具强化吸取回复。
	HeldItemDrainHealingBoost bool
	// HeldItemAccuracyBoost 表示道具强化普通命中率。
	HeldItemAccuracyBoost bool
	// HeldItemOpponentAccuracyReduction 表示道具降低对手针对持有者的普通命中率。
	HeldItemOpponentAccuracyReduction bool
	// HeldItemCriticalHitStageBoost 表示道具增加一级要害判定。
	HeldItemCriticalHitStageBoost bool
	// HeldItemAirborneUntilDamaged 表示道具让成员在受伤前视为空中。
	HeldItemAirborneUntilDamaged bool
	// HeldItemForceGrounded 表示道具强制成员接地。
	HeldItemForceGrounded bool
	// HeldItemSpeedHalf 表示道具把行动排序速度减半。
	HeldItemSpeedHalf bool
	// HeldItemSpecialDefenseBoost 表示道具强化普通特殊伤害公式中的特防。
	HeldItemSpecialDefenseBoost bool
	// HeldItemStatusSkillRestriction 表示道具禁止选择变化技能。
	HeldItemStatusSkillRestriction bool
	// HeldItemPhysicalDamagePowerBoost50 表示讲究头带强化普通物理技能。
	HeldItemPhysicalDamagePowerBoost50 bool
	// HeldItemSpecialDamagePowerBoost50 表示讲究眼镜强化普通特殊技能。
	HeldItemSpecialDamagePowerBoost50 bool
	// HeldItemChoiceSkillLock 表示道具限制首次宣告后的技能选择。
	HeldItemChoiceSkillLock bool
	// HeldItemSpeedBoost50 表示讲究围巾提高有效速度。
	HeldItemSpeedBoost50 bool
	// HeldItemAccuracyAfterTargetActedBoost 表示目标已行动时强化命中率。
	HeldItemAccuracyAfterTargetActedBoost bool
	// HeldItemTypeImmunitySuppression 表示自身属性提供的伤害免疫失效。
	HeldItemTypeImmunitySuppression bool
	// HeldItemOpponentStatStageReductionImmunity 表示阻止对手降阶。
	HeldItemOpponentStatStageReductionImmunity bool
	// HeldItemNegativeStatStageReset 表示降阶后重置负阶级。
	HeldItemNegativeStatStageReset bool
	// HeldItemAbilityStatReductionSpeedBoost 表示对手入场特性降阶后的速度补偿。
	HeldItemAbilityStatReductionSpeedBoost bool
	// HeldItemOpponentPositiveStatStageCopy 表示复制对手技能产生的正阶级。
	HeldItemOpponentPositiveStatStageCopy bool
	// HeldItemDamagingSkillSecondaryEffectImmunity 表示道具阻止伤害技能的目标侧追加效果。
	HeldItemDamagingSkillSecondaryEffectImmunity bool
	// HeldItemBindingTurns 表示道具建立束缚时覆盖的固定持续次数。
	HeldItemBindingTurns uint8
	// HeldItemBindingDamageDenominator 表示道具建立束缚时冻结的回合末伤害分母。
	HeldItemBindingDamageDenominator uint16
	// HeldItemAccuracyMissStatStageBoostStat 是技能命中落空后提升的能力项。
	HeldItemAccuracyMissStatStageBoostStat battleengine.Stat
	// HeldItemAccuracyMissStatStageBoostDelta 是技能命中落空后应用的正阶级变化量。
	HeldItemAccuracyMissStatStageBoostDelta int8
	// HeldItemWeaknessPolicy 表示严格克制受伤后提升攻击和特攻并消费道具。
	HeldItemWeaknessPolicy bool
	// HeldItemWaterDamageSpecialAttackBoostElementID 是球根类规则匹配的属性稳定 Identifier。
	HeldItemWaterDamageSpecialAttackBoostElementID snowflake.ID
	// HeldItemElectricDamageAttackBoostElementID 是充电电池类规则匹配的属性稳定 Identifier。
	HeldItemElectricDamageAttackBoostElementID snowflake.ID
	// HeldItemWaterDamageSpecialDefenseBoostElementID 是光苔类规则匹配的属性稳定 Identifier。
	HeldItemWaterDamageSpecialDefenseBoostElementID snowflake.ID
	// HeldItemIceDamageAttackBoostElementID 是雪球类规则匹配的属性稳定 Identifier。
	HeldItemIceDamageAttackBoostElementID snowflake.ID
	// HeldItemAdditionalFlinchChancePercent 是伤害技能追加的畏缩概率。
	HeldItemAdditionalFlinchChancePercent uint8
	// HeldItemRandomActionOrderBoostChancePercent 是技能行动随机先行概率。
	HeldItemRandomActionOrderBoostChancePercent uint8
	// HeldItemForcedLastActionOrder 表示技能行动强制后行。
	HeldItemForcedLastActionOrder bool
	// HeldItemLowHPActionOrderBoost 表示低生命时消费道具并先行。
	HeldItemLowHPActionOrderBoost bool
	// HeldItemFieldSpeedOrderSpeedStageDrop 表示戏法空间建立后降速消费。
	HeldItemFieldSpeedOrderSpeedStageDrop bool
	// HeldItemConsecutiveSkillDamageBoost 表示连续宣告同一技能时提高伤害。
	HeldItemConsecutiveSkillDamageBoost bool
	// HeldItemPunchBasedContactSuppression 表示当前持有道具是否令拳击类接触技能失去本次有效接触。
	HeldItemPunchBasedContactSuppression bool
	// HeldItemPowderSkillImmunity 表示当前持有道具是否在命中前阻止粉末或孢子类技能影响成员。
	HeldItemPowderSkillImmunity bool
	// HeldItemMultiHitCountMinimum 是当前持有道具对匹配连续命中技能应用后的实际段数下界；0 表示不覆盖。
	// 它必须和其它三个区间字段一同从资料冻结，不能让 Battle Engine 回查实时道具详情。
	HeldItemMultiHitCountMinimum uint8
	// HeldItemMultiHitCountMaximum 是当前持有道具对匹配连续命中技能应用后的实际段数上界；0 表示不覆盖。
	HeldItemMultiHitCountMaximum uint8
	// HeldItemMultiHitRequiredMinimum 是当前持有道具可以覆盖的技能原始段数下界；0 表示不覆盖。
	HeldItemMultiHitRequiredMinimum uint8
	// HeldItemMultiHitRequiredMaximum 是当前持有道具可以覆盖的技能原始段数上界；0 表示不覆盖。
	HeldItemMultiHitRequiredMaximum uint8
	// HeldItemElementID 是持有道具提供属性伤害强化时冻结的属性稳定 Identifier。
	// 空字符串表示该道具没有此身份；它与特性开关分离，避免把道具资料解释为无类型战斗效果。
	HeldItemElementID snowflake.ID
	// SwitchInHeldItemElementIdentity 表示特性进入场地时按所持道具的属性身份替换自身属性。
	// Battle 只冻结独立开关和道具属性，真正的生命周期、事件与离场恢复由 Battle Engine 结算。
	SwitchInHeldItemElementIdentity bool
}

// BattleSideFacts 是一方 Participant 在实时资料读取后可供编译器消费的完整成员事实集合。
type BattleSideFacts struct {
	// Side 是与 Battle Participant 固定对应的阵营位置。
	Side ParticipantSide
	// Members 包含该方冻结 Team 全部成员的已解析事实，编译器只选择已锁定 Preview 中的成员。
	Members []BattleMemberFacts
}

// InitialStateFacts 是编译一场等待 Runtime 承载的 Battle 所需的赛制、规则和两方已解析成员事实。
//
// 调用者必须在同一实时资料修订下构造所有字段，并在读取前后确认全局维护状态与修订未改变。
// 本结构不包含数据库连接、Runtime 或可变缓存，因而可被确定性单元测试直接构造。
type InitialStateFacts struct {
	// Format 是从当前启用 BattleFormat 解析出的引擎执行赛制。
	Format battleengine.FormatSnapshot
	// Rules 是从当前启用资料解析出的引擎规则快照。
	Rules battleengine.RuleSnapshot
	// Sides 是双方冻结 Team 的完整已解析成员集合。
	Sides []BattleSideFacts
}

// CompileInitialState 根据等待 Runtime 承载的 Battle、双方已锁定 Preview 和实时资料事实构造引擎输入。
//
// Preview 里的成员位置保持原始 Team 位置，而非为了引擎重新编号；因此回合中的换人命令、历史记录
// 和披露账本始终指向同一个稳定位置。生成结果会再由 battleengine.NewState 校验，避免编译器和引擎
// 的不变量随时间漂移。
func CompileInitialState(session Battle, facts InitialStateFacts) (battleengine.InitialState, error) {
	if session.Status != StatusRunning || !session.StartedAt.IsZero() || !validBattleFormat(session.Format) ||
		len(session.Participants) != 2 || len(session.PreviewSubmissions) != 2 || len(facts.Sides) != 2 {
		return battleengine.InitialState{}, ErrInitialStateCompilation
	}
	if facts.Format.TeamSize != uint8(session.Format.SelectCount) ||
		facts.Format.ActiveSlotsPerSide != battleengine.SlotPosition(session.Format.ActiveParticipantsPerSide) {
		return battleengine.InitialState{}, fmt.Errorf("%w: 赛制与预览规则不一致", ErrInitialStateCompilation)
	}

	initial := battleengine.InitialState{
		Format: facts.Format,
		Rules:  facts.Rules,
		Sides:  make([]battleengine.SideSnapshot, 0, 2),
	}
	for _, side := range []ParticipantSide{ParticipantSideOne, ParticipantSideTwo} {
		participant, participantFound := sessionParticipantBySide(session.Participants, side)
		preview, previewFound := previewSubmissionBySide(session.PreviewSubmissions, side)
		factSide, factsFound := battleSideFactsBySide(facts.Sides, side)
		if !participantFound || !previewFound || !factsFound {
			return battleengine.InitialState{}, fmt.Errorf("%w: 阵营 %d 缺少冻结事实", ErrInitialStateCompilation, side)
		}
		compiled, err := compileSide(participant, preview, factSide, facts.Format)
		if err != nil {
			return battleengine.InitialState{}, err
		}
		initial.Sides = append(initial.Sides, compiled)
	}
	if _, err := battleengine.NewState(initial); err != nil {
		return battleengine.InitialState{}, fmt.Errorf("%w: %v", ErrInitialStateCompilation, err)
	}
	return initial, nil
}

func compileSide(
	participant Participant,
	preview PreviewSubmission,
	facts BattleSideFacts,
	format battleengine.FormatSnapshot,
) (battleengine.SideSnapshot, error) {
	teamPositions := make(map[battleengine.MemberPosition]struct{}, len(participant.Team.Members))
	for _, member := range participant.Team.Members {
		teamPositions[battleengine.MemberPosition(member.Position)] = struct{}{}
	}
	factByPosition := make(map[battleengine.MemberPosition]BattleMemberFacts, len(facts.Members))
	for _, fact := range facts.Members {
		if !fact.Position.Valid() {
			return battleengine.SideSnapshot{}, fmt.Errorf("%w: 成员位置无效", ErrInitialStateCompilation)
		}
		if _, duplicate := factByPosition[fact.Position]; duplicate {
			return battleengine.SideSnapshot{}, fmt.Errorf("%w: 成员事实位置重复", ErrInitialStateCompilation)
		}
		factByPosition[fact.Position] = fact
	}

	side := battleengine.SideSnapshot{
		Side:          battleSide(preview.Side),
		ActiveMembers: make([]battleengine.MemberPosition, len(preview.ActivePositions)),
		Members:       make([]battleengine.MemberSnapshot, 0, len(preview.MemberPositions)),
	}
	selected := make(map[battleengine.MemberPosition]struct{}, len(preview.MemberPositions))
	for _, rawPosition := range preview.MemberPositions {
		position := battleengine.MemberPosition(rawPosition)
		if _, exists := teamPositions[position]; !exists {
			return battleengine.SideSnapshot{}, fmt.Errorf("%w: 预览成员不属于冻结 Team", ErrInitialStateCompilation)
		}
		if _, duplicate := selected[position]; duplicate {
			return battleengine.SideSnapshot{}, fmt.Errorf("%w: 预览成员位置重复", ErrInitialStateCompilation)
		}
		fact, found := factByPosition[position]
		if !found {
			return battleengine.SideSnapshot{}, fmt.Errorf("%w: 预览成员缺少实时资料事实", ErrInitialStateCompilation)
		}
		selected[position] = struct{}{}
		side.Members = append(side.Members, memberSnapshotFromFacts(fact))
	}
	if len(side.Members) != int(format.TeamSize) {
		return battleengine.SideSnapshot{}, fmt.Errorf("%w: 预览成员数量不符合赛制", ErrInitialStateCompilation)
	}
	for index, rawPosition := range preview.ActivePositions {
		position := battleengine.MemberPosition(rawPosition)
		if _, exists := selected[position]; !exists {
			return battleengine.SideSnapshot{}, fmt.Errorf("%w: 上场成员未被预览选择", ErrInitialStateCompilation)
		}
		side.ActiveMembers[index] = position
	}
	return side, nil
}

func memberSnapshotFromFacts(facts BattleMemberFacts) battleengine.MemberSnapshot {
	return battleengine.MemberSnapshot{
		Position: facts.Position, CreatureID: facts.CreatureID, NatureID: facts.NatureID, Level: facts.Level,
		GenderCode: facts.GenderCode,
		MaxHP:      facts.MaxHP, CurrentHP: facts.MaxHP, Stats: facts.Stats,
		Weight:     facts.Weight,
		ElementIDs: append([]battleengine.Identifier(nil), facts.ElementIDs...), FormProfiles: cloneBattleFormProfiles(facts.FormProfiles),
		NaturalElementIDs: append([]battleengine.Identifier(nil), facts.ElementIDs...),
		TeraElementID:     facts.TeraElementID,
		Skills:            cloneSkillSnapshots(facts.Skills),
		AbilityID:         facts.AbilityID, WeatherDamageImmunities: append([]battleengine.WeatherKind(nil), facts.WeatherDamageImmunities...),
		WeatherEffectsSuppressed:                             facts.WeatherEffectsSuppressed,
		ReactiveAbilityRules:                                 battleengine.CloneReactiveAbilityRules(facts.ReactiveAbilityRules),
		AccuracyMultiplier:                                   cloneBattleAccuracyMultiplier(facts.AccuracyMultiplier),
		PhysicalSkillAccuracyMultiplier:                      cloneBattleAccuracyMultiplier(facts.PhysicalSkillAccuracyMultiplier),
		BasePowerAtMostDamageBoost:                           cloneBattleBasePowerAtMostDamageBoost(facts.BasePowerAtMostDamageBoost),
		RecoilSkillDamageBoost:                               cloneBattleRecoilSkillDamageBoost(facts.RecoilSkillDamageBoost),
		LowHPElementDamageBoost:                              cloneBattleLowHPElementDamageBoost(facts.LowHPElementDamageBoost),
		WeatherElementDamageBoost:                            cloneBattleWeatherElementDamageBoost(facts.WeatherElementDamageBoost),
		ElementSkillDamageBoost:                              cloneBattleElementSkillDamageBoost(facts.ElementSkillDamageBoost),
		SameElementBonusOverride:                             cloneBattleSameElementBonusOverride(facts.SameElementBonusOverride),
		ContactBasedSkillDamageBoost:                         cloneBattleContactBasedSkillDamageBoost(facts.ContactBasedSkillDamageBoost),
		CriticalHitDamageBoost:                               cloneBattleCriticalHitDamageBoost(facts.CriticalHitDamageBoost),
		SuperEffectiveDamageBoost:                            cloneBattleSuperEffectiveDamageBoost(facts.SuperEffectiveDamageBoost),
		NotVeryEffectiveDamageBoost:                          cloneBattleNotVeryEffectiveDamageBoost(facts.NotVeryEffectiveDamageBoost),
		TargetGenderDamageMultiplier:                         cloneFormulaRule(facts.TargetGenderDamageMultiplier),
		PunchBasedSkillDamageBoost:                           cloneFormulaRule(facts.PunchBasedSkillDamageBoost),
		SlicingBasedSkillDamageBoost:                         cloneFormulaRule(facts.SlicingBasedSkillDamageBoost),
		SoundBasedSkillDamageBoost:                           cloneFormulaRule(facts.SoundBasedSkillDamageBoost),
		PulseBasedSkillDamageBoost:                           cloneFormulaRule(facts.PulseBasedSkillDamageBoost),
		BiteBasedSkillDamageBoost:                            cloneFormulaRule(facts.BiteBasedSkillDamageBoost),
		SecondaryEffectsSuppressedDamageBoost:                cloneFormulaRule(facts.SecondaryEffectsSuppressedDamageBoost),
		SoundBasedSkillDamageReduction:                       cloneFormulaRule(facts.SoundBasedSkillDamageReduction),
		SuperEffectiveDamageReduction:                        cloneFormulaRule(facts.SuperEffectiveDamageReduction),
		FullHPDamageReduction:                                cloneFormulaRule(facts.FullHPDamageReduction),
		DamageClassDamageReduction:                           cloneFormulaDamageClassReduction(facts.DamageClassDamageReduction),
		ElementSkillDamageReduction:                          cloneFormulaElementReduction(facts.ElementSkillDamageReduction),
		ContactBasedSkillDamageReduction:                     cloneFormulaRule(facts.ContactBasedSkillDamageReduction),
		AttackingStatMultiplier:                              cloneFormulaAttackingStat(facts.AttackingStatMultiplier),
		OpponentAttackingStatMultiplier:                      cloneFormulaRule(facts.OpponentAttackingStatMultiplier),
		DefendingStatMultiplier:                              cloneFormulaRule(facts.DefendingStatMultiplier),
		OpponentDefendingStatMultiplier:                      cloneFormulaRule(facts.OpponentDefendingStatMultiplier),
		AllySkillDamageBoost:                                 cloneFormulaAllySkillBoost(facts.AllySkillDamageBoost),
		AllyReceivedDamageReduction:                          cloneFormulaRule(facts.AllyReceivedDamageReduction),
		AllyAbilityGroupCode:                                 facts.AllyAbilityGroupCode,
		AllyAbilityPresenceAttackingStatMultiplier:           cloneFormulaRule(facts.AllyAbilityPresenceAttackingStatMultiplier),
		OpponentAccuracySandstormMultiplier:                  cloneBattleAccuracyMultiplier(facts.OpponentAccuracySandstormMultiplier),
		OpponentAccuracySnowMultiplier:                       cloneBattleAccuracyMultiplier(facts.OpponentAccuracySnowMultiplier),
		OpponentAccuracyConfusionMultiplier:                  cloneBattleAccuracyMultiplier(facts.OpponentAccuracyConfusionMultiplier),
		AccuracyAlwaysHits:                                   facts.AccuracyAlwaysHits,
		StatusSkillAccuracyCap:                               facts.StatusSkillAccuracyCap,
		IgnoreOpponentAccuracyStatStages:                     facts.IgnoreOpponentAccuracyStatStages,
		CriticalHitImmunity:                                  facts.CriticalHitImmunity,
		SkillRecoilDamageImmunity:                            facts.SkillRecoilDamageImmunity,
		IndirectDamageImmunity:                               facts.IndirectDamageImmunity,
		ContactDamageToAttackerDenominator:                   facts.ContactDamageToAttackerDenominator,
		IgnoreOpponentDamageStatStages:                       facts.IgnoreOpponentDamageStatStages,
		IgnoreTargetAbilityEffects:                           facts.IgnoreTargetAbilityEffects,
		SurviveFatalDamageAtFullHP:                           facts.SurviveFatalDamageAtFullHP,
		OpponentStatusSkillImmunity:                          facts.OpponentStatusSkillImmunity,
		NonSuperEffectiveDamageImmunity:                      facts.NonSuperEffectiveDamageImmunity,
		CriticalHitStageBoost:                                facts.CriticalHitStageBoost,
		MultiHitMaximum:                                      facts.MultiHitMaximum,
		DamagingSkillSecondaryEffectImmunity:                 facts.DamagingSkillSecondaryEffectImmunity,
		PriorityMoveImmunityForSideEnabled:                   facts.PriorityMoveImmunityForSideEnabled,
		PriorityMoveImmunityForSideProtectsAllies:            facts.PriorityMoveImmunityForSideProtectsAllies,
		StatusSkillMovesLastAndIgnoresTargetAbility:          facts.StatusSkillMovesLastAndIgnoresTargetAbility,
		ContactSkillProtectionBypass:                         facts.ContactSkillProtectionBypass,
		ContactSkillProtectionBypassDamageMultiplier:         cloneBattleDamageFraction(facts.ContactSkillProtectionBypassDamageMultiplier),
		SkillWeatherOverride:                                 facts.SkillWeatherOverride,
		SkillElementConversion:                               cloneBattleSkillElementConversion(facts.SkillElementConversion),
		ContactSuppression:                                   facts.ContactSuppression,
		ReceivedContactDamageHalved:                          facts.ReceivedContactDamageHalved,
		ReceivedFireDamageDoubled:                            facts.ReceivedFireDamageDoubled,
		ForcedSwitchImmunity:                                 facts.ForcedSwitchImmunity,
		OpponentSwitchRestriction:                            cloneBattleOpponentSwitchRestriction(facts.OpponentSwitchRestriction),
		DamageCrossedHalfHPForceSelfSwitch:                   facts.DamageCrossedHalfHPForceSelfSwitch,
		SwitchOutMajorStatusCure:                             facts.SwitchOutMajorStatusCure,
		SwitchOutHealDenominator:                             facts.SwitchOutHealDenominator,
		WeatherEndTurnHealing:                                cloneBattleWeatherEndTurnHealing(facts.WeatherEndTurnHealing),
		WeatherSpeedMultipliers:                              append([]battleengine.WeatherSpeedMultiplier(nil), facts.WeatherSpeedMultipliers...),
		EnvironmentHighestStatMultiplier:                     cloneBattleEnvironmentHighestStatMultiplier(facts.EnvironmentHighestStatMultiplier),
		SwitchInStrongWeather:                                facts.SwitchInStrongWeather,
		SwitchInWeather:                                      cloneBattleSwitchInWeather(facts.SwitchInWeather),
		SwitchInTerrain:                                      cloneBattleSwitchInTerrain(facts.SwitchInTerrain),
		SwitchInStatStageChange:                              cloneBattleSwitchInStatStageChange(facts.SwitchInStatStageChange),
		SwitchInAllyHeal:                                     cloneBattleSwitchInAllyHeal(facts.SwitchInAllyHeal),
		SwitchInOpponentDefenseComparisonBoost:               facts.SwitchInOpponentDefenseComparisonBoost,
		SwitchInAllyStatStageCopy:                            facts.SwitchInAllyStatStageCopy,
		SwitchInAllyStatStageReset:                           facts.SwitchInAllyStatStageReset,
		SwitchInClearAllSideDamageReductions:                 facts.SwitchInClearAllSideDamageReductions,
		SwitchInCopyOpponentAbility:                          facts.SwitchInCopyOpponentAbility,
		SwitchInRevealOpponentHeldItems:                      facts.SwitchInRevealOpponentHeldItems,
		SwitchInRevealOpponentHighestPowerSkill:              facts.SwitchInRevealOpponentHighestPowerSkill,
		SwitchInTransformIntoOpponent:                        facts.SwitchInTransformIntoOpponent,
		SwitchInDetectDangerousOpponentSkill:                 facts.SwitchInDetectDangerousOpponentSkill,
		SwitchInDisguiseAsLastHealthyAlly:                    facts.SwitchInDisguiseAsLastHealthyAlly,
		SwitchInFormChange:                                   cloneBattleSwitchInFormChange(facts.SwitchInFormChange),
		SwitchOutFormChange:                                  cloneBattleSwitchOutFormChange(facts.SwitchOutFormChange),
		WeatherFormChange:                                    cloneBattleWeatherFormChange(facts.WeatherFormChange),
		TerastallizationStatStageChange:                      cloneBattleTerastallizationStatStageChange(facts.TerastallizationStatStageChange),
		TerastallizationEnvironmentClear:                     facts.TerastallizationEnvironmentClear,
		ItemID:                                               facts.ItemID,
		HighestStatBoosterAbilityIDs:                         append([]battleengine.Identifier(nil), facts.HighestStatBoosterAbilityIDs...),
		DamagedForceSelfSwitch:                               facts.DamagedForceSelfSwitch,
		DamagedForceAttackerSwitch:                           facts.DamagedForceAttackerSwitch,
		NegativeStatStageForceSelfSwitch:                     facts.NegativeStatStageForceSelfSwitch,
		SwitchRestrictionImmunity:                            facts.SwitchRestrictionImmunity,
		ContactSideEffectImmunity:                            facts.ContactSideEffectImmunity,
		HeldItemContactDamageToAttackerDenominator:           facts.HeldItemContactDamageToAttackerDenominator,
		HeldItemEndTurnHealDenominator:                       facts.HeldItemEndTurnHealDenominator,
		HeldItemEndTurnHealForElementID:                      facts.HeldItemEndTurnHealForElementID,
		HeldItemEndTurnHealForElementDenominator:             facts.HeldItemEndTurnHealForElementDenominator,
		HeldItemEndTurnDamageDenominator:                     facts.HeldItemEndTurnDamageDenominator,
		HeldItemEndTurnDamageWithoutElementID:                facts.HeldItemEndTurnDamageWithoutElementID,
		HeldItemEndTurnDamageWithoutElementDenominator:       facts.HeldItemEndTurnDamageWithoutElementDenominator,
		HeldItemConsumableElementDamageBoostElementID:        facts.HeldItemConsumableElementDamageBoostElementID,
		HeldItemConsumableElementDamageBoostNumerator:        facts.HeldItemConsumableElementDamageBoostNumerator,
		HeldItemConsumableElementDamageBoostDenominator:      facts.HeldItemConsumableElementDamageBoostDenominator,
		ContactTransferToAttacker:                            facts.ContactTransferToAttacker,
		ChargeSkipOnce:                                       facts.ChargeSkipOnce,
		HeldItemSurviveFatalDamageAtFullHP:                   facts.HeldItemSurviveFatalDamageAtFullHP,
		HeldItemReflectTurnsRemaining:                        facts.HeldItemReflectTurnsRemaining,
		HeldItemLightScreenTurnsRemaining:                    facts.HeldItemLightScreenTurnsRemaining,
		HeldItemAuroraVeilTurnsRemaining:                     facts.HeldItemAuroraVeilTurnsRemaining,
		HeldItemRainTurnsRemaining:                           facts.HeldItemRainTurnsRemaining,
		HeldItemSandstormTurnsRemaining:                      facts.HeldItemSandstormTurnsRemaining,
		HeldItemSnowTurnsRemaining:                           facts.HeldItemSnowTurnsRemaining,
		HeldItemSunTurnsRemaining:                            facts.HeldItemSunTurnsRemaining,
		HeldItemTerrainTurnsRemaining:                        facts.HeldItemTerrainTurnsRemaining,
		HeldItemSandstormDamageImmunity:                      facts.HeldItemSandstormDamageImmunity,
		HeldItemEntryHazardImmunity:                          facts.HeldItemEntryHazardImmunity,
		HeldItemWeightHalf:                                   facts.HeldItemWeightHalf,
		HeldItemCuresParalysis:                               facts.HeldItemCuresParalysis,
		HeldItemCuresSleep:                                   facts.HeldItemCuresSleep,
		HeldItemCuresPoison:                                  facts.HeldItemCuresPoison,
		HeldItemCuresBurn:                                    facts.HeldItemCuresBurn,
		HeldItemCuresFreeze:                                  facts.HeldItemCuresFreeze,
		HeldItemCuresAllMajorStatuses:                        facts.HeldItemCuresAllMajorStatuses,
		HeldItemCuresConfusion:                               facts.HeldItemCuresConfusion,
		HeldItemPunchBasedSkillPowerBoost:                    facts.HeldItemPunchBasedSkillPowerBoost,
		HeldItemPhysicalDamagePowerBoost:                     facts.HeldItemPhysicalDamagePowerBoost,
		HeldItemSpecialDamagePowerBoost:                      facts.HeldItemSpecialDamagePowerBoost,
		HeldItemElementDamageReductionElementID:              facts.HeldItemElementDamageReductionElementID,
		HeldItemElementDamageReductionRequiresSuperEffective: facts.HeldItemElementDamageReductionRequiresSuperEffective,
		HeldItemSuperEffectiveDamageBoost:                    facts.HeldItemSuperEffectiveDamageBoost,
		HeldItemDamageBoostWithRecoil:                        facts.HeldItemDamageBoostWithRecoil,
		HeldItemDamageDealtHeal:                              facts.HeldItemDamageDealtHeal,
		HeldItemDrainHealingBoost:                            facts.HeldItemDrainHealingBoost,
		HeldItemAccuracyBoost:                                facts.HeldItemAccuracyBoost,
		HeldItemOpponentAccuracyReduction:                    facts.HeldItemOpponentAccuracyReduction,
		HeldItemCriticalHitStageBoost:                        facts.HeldItemCriticalHitStageBoost,
		HeldItemAirborneUntilDamaged:                         facts.HeldItemAirborneUntilDamaged,
		HeldItemForceGrounded:                                facts.HeldItemForceGrounded,
		HeldItemSpeedHalf:                                    facts.HeldItemSpeedHalf,
		HeldItemSpecialDefenseBoost:                          facts.HeldItemSpecialDefenseBoost,
		HeldItemStatusSkillRestriction:                       facts.HeldItemStatusSkillRestriction,
		HeldItemPhysicalDamagePowerBoost50:                   facts.HeldItemPhysicalDamagePowerBoost50,
		HeldItemSpecialDamagePowerBoost50:                    facts.HeldItemSpecialDamagePowerBoost50,
		HeldItemChoiceSkillLock:                              facts.HeldItemChoiceSkillLock,
		HeldItemSpeedBoost50:                                 facts.HeldItemSpeedBoost50,
		HeldItemAccuracyAfterTargetActedBoost:                facts.HeldItemAccuracyAfterTargetActedBoost,
		HeldItemTypeImmunitySuppression:                      facts.HeldItemTypeImmunitySuppression,
		HeldItemOpponentStatStageReductionImmunity:           facts.HeldItemOpponentStatStageReductionImmunity,
		HeldItemNegativeStatStageReset:                       facts.HeldItemNegativeStatStageReset,
		HeldItemAbilityStatReductionSpeedBoost:               facts.HeldItemAbilityStatReductionSpeedBoost,
		HeldItemOpponentPositiveStatStageCopy:                facts.HeldItemOpponentPositiveStatStageCopy,
		HeldItemDamagingSkillSecondaryEffectImmunity:         facts.HeldItemDamagingSkillSecondaryEffectImmunity,
		HeldItemBindingTurns:                                 facts.HeldItemBindingTurns,
		HeldItemBindingDamageDenominator:                     facts.HeldItemBindingDamageDenominator,
		HeldItemAccuracyMissStatStageBoostStat:               facts.HeldItemAccuracyMissStatStageBoostStat,
		HeldItemAccuracyMissStatStageBoostDelta:              facts.HeldItemAccuracyMissStatStageBoostDelta,
		HeldItemWeaknessPolicy:                               facts.HeldItemWeaknessPolicy,
		HeldItemWaterDamageSpecialAttackBoostElementID:       facts.HeldItemWaterDamageSpecialAttackBoostElementID,
		HeldItemElectricDamageAttackBoostElementID:           facts.HeldItemElectricDamageAttackBoostElementID,
		HeldItemWaterDamageSpecialDefenseBoostElementID:      facts.HeldItemWaterDamageSpecialDefenseBoostElementID,
		HeldItemIceDamageAttackBoostElementID:                facts.HeldItemIceDamageAttackBoostElementID,
		HeldItemAdditionalFlinchChancePercent:                facts.HeldItemAdditionalFlinchChancePercent,
		HeldItemRandomActionOrderBoostChancePercent:          facts.HeldItemRandomActionOrderBoostChancePercent,
		HeldItemForcedLastActionOrder:                        facts.HeldItemForcedLastActionOrder,
		HeldItemLowHPActionOrderBoost:                        facts.HeldItemLowHPActionOrderBoost,
		HeldItemFieldSpeedOrderSpeedStageDrop:                facts.HeldItemFieldSpeedOrderSpeedStageDrop,
		HeldItemConsecutiveSkillDamageBoost:                  facts.HeldItemConsecutiveSkillDamageBoost,
		HeldItemPunchBasedContactSuppression:                 facts.HeldItemPunchBasedContactSuppression,
		HeldItemPowderSkillImmunity:                          facts.HeldItemPowderSkillImmunity,
		HeldItemMultiHitCountMinimum:                         facts.HeldItemMultiHitCountMinimum,
		HeldItemMultiHitCountMaximum:                         facts.HeldItemMultiHitCountMaximum,
		HeldItemMultiHitRequiredMinimum:                      facts.HeldItemMultiHitRequiredMinimum,
		HeldItemMultiHitRequiredMaximum:                      facts.HeldItemMultiHitRequiredMaximum,
		HeldItemElementID:                                    facts.HeldItemElementID,
		SwitchInHeldItemElementIdentity:                      facts.SwitchInHeldItemElementIdentity,
	}
}

// cloneBattleOpponentSwitchRestriction 深复制 Battle 已解析的敌方主动换人限制规则。
//
// 该规则当前只含值类型字段，但它作为指针表达规则是否存在。这里保持独立克隆，确保调用方随后修改事实输入时
// 不会影响已经冻结到 Battle Engine 的初始成员快照。
// cloneBattleAccuracyMultiplier 深拷贝冻结命中分数，防止 Battle 编译产物与调用方共享可变指针。
func cloneBattleAccuracyMultiplier(value *battleengine.AccuracyMultiplier) *battleengine.AccuracyMultiplier {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneBattleDamageFraction 深复制 Battle 已解析的独立伤害倍率。
func cloneBattleDamageFraction(value *battleengine.DamageFraction) *battleengine.DamageFraction {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneBattleSkillElementConversion 深复制 Battle 已解析的技能属性转换规则。
func cloneBattleSkillElementConversion(value *battleengine.SkillElementConversion) *battleengine.SkillElementConversion {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

func cloneBattleOpponentSwitchRestriction(value *battleengine.OpponentSwitchRestriction) *battleengine.OpponentSwitchRestriction {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneBattleFormProfiles 深复制 Battle 已解析的全部形态战斗画像。
func cloneBattleFormProfiles(values []battleengine.FormProfile) []battleengine.FormProfile {
	cloned := make([]battleengine.FormProfile, len(values))
	for index, value := range values {
		cloned[index] = value
		cloned[index].ElementIDs = append([]battleengine.Identifier(nil), value.ElementIDs...)
	}
	return cloned
}

// cloneBattleWeatherEndTurnHealing 深复制 Battle 已解析的特性天气回合末回复规则。
func cloneBattleWeatherEndTurnHealing(value *battleengine.WeatherEndTurnHealing) *battleengine.WeatherEndTurnHealing {
	if value == nil {
		return nil
	}
	return &battleengine.WeatherEndTurnHealing{
		Weathers:        append([]battleengine.WeatherKind(nil), value.Weathers...),
		HealDenominator: value.HealDenominator,
	}
}

// cloneBattleEnvironmentHighestStatMultiplier 深复制 Battle 已解析的环境最高原始能力强化规则。
//
// 该规则当前仅含值类型枚举，但仍通过独立克隆函数穿过 Battle 到 Engine 的边界。这样后续为规则增加字段时，
// 不会因直接复用指针而让测试输入、持久化快照或调用方可变对象与权威初始状态共享内存。
func cloneBattleEnvironmentHighestStatMultiplier(value *battleengine.EnvironmentHighestStatMultiplier) *battleengine.EnvironmentHighestStatMultiplier {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneBattleSwitchInWeather 深复制 Battle 已解析的特性入场普通天气规则。
func cloneBattleSwitchInWeather(value *battleengine.SwitchInWeather) *battleengine.SwitchInWeather {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneBattleSwitchInTerrain 深复制 Battle 已解析的特性入场普通场地规则。
func cloneBattleSwitchInTerrain(value *battleengine.SwitchInTerrain) *battleengine.SwitchInTerrain {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneBattleSwitchInStatStageChange 深复制 Battle 已解析的特性入场能力阶级变化规则。
func cloneBattleSwitchInStatStageChange(value *battleengine.SwitchInStatStageChange) *battleengine.SwitchInStatStageChange {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneBattleSwitchInAllyHeal 深复制 Battle 已解析的特性入场同侧回复规则。
func cloneBattleSwitchInAllyHeal(value *battleengine.SwitchInAllyHeal) *battleengine.SwitchInAllyHeal {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneBattleSwitchInFormChange 深复制 Battle 已解析的入场形态切换规则。
func cloneBattleSwitchInFormChange(value *battleengine.SwitchInFormChange) *battleengine.SwitchInFormChange {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneBattleSwitchOutFormChange 深复制 Battle 已解析的成功离场形态切换规则。
func cloneBattleSwitchOutFormChange(value *battleengine.SwitchOutFormChange) *battleengine.SwitchOutFormChange {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneBattleWeatherFormChange 深复制 Battle 已解析的天气形态切换规则。
func cloneBattleWeatherFormChange(value *battleengine.WeatherFormChange) *battleengine.WeatherFormChange {
	if value == nil {
		return nil
	}
	cloned := *value
	cloned.Targets = append([]battleengine.WeatherFormTarget(nil), value.Targets...)
	return &cloned
}

// cloneBattleTerastallizationStatStageChange 深复制 Battle 已解析的太晶化能力阶级变化规则。
func cloneBattleTerastallizationStatStageChange(value *battleengine.TerastallizationStatStageChange) *battleengine.TerastallizationStatStageChange {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

func cloneSkillSnapshots(source []battleengine.SkillSnapshot) []battleengine.SkillSnapshot {
	cloned := append([]battleengine.SkillSnapshot(nil), source...)
	for index := range cloned {
		cloned[index].StatusApplications = append([]battleengine.MajorStatusApplication(nil), cloned[index].StatusApplications...)
		cloned[index].StatStageEffects = append([]battleengine.StatStageEffect(nil), cloned[index].StatStageEffects...)
	}
	return cloned
}

func sessionParticipantBySide(participants []Participant, side ParticipantSide) (Participant, bool) {
	for _, participant := range participants {
		if participant.Side == side {
			return participant, true
		}
	}
	return Participant{}, false
}

func previewSubmissionBySide(submissions []PreviewSubmission, side ParticipantSide) (PreviewSubmission, bool) {
	for _, submission := range submissions {
		if submission.Side == side {
			return submission, true
		}
	}
	return PreviewSubmission{}, false
}

func battleSideFactsBySide(sides []BattleSideFacts, side ParticipantSide) (BattleSideFacts, bool) {
	for _, candidate := range sides {
		if candidate.Side == side {
			return candidate, true
		}
	}
	return BattleSideFacts{}, false
}
