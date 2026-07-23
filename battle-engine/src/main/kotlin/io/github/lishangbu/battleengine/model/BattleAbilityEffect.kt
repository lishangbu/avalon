package io.github.lishangbu.battleengine.model

/**
 * 特性在战斗中的可执行效果。
 *
 * 该 sealed 类型是规则资料 policy code 进入纯引擎后的结构化形态。当前覆盖的高价值 hook 包括：
 * 低体力时强化指定属性伤害、满 HP 承受致命伤害时保留 1 HP、吸收指定属性技能并回复或提阶、阻止对手先制技能影响己方、
 * 天气下速度修正、天气伤害免疫、天气下回合末回复、受到接触类技能后有概率给攻击方附加主要异常状态，稳定状态免疫、环境下速度修正，
 * 间接伤害免疫、技能反作用伤害免疫、击中要害免疫、无视对手伤害公式能力阶级变化、无视对手命中/闪避阶级
 * 变化、攻击时无视目标特性效果、免疫声音类技能、体重修正，以及成员出场时的能力阶级变化、天气设置和场地设置。
 *
 * 后续每新增一种复杂特性，都应该先明确触发阶段、输入状态、不变量和对照测试，再扩展这里或拆分专门处理器。
 */
sealed interface BattleAbilityEffect {
	/**
	 * 按攻击方与目标性别关系修正最终伤害；任一方无性别时保持中性。
	 */
	data class TargetGenderDamageMultiplier(
		val sameGenderMultiplier: Double = 1.25,
		val oppositeGenderMultiplier: Double = 0.75,
	) : BattleAbilityEffect {
		init {
			require(sameGenderMultiplier > 0.0) { "sameGenderMultiplier must be positive" }
			require(oppositeGenderMultiplier > 0.0) { "oppositeGenderMultiplier must be positive" }
		}
	}

	/** 持有者在场时压制全部天气效果，但不移除天气及其持续时间。 */
	data class WeatherEffectSuppression(private val marker: Unit = Unit) : BattleAbilityEffect
	/**
	 * 免疫一组主要异常状态。
	 *
	 * 用于表达免疫中毒、免疫灼伤、免疫睡眠等稳定特性。具体特性名不进入引擎，避免把本地化文本或资料库名称
	 * 混进规则状态机；资料层负责把特性翻译成这类结构化效果。
	 */
	data class MajorStatusImmunity(
		val statuses: Set<BattleMajorStatus>,
		val requiredWeather: BattleWeather? = null,
	) : BattleAbilityEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
			require(requiredWeather != BattleWeather.NONE) { "requiredWeather cannot be NONE" }
		}
	}

	/** 始终被视为睡眠，但不会因睡眠而无法行动，且免疫其它主要异常状态。 */
	data class AlwaysTreatedAsleep(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 允许使用者绕过属性带来的中毒和剧毒免疫。 */
	data class PoisonElementStatusBypass(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 免疫对手使用的变化技能。 */
	data class OpponentStatusSkillImmunity(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 把面向持有者的对手单体变化技能反射给原使用者。 */
	data class OpponentStatusSkillReflection(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 场上其他成员使用舞蹈类技能后，立即复制该技能。 */
	data class DanceMoveCopy(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 根据技能类别在防御形态与攻击形态之间切换。 */
	data class StanceChange(
		val defensiveFormCode: String,
		val offensiveFormCode: String,
	) : BattleAbilityEffect {
		init {
			require(defensiveFormCode.isNotBlank()) { "defensiveFormCode must not be blank" }
			require(offensiveFormCode.isNotBlank()) { "offensiveFormCode must not be blank" }
		}
	}

	/** 每个完整回合结束时在两个形态之间交替切换。 */
	data class EndTurnFormToggle(
		val firstFormCode: String,
		val secondFormCode: String,
	) : BattleAbilityEffect {
		init {
			require(firstFormCode.isNotBlank()) { "firstFormCode must not be blank" }
			require(secondFormCode.isNotBlank()) { "secondFormCode must not be blank" }
			require(firstFormCode != secondFormCode) { "form codes must differ" }
		}
	}

	/** 回合结束时按 HP 阈值、等级和形态组切换当前形态。 */
	data class EndTurnHpFormChange(
		val formPairs: List<BattleFormPair>,
		val thresholdNumerator: Int,
		val thresholdDenominator: Int,
		val alternateAtOrBelowThreshold: Boolean,
		val minimumLevel: Int = 1,
		val revertsWhenConditionNotMet: Boolean = true,
		val addsMaximumHpDifference: Boolean = false,
		val majorStatusImmuneFormCodes: Set<String> = emptySet(),
	) : BattleAbilityEffect {
		init {
			require(formPairs.isNotEmpty()) { "formPairs must not be empty" }
			require(thresholdNumerator > 0) { "thresholdNumerator must be positive" }
			require(thresholdDenominator >= thresholdNumerator) { "thresholdDenominator must cover numerator" }
			require(minimumLevel in 1..100) { "minimumLevel must be between 1 and 100" }
			require(majorStatusImmuneFormCodes.all { it.isNotBlank() }) { "immune form codes must not be blank" }
		}
	}

	/** 使对手以持有者为目标使用技能时额外消耗 PP。 */
	data class OpponentSkillPpCostIncrease(val additionalCost: Int) : BattleAbilityEffect {
		init {
			require(additionalCost > 0) { "additionalCost must be positive" }
		}
	}

	/** 限制满足条件的对手主动替换。 */
	data class OpponentSwitchRestriction(
		val requiredTargetElementId: Long? = null,
		val requiresGroundedTarget: Boolean = false,
		val sameEffectGrantsImmunity: Boolean = false,
	) : BattleAbilityEffect {
		init {
			require(requiredTargetElementId == null || requiredTargetElementId > 0) {
				"requiredTargetElementId must be positive"
			}
		}
	}

	/** 免疫由对手技能或对手道具造成的强制替换。 */
	data class ForcedSwitchImmunity(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 本次上场期间失去携带道具后提供速度倍率。 */
	data class ItemLostSpeedMultiplier(val multiplier: Double) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 指定攻击属性可绕过目标属性提供的 0 倍免疫。 */
	data class ElementTypeImmunityBypass(val elementIds: Set<Long>) : BattleAbilityEffect {
		init {
			require(elementIds.isNotEmpty()) { "elementIds must not be empty" }
			require(elementIds.all { it > 0 }) { "elementIds must be positive" }
		}
	}

	/** 免疫属性克制倍率小于等于一的伤害技能。 */
	data class NonSuperEffectiveDamageImmunity(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 持有者被本次伤害击倒后，对攻击者造成间接伤害。 */
	data class FaintAttackerDamage(
		val requiresContact: Boolean = false,
		val attackerMaxHpDenominator: Int? = null,
		val usesDamageTaken: Boolean = false,
		val suppressedByExplosionSuppression: Boolean = false,
	) : BattleAbilityEffect {
		init {
			require((attackerMaxHpDenominator != null) xor usesDamageTaken) { "exactly one damage source is required" }
			require(attackerMaxHpDenominator == null || attackerMaxHpDenominator > 0) {
				"attackerMaxHpDenominator must be positive"
			}
		}
	}

	/** 压制爆炸类技能与明确标记为爆炸类的倒下反伤。 */
	data class ExplosionEffectSuppression(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 受伤后首次跨过体力阈值时强制自身替换。 */
	data class DamageCrossedHpThresholdForceSelfSwitch(
		val thresholdNumerator: Int = 1,
		val thresholdDenominator: Int = 2,
	) : BattleAbilityEffect {
		init {
			require(thresholdNumerator > 0 && thresholdDenominator > 0) { "threshold must be positive" }
			require(thresholdNumerator <= thresholdDenominator) { "threshold cannot exceed full hp" }
		}
	}

	/** 受到伤害后改变场上除持有者外所有成员的能力阶级。 */
	data class ReceivedDamageAllOtherStatStageChange(
		val stat: BattleStat,
		val stageDelta: Int,
	) : BattleAbilityEffect {
		init {
			require(stageDelta != 0) { "stageDelta must not be zero" }
		}
	}

	/** 受到伤害后按概率禁用攻击者本次使用的技能。 */
	data class ReceivedDamageDisableAttackerSkill(
		val chancePercent: Int,
		val turns: Int,
	) : BattleAbilityEffect {
		init {
			require(chancePercent in 1..100) { "chancePercent must be between one and one hundred" }
			require(turns > 0) { "turns must be positive" }
		}
	}

	/** 因畏缩无法行动后提升指定能力。 */
	data class FlinchStatStageBoost(
		val stat: BattleStat,
		val stageDelta: Int,
	) : BattleAbilityEffect {
		init {
			require(stageDelta > 0) { "stageDelta must be positive" }
		}
	}

	/** 缩短自身被附加睡眠时的持续回合。 */
	data class SleepDurationDivisor(val divisor: Int) : BattleAbilityEffect {
		init {
			require(divisor > 1) { "divisor must be greater than one" }
		}
	}

	/** 为持有者所在一侧的上场成员提供主要异常状态免疫。 */
	data class SideMajorStatusImmunity(val statuses: Set<BattleMajorStatus>) : BattleAbilityEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
		}
	}

	/** 目标处于指定主要异常状态时必定击中要害。 */
	data class MajorStatusGuaranteedCriticalHit(val statuses: Set<BattleMajorStatus>) : BattleAbilityEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
		}
	}

	/** 造成直接伤害后按概率给目标附加主要异常状态。 */
	data class DealtDamageMajorStatusChance(
		val status: BattleMajorStatus,
		val chancePercent: Int,
		val requiresContact: Boolean = false,
	) : BattleAbilityEffect {
		init {
			require(chancePercent in 1..100) { "chancePercent must be between one and one hundred" }
		}
	}

	/**
	 * 免疫一组临时状态。
	 *
	 * 用于表达混乱免疫、畏缩免疫等稳定特性。不同临时状态的生命周期仍由状态机决定：畏缩只持续到本回合
	 * 行动前或回合末，混乱使用独立持续计数；该效果只负责在附加前阻止状态写入。
	 */
	data class VolatileStatusImmunity(
		val statuses: Set<BattleVolatileStatus>,
	) : BattleAbilityEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
		}
	}

	/** 阻止对手降低指定能力阶级。 */
	data class OpponentStatStageReductionImmunity(
		val stats: Set<BattleStat>,
	) : BattleAbilityEffect {
		init {
			require(stats.isNotEmpty()) { "stats must not be empty" }
		}
	}

	/** 将所有能力阶级增减按固定整数倍率转换。 */
	data class StatStageDeltaMultiplier(val multiplier: Int) : BattleAbilityEffect {
		init {
			require(multiplier != 0) { "multiplier must not be zero" }
		}
	}

	/** 被对手实际降低任意能力后提升指定能力。 */
	data class OpponentStatReductionReactiveBoost(
		val stat: BattleStat,
		val stageDelta: Int,
	) : BattleAbilityEffect {
		init {
			require(stageDelta > 0) { "stageDelta must be positive" }
		}
	}

	/**
	 * 指定天气下的速度倍率。
	 *
	 * 用于表达雨天下速度提升、晴天下速度提升、沙暴/雪景下速度提升等稳定特性。具体特性名称不进入公式；
	 * 引擎只读取天气和倍率，保证同一快照可复盘。
	 */
	data class WeatherSpeedMultiplier(
		val weather: BattleWeather,
		val multiplier: Double,
	) : BattleAbilityEffect {
		init {
			require(weather != BattleWeather.NONE) { "weather speed multiplier requires an active weather" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 主要异常状态存在时的速度倍率。 */
	data class MajorStatusSpeedMultiplier(
		val multiplier: Double,
		val ignoresParalysisReduction: Boolean = false,
	) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 使用技能时的命中倍率。 */
	data class AccuracyMultiplier(
		val multiplier: Double,
		val damageClasses: Set<BattleDamageClass> = emptySet(),
	) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
			require(BattleDamageClass.STATUS !in damageClasses) { "damageClasses cannot contain status" }
		}
	}

	/** 对手以自己为目标时的命中倍率。 */
	data class OpponentAccuracyMultiplier(
		val multiplier: Double,
		val requiredWeather: BattleWeather? = null,
		val requiresConfusion: Boolean = false,
	) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
			require(requiredWeather != BattleWeather.NONE) { "requiredWeather cannot be NONE" }
		}
	}

	/** 自己使用或以自己为目标的技能跳过普通命中判定。 */
	data class AlwaysHit(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 限制对手变化技能以自己为目标时的最终命中率上限。 */
	data class StatusSkillAccuracyCap(val maximumAccuracy: Int) : BattleAbilityEffect {
		init {
			require(maximumAccuracy in 1..100) { "maximumAccuracy must be between one and one hundred" }
		}
	}

	/** 免疫粉末类技能。 */
	data class PowderSkillImmunity(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 免疫伤害技能的追加效果。 */
	data class DamagingSkillSecondaryEffectImmunity(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 使用者发动的技能不再构成接触。 */
	data class ContactSuppression(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 固定提升击中要害等级。 */
	data class CriticalHitStageBoost(val stageDelta: Int) : BattleAbilityEffect {
		init {
			require(stageDelta > 0) { "stageDelta must be positive" }
		}
	}

	/** 可变连击技能固定取最大命中次数。 */
	data class MultiHitMaximum(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 基础威力不超过上限时的最终伤害倍率。 */
	data class BasePowerAtMostDamageBoost(
		val maximumPower: Int,
		val multiplier: Double,
	) : BattleAbilityEffect {
		init {
			require(maximumPower > 0) { "maximumPower must be positive" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 带有按实际伤害反作用力的技能伤害倍率。 */
	data class RecoilSkillDamageBoost(val multiplier: Double) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 主动离场时治愈主要异常状态。 */
	data class SwitchOutMajorStatusCure(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 主动离场时按最大体力固定比例回复。 */
	data class SwitchOutHeal(val healDenominator: Int) : BattleAbilityEffect {
		init {
			require(healDenominator > 0) { "healDenominator must be positive" }
		}
	}

	/**
	 * 指定场地下的速度倍率。
	 *
	 * 用于表达电气场地下速度提升等稳定特性。该效果只参与行动排序、替换排序和其它读取有效速度的流程；
	 * 场地本身的持续时间、伤害修正或状态免疫仍由环境状态和其它规则处理。
	 */
	data class TerrainSpeedMultiplier(
		val terrain: BattleTerrain,
		val multiplier: Double,
	) : BattleAbilityEffect {
		init {
			require(terrain != BattleTerrain.NONE) { "terrain speed multiplier requires an active terrain" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 修正成员在体重相关规则中被读取到的当前体重。
	 *
	 * 该效果用于表达现代规则中“拥有者体重按固定比例变重或变轻”的稳定特性，例如体重翻倍或减半。它不直接修改
	 * [BattleParticipant.weight]，因为基础资料体重仍然需要保持可复盘；伤害公式在结算低踢、打草结、重磅冲撞、
	 * 高温重压这类动态威力时会读取所有体重倍率并形成有效体重。
	 *
	 * 使用分子/分母而不是 Double，是为了保留 69 这类一位小数资料在减半后的 34.5 精度，避免整数除法把阈值边界
	 * 算错。该效果只描述稳定倍率，不处理技能造成的临时体重变化；临时变化需要有自己的状态字段和持续时间规则。
	 */
	data class WeightMultiplier(
		val numerator: Int,
		val denominator: Int,
	) : BattleAbilityEffect {
		init {
			require(numerator > 0) { "numerator must be positive" }
			require(denominator > 0) { "denominator must be positive" }
		}
	}

	/**
	 * 免疫一组天气造成的回合末伤害。
	 */
	data class WeatherDamageImmunity(
		val weathers: Set<BattleWeather>,
	) : BattleAbilityEffect {
		init {
			require(weathers.isNotEmpty()) { "weathers must not be empty" }
			require(BattleWeather.NONE !in weathers) { "weather damage immunity cannot target NONE" }
		}
	}

	/**
	 * 指定天气下的回合末回复。
	 *
	 * 该效果用于表达雨天、雪天等环境中按最大 HP 固定比例回复的特性。它只描述回复条件和比例，不处理天气本身
	 * 的持续回合、天气伤害或道具回复；状态机在回合末天气阶段读取它并产生专用天气回复事件。
	 */
	data class WeatherEndTurnHeal(
		val weathers: Set<BattleWeather>,
		val healDenominator: Int,
	) : BattleAbilityEffect {
		init {
			require(weathers.isNotEmpty()) { "weathers must not be empty" }
			require(BattleWeather.NONE !in weathers) { "weather healing cannot target NONE" }
			require(healDenominator > 0) { "healDenominator must be positive" }
		}
	}

	/**
	 * 免疫非技能直接伤害。
	 *
	 * 该效果用于表达现代规则中“只会被直接攻击伤害扣减 HP”的稳定特性。引擎把普通物理/特殊技能命中后写入目标
	 * 的 [io.github.lishangbu.battleengine.model.BattleEvent.DamageApplied] 视为直接伤害；异常状态回合末伤害、
	 * 天气伤害、入场陷阱、技能反作用伤害、携带道具反伤和混乱自伤等都属于间接伤害，命中该效果时不改变 HP、
	 * 不触发低体力回复道具，也不追加对应伤害事件。
	 *
	 * 该效果只描述伤害免疫，不阻止主要异常状态或临时状态本身写入，也不移除已存在的天气、陷阱或携带道具倍率。
	 * 例如造成伤害后附带反伤的道具仍然提供伤害倍率，但它产生的反伤会被这里阻止。
	 */
	data class IndirectDamageImmunity(private val marker: Unit = Unit) : BattleAbilityEffect

	/**
	 * 免疫技能自身造成的反作用伤害。
	 *
	 * 该效果只阻止 [BattleSkillHpEffect.RecoilByDamageDealt] 这类“技能命中并造成实际伤害后，使用者按伤害
	 * 比例自损”的反作用伤害，不阻止携带道具反伤、混乱自伤、入场陷阱、异常状态、天气或其它间接伤害。
	 * 因此它比 [IndirectDamageImmunity] 范围更窄：拥有者仍会正常受到伤害增幅道具造成的最大 HP 固定反伤。
	 *
	 * 公开现代规则中挣扎等特殊技能的自损不被这类特性阻止；引擎通过
	 * [BattleSkillHpEffect.RecoilByUserMaxHp] 表达这类“按使用者最大 HP 支付代价”的来源，因此这里不需要判断技能
	 * 名称，也不会把该代价误当作普通反作用伤害阻止。
	 */
	data class SkillRecoilDamageImmunity(private val marker: Unit = Unit) : BattleAbilityEffect

	/**
	 * 免疫被技能击中要害。
	 *
	 * 该效果用于表达现代规则中“对手技能无法对拥有者造成击中要害”的稳定特性。它不改变技能自身的
	 * `criticalHitStage`，也不改变要害随机数的消费规则；状态机先按技能等级结算是否原本会击中要害，再由
	 * 目标方的该效果把本次伤害请求中的 `criticalHit` 置为 false。这样普通随机要害仍保留可复盘的随机轨迹，
	 * 必定要害技能也会被明确降回非要害伤害。
	 *
	 * 它只影响直接技能伤害是否按要害倍率计算，不阻止伤害本身、附加状态、能力阶级变化、天气、场地或其它
	 * 伤害来源。若后续接入能绕过目标特性的规则，应在特性抑制层统一处理，而不是在这里判断具体技能名称。
	 */
	data class CriticalHitImmunity(private val marker: Unit = Unit) : BattleAbilityEffect

	/**
	 * 无视对手在伤害公式中使用的能力阶级变化。
	 *
	 * 该效果用于表达现代规则中“结算双方直接伤害时，不读取对手强化或削弱过的相关攻防阶级”的稳定特性：
	 * - 持有效果的一方作为攻击方时，忽略目标的防御或特防阶级，按目标原始防御侧数值进入公式。
	 * - 持有效果的一方作为防守方时，忽略使用者的攻击或特攻阶级，按使用者原始攻击侧数值进入公式。
	 *
	 * 它不清除双方状态，也不修改战斗快照里的阶级；只影响本次普通物理/特殊伤害计算。击中要害、灼伤物理减半、
	 * 天气防御修正、场地修正、道具倍率和特性倍率仍按各自规则继续结算。命中与闪避阶级由
	 * [IgnoreOpponentAccuracyStatStages] 在状态机命中流程中处理，避免伤害公式承担命中语义。
	 */
	data class IgnoreOpponentDamageStatStages(private val marker: Unit = Unit) : BattleAbilityEffect

	/**
	 * 无视对手在命中判定中使用的命中或闪避阶级变化。
	 *
	 * 该效果用于表达现代规则中“结算技能命中率时，不读取对手改变过的命中相关阶级”的稳定特性：
	 * - 持有效果的一方作为攻击方时，忽略目标的闪避阶级，按目标 0 闪避阶级计算有效命中。
	 * - 持有效果的一方作为防守方时，忽略使用者的命中阶级，按使用者 0 命中阶级计算有效命中。
	 *
	 * 它只影响状态机中的命中随机判定，不改变双方快照里的真实阶级，也不影响必中技能、天气命中覆盖、保护、
	 * 属性免疫或普通伤害公式。伤害公式中的攻击/防御/特攻/特防阶级由 [IgnoreOpponentDamageStatStages]
	 * 独立处理，便于测试用例精确定位失败发生在哪个结算阶段。
	 */
	data class IgnoreOpponentAccuracyStatStages(private val marker: Unit = Unit) : BattleAbilityEffect

	/**
	 * 使用技能时无视目标侧防守特性效果。
	 *
	 * 该效果用于表达现代规则中“本成员使用技能时，目标的部分特性不会影响本次技能”的稳定特性。它不删除目标
	 * 快照上的特性，也不让特性在回合末、入场、天气、道具或目标自己行动时失效；只在本次技能结算链路里跳过
	 * 目标侧防守型特性检查，例如属性吸收、满 HP 致命伤害保留、击中要害免疫、主要/临时状态免疫、先制技能侧
	 * 防护和目标作为防守方时无视攻击者命中或伤害阶级变化。
	 *
	 * 判断范围刻意限定为“攻击方对对手目标使用技能”。同侧辅助、自身目标、使用者自己的攻击侧特性和目标携带
	 * 道具都不受该效果影响，避免把特性抑制误扩散到道具、场地、属性天然免疫或非技能伤害来源。
	 */
	data class IgnoreTargetAbilityEffects(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 变化技能在同优先度内最后行动，并在执行期间忽略对手的防守特性。 */
	data class StatusSkillMovesLastAndIgnoresTargetAbility(private val marker: Unit = Unit) : BattleAbilityEffect

	/**
	 * 接触类技能无视目标保护类阻挡。
	 *
	 * 该效果用于表达现代规则中“拥有者使用会接触对手的技能时，可以绕过对手的防住类屏障”。它只改变命中前保护
	 * gate 的阻挡判断，不把技能改成 [BattleSkillSlot.affectedByProtect] 为 false：技能仍然保留自己的资料标签，
	 * 也仍然会被拳击手套这类动态非接触规则影响。
	 *
	 * 与佯攻类 [BattleSkillSlot.breaksProtection] 不同，本效果不会移除目标已经建立的个人保护，也不会移除广域防守
	 * 或快速防守这类本回合临时侧防护。目标的连续保护计数、同回合后续对其它技能的阻挡能力都应继续保留。它也不
	 * 绕过极巨防壁等未来可能加入的更高优先级防护；那类防护应在保护 gate 中用独立模型表达。
	 */
	data class ContactSkillProtectionBypass(private val marker: Unit = Unit) : BattleAbilityEffect

	/**
	 * 免疫其它成员使用的声音类技能。
	 *
	 * 该效果用于表达现代规则中“拥有者不会受到声音类技能影响”的稳定特性。它只读取技能槽上的
	 * `soundBased` 结构化标签，不判断技能名称；伤害技能和变化技能都会在命中与附加效果前被阻止。
	 *
	 * 拥有者自己使用的声音类技能不会被该效果阻止；对手若拥有 [IgnoreTargetAbilityEffects]，则可以在本次技能中
	 * 绕过目标的该免疫。替身是否被声音类技能穿透仍由替身规则单独处理，这里只表达目标特性的免疫能力。
	 */
	data class SoundBasedSkillImmunity(private val marker: Unit = Unit) : BattleAbilityEffect

	/**
	 * 满 HP 承受会导致倒下的直接伤害时保留 1 HP。
	 *
	 * 该效果只处理普通伤害结算中的“从满 HP 被一击打倒”场景，不阻止异常状态、天气、入场陷阱、混乱自伤、
	 * 反作用伤害或其它非技能直接伤害。破格、特性失效等绕过条件会在对应规则模型存在后接入，不在这里判断具体
	 * 特性名称。
	 */
	data class SurviveFatalDamageAtFullHp(
		val remainingHp: Int = 1,
	) : BattleAbilityEffect {
		init {
			require(remainingHp > 0) { "remainingHp must be positive" }
		}
	}

	/**
	 * 阻止对手先制技能影响拥有者所在一侧。
	 *
	 * 现代规则中，这类特性保护拥有者和同侧伙伴，不阻止同侧成员主动使用先制技能，也不阻止没有目标到己方成员的
	 * 场地、撒场或自我目标技能。`protectsAllies` 保留为结构化字段，便于后续接入只保护自身的变体时不改变事件
	 * 和状态机入口。
	 */
	data class PriorityMoveImmunityForSide(
		val protectsAllies: Boolean = true,
	) : BattleAbilityEffect

	/**
	 * 提升变化类技能的行动优先度。
	 *
	 * 该效果只作用于 [BattleDamageClass.STATUS] 技能，不改变物理或特殊技能的基础优先度。现代规则中，由这类
	 * 特性提升优先度的对手变化技能无法影响恶属性目标；`darkElementTargetsImmune` 明确保存这一副作用，避免
	 * 状态机靠具体特性名判断。该免疫只针对对手目标，同侧辅助技能仍正常结算。
	 */
	data class StatusSkillPriorityBoost(
		val priorityDelta: Int = 1,
		val darkElementTargetsImmune: Boolean = true,
	) : BattleAbilityEffect {
		init {
			require(priorityDelta > 0) { "priorityDelta must be positive" }
		}
	}

	/**
	 * 吸收指定属性技能并按最大 HP 回复。
	 *
	 * 该效果用于表达现代规则中“被指定属性技能命中时无效并回复 HP”的稳定特性。它在命中判定成功后、
	 * 普通伤害或附加效果写入前触发；即使目标已经满 HP，也仍会阻止该技能继续结算，只是回复量会被夹取为 0。
	 * `healDenominator` 表示回复最大 HP 的分母，例如 4 表示最多回复最大 HP 的 1/4。
	 */
	data class ElementSkillAbsorbHeal(
		val elementId: Long,
		val healDenominator: Int = 4,
	) : BattleAbilityEffect {
		init {
			require(elementId > 0) { "elementId must be positive" }
			require(healDenominator > 0) { "healDenominator must be positive" }
		}
	}

	/**
	 * 吸收指定属性技能并提升自身能力阶级。
	 *
	 * 该效果用于表达现代规则中“被指定属性技能命中时无效，并提升自身某项能力”的稳定特性。触发位置与
	 * [ElementSkillAbsorbHeal] 一致：命中判定成功后、普通伤害或附加效果写入前。能力阶级仍受 -6..6 边界夹取；
	 * 如果已经到达上限，技能仍被吸收，只是不产生 [BattleEvent.StatStageChanged] 事件。
	 */
	data class ElementSkillAbsorbStatStage(
		val elementId: Long,
		val stat: BattleStat,
		val stageDelta: Int,
	) : BattleAbilityEffect {
		init {
			require(elementId > 0) { "elementId must be positive" }
			require(stageDelta in -6..6 && stageDelta != 0) { "stageDelta must be between -6 and 6 and not zero" }
		}
	}

	/**
	 * 低体力时强化指定属性技能的伤害倍率。
	 *
	 * 该效果用于表达现代规则中“当前 HP 小于等于最大 HP 的 1/3 时，指定属性技能伤害提高 50%”这类稳定特性。
	 * 它只影响使用者主动造成的普通物理/特殊技能伤害；是否匹配属性、当前 HP 是否达到阈值以及倍率叠乘都由
	 * 伤害计算阶段读取，状态机本身不关心具体特性名称。
	 */
	data class LowHpElementDamageBoost(
		val elementId: Long,
		val hpThresholdNumerator: Int = 1,
		val hpThresholdDenominator: Int = 3,
		val multiplier: Double = 1.5,
	) : BattleAbilityEffect {
		init {
			require(elementId > 0) { "elementId must be positive" }
			require(hpThresholdNumerator > 0) { "hpThresholdNumerator must be positive" }
			require(hpThresholdDenominator > 0) { "hpThresholdDenominator must be positive" }
			require(hpThresholdNumerator <= hpThresholdDenominator) { "hp threshold numerator must not exceed denominator" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 指定天气下强化一组属性技能的伤害倍率。
	 *
	 * 该效果用于表达现代规则中“当前天气匹配，且技能当前有效属性属于指定集合时，技能威力按固定倍率提升”的
	 * 稳定特性。引擎读取的是 [BattleSkillSlot.effectiveElementId] 的结果，因此天气球类技能在天气下改变属性后，
	 * 会和普通属性技能走同一套判断口径。
	 *
	 * `elementIds` 保存资料层解析后的属性 ID 集合，不保存属性名称或本地化文本。该效果只属于攻击方主动造成的
	 * 普通物理/特殊技能直接伤害，不改变天气持续时间、不提供天气伤害免疫，也不影响命中、保护、替身或入场陷阱。
	 * 如果某个特性同时拥有天气伤害免疫，资料层应额外提供 [WeatherDamageImmunity]，让回合末天气阶段单独读取。
	 */
	data class WeatherElementDamageBoost(
		val weather: BattleWeather,
		val elementIds: Set<Long>,
		val multiplier: Double = 1.3,
	) : BattleAbilityEffect {
		init {
			require(weather != BattleWeather.NONE) { "weather element damage boost requires an active weather" }
			require(elementIds.isNotEmpty()) { "elementIds must not be empty" }
			require(elementIds.all { it > 0 }) { "elementIds must be positive" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 强化一组指定属性技能的伤害倍率。
	 *
	 * 该效果用于表达现代规则中“使用某个属性的物理/特殊技能时，拥有者的直接技能伤害按固定倍率提升”的稳定特性。
	 * 引擎读取 [BattleSkillSlot.effectiveElementId]，因此天气、场地或其它规则已经改写本次有效属性后，这里会按
	 * 改写后的属性判断；它不会回看技能的原始属性，也不会读取技能名称、本地化文本或特性名称。
	 *
	 * `elementIds` 由资料层解析成资料库属性 ID，可以包含一个或多个属性。该效果只属于攻击方主动造成的普通技能
	 * 直接伤害，不影响固定伤害、间接伤害、反作用伤害、天气伤害、入场陷阱、变化技能或同伴光环类全场规则。
	 * 如果未来接入会动态改变技能有效属性的特性，应先在技能上下文中形成最终属性，再让该效果按同一口径读取。
	 */
	data class ElementSkillDamageBoost(
		val elementIds: Set<Long>,
		val multiplier: Double,
	) : BattleAbilityEffect {
		init {
			require(elementIds.isNotEmpty()) { "elementIds must not be empty" }
			require(elementIds.all { it > 0 }) { "elementIds must be positive" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 覆盖属性一致加成的倍率。
	 *
	 * 该效果用于表达现代规则中“技能当前有效属性与使用者属性一致时，属性一致加成使用不同倍率”的稳定特性。
	 * 它只改变普通伤害公式中的属性一致加成位置，不属于最终伤害倍率，也不改变技能有效属性、属性相性、天气、
	 * 场地、道具或击中要害等其它倍率。
	 *
	 * 引擎读取 [BattleSkillSlot.effectiveElementId] 的结果，因此天气、场地或其它规则已经改写本次有效属性后，
	 * 这里会按统一口径判断是否与使用者属性集合匹配。若技能没有属性一致关系，该效果保持中性；若未来接入能让
	 * 非同属性技能强制获得属性一致加成的规则，应通过额外结构化字段表达，而不是让本效果读取技能名称或文本。
	 */
	data class SameElementBonusOverride(
		val multiplier: Double,
	) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 强化拳击类技能的伤害倍率。
	 *
	 * 该效果用于表达现代规则中“使用带拳击标签的技能时，技能威力按固定倍率提升”的稳定特性。引擎只读取
	 * [io.github.lishangbu.battleengine.model.BattleSkillSlot.punchBased] 结构化标签，不解析技能名称、中文文本
	 * 或资料库原始标记；资料导入层负责把公开技能资料里的 punch flag 归一成该布尔值。
	 *
	 * 该倍率属于攻击方特性带来的伤害公式修正，只影响拥有者主动使用普通物理/特殊技能造成的直接伤害。变化技能、
	 * 不带拳击标签的技能、间接伤害、反作用伤害、天气伤害和入场陷阱都不会读取它。它与属性一致、属性克制、
	 * 天气、场地、道具和低体力属性强化按现有最终倍率链叠乘。
	 */
	data class PunchBasedSkillDamageBoost(
		val multiplier: Double = 1.2,
	) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 强化切割类技能的伤害倍率。
	 *
	 * 该效果用于表达现代规则中“使用带切割标签的技能时，技能威力按固定倍率提升”的稳定特性。引擎只读取
	 * [io.github.lishangbu.battleengine.model.BattleSkillSlot.slicingBased] 结构化标签，不根据技能名称判断是否为
	 * 斩击、利刃或刀类技能；这样同一套规则可以被数据库种子、管理端维护和测试用例复用。
	 *
	 * 该倍率只属于攻击方的直接技能伤害修正，不改变技能原始威力、不赋予接触标签，也不影响击中要害等级、
	 * 命中率或替身交互。若某个切割技能同时被其它规则改变有效属性或有效威力，状态机仍先形成统一的技能槽和环境
	 * 快照，再由伤害公式按统一倍率顺序叠乘。
	 */
	data class SlicingBasedSkillDamageBoost(
		val multiplier: Double = 1.5,
	) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 强化接触类技能的伤害倍率。
	 *
	 * 该效果用于表达现代规则中“使用本次仍然构成接触的技能时，技能威力按固定倍率提升”的稳定特性。引擎先读取
	 * [io.github.lishangbu.battleengine.model.BattleSkillSlot.makesContact] 作为资料层静态标签，再通过
	 * [io.github.lishangbu.battleengine.model.makesEffectiveContact] 合并拳击手套这类动态非接触来源，最终只按本次
	 * 冻结后的接触事实结算倍率。
	 *
	 * 该效果只属于攻击方主动造成直接技能伤害时的公式修正，不影响接触事件本身是否发生，也不改变目标侧基于接触
	 * 触发的特性、道具或状态流程。也就是说，某技能若被其它规则动态改为非接触，接触反制和这里的伤害倍率都应
	 * 同时不再触发；后续若接入“远隔”等动态接触来源，也应继续复用同一个本次接触事实入口。
	 */
	data class ContactBasedSkillDamageBoost(
		val multiplier: Double = 1.3,
	) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 强化声音类技能的伤害倍率。
	 *
	 * 该效果用于表达现代规则中“使用带声音标签的技能时，技能威力按固定倍率提升”的稳定特性。引擎只读取
	 * [io.github.lishangbu.battleengine.model.BattleSkillSlot.soundBased]，不解析招式名称、文本描述或音效表现；
	 * 资料层负责把公开技能资料中的 sound flag 维护为结构化标签。
	 *
	 * 该效果只在拥有者作为攻击方主动造成普通物理/特殊技能伤害时生效。声音类变化技能仍然可以穿透替身或被声音
	 * 免疫阻止，但不会进入普通伤害公式；声音类伤害技能的替身穿透、目标免疫、属性克制和道具倍率仍由对应规则
	 * 分别处理，避免把声音标签的一切副作用塞进同一个特性效果。
	 */
	data class SoundBasedSkillDamageBoost(
		val multiplier: Double = 1.3,
	) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 减少受到声音类技能造成的伤害。
	 *
	 * 该效果用于表达现代规则中“受到带声音标签的技能攻击时，最终伤害按固定倍率降低”的防守方特性。它只读取
	 * [io.github.lishangbu.battleengine.model.BattleSkillSlot.soundBased]，且只影响普通物理/特殊技能的直接伤害；
	 * 声音类变化技能是否命中、是否穿透替身、是否被免疫，仍由命中和目标特性阶段处理。
	 *
	 * 如果攻击方本次技能拥有“无视目标特性效果”的结构化效果，伤害请求会标记为忽略防守方特性，此时该减伤不会
	 * 生效。这样声音减伤与属性吸收、满 HP 保留 1 HP、击中要害免疫等防守方特性共享同一绕过语义。
	 */
	data class SoundBasedSkillDamageReduction(
		val multiplier: Double = 0.5,
	) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 减少受到效果绝佳直接技能伤害时的最终伤害倍率。
	 *
	 * 该效果用于表达现代规则中“目标被属性相性判定为效果绝佳的物理/特殊技能命中时，最终伤害按固定倍率降低”的
	 * 防守方特性。它不保存具体特性名称，也不保存属性名称；伤害公式只读取已经由 [ElementEffectivenessChart]
	 * 计算出的本次属性克制倍率，`effectiveness > 1.0` 时视为触发。
	 *
	 * 该效果不会修改属性相性本身，因此事件、日志和后续规则仍然能看到原始效果绝佳倍率。它也不影响固定伤害、
	 * 间接伤害、天气伤害、入场陷阱或变化技能。若本次技能标记为忽略目标特性，伤害请求会统一跳过防守方特性
	 * 倍率，让它与声音类减伤、击中要害免疫等防守方规则共享同一绕过语义。
	 */
	data class SuperEffectiveDamageReduction(
		val multiplier: Double = 0.75,
	) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 满 HP 时减少受到的直接技能伤害。
	 *
	 * 该效果用于表达现代规则中“防守方当前 HP 仍为最大 HP 时，受到的物理/特殊直接技能伤害按固定倍率降低”的
	 * 稳定特性。它只读取战斗成员快照中的 `currentHp` 和 `maxHp`，不保存具体特性名称，也不解析本地化文本。
	 *
	 * 该倍率属于防守方最终伤害修正，不改变防御/特防能力值、不改变属性相性，也不影响固定伤害、间接伤害、
	 * 天气伤害、入场陷阱、反作用伤害或变化技能。多段攻击由状态机逐段更新 HP 后再次构造伤害请求；因此只有
	 * 第一段仍满足满 HP 条件时才会触发。若本次技能忽略目标特性，伤害请求会统一跳过该效果。
	 */
	data class FullHpDamageReduction(
		val multiplier: Double = 0.5,
	) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 按技能伤害分类减少受到的直接技能伤害。
	 *
	 * 该效果用于表达现代规则中“防守方受到某些伤害分类的直接技能命中时，最终伤害按固定倍率降低”的稳定特性。
	 * `damageClasses` 只允许包含 [BattleDamageClass.PHYSICAL] 或 [BattleDamageClass.SPECIAL]，因为变化类技能不会进入
	 * 普通伤害公式，也不存在可被这里修正的直接伤害。
	 *
	 * 该倍率属于防守方最终伤害修正，不改变防御/特防能力值、不改变技能分类本身，也不影响固定伤害、间接伤害、
	 * 天气伤害、入场陷阱、反作用伤害或变化技能。若本次技能忽略目标特性，伤害请求会统一跳过该效果。资料层
	 * 应把具体特性名称转换成需要匹配的伤害分类集合，纯引擎只读取结构化分类，不解析本地化文本或特性代号。
	 */
	data class DamageClassDamageReduction(
		val damageClasses: Set<BattleDamageClass>,
		val multiplier: Double = 0.5,
	) : BattleAbilityEffect {
		init {
			require(damageClasses.isNotEmpty()) { "damageClasses must not be empty" }
			require(BattleDamageClass.STATUS !in damageClasses) { "status skills do not use standard damage formula" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/**
	 * 修正防守方进入普通伤害公式的防御侧能力值。
	 *
	 * 该效果用于表达现代规则中“拥有者承受物理或特殊直接技能伤害时，防御或特防能力值按固定倍率参与公式”的
	 * 稳定特性。它和 [DamageClassDamageReduction] 不同：这里改变的是公式中的防御侧能力值，因此会影响基础伤害
	 * 的整数除法与取整位置；按分类减伤则属于最终伤害倍率。
	 *
	 * `stat` 当前只允许 [BattleStat.DEFENSE] 和 [BattleStat.SPECIAL_DEFENSE]，因为其它能力项不会作为防御侧能力值
	 * 进入普通伤害公式。`requiredTerrain` 为空时表示无环境要求；不为空时，只有当前全场场地匹配才触发。该效果
	 * 不改变战斗快照中的真实能力值或能力阶级，也不影响固定伤害、间接伤害、天气伤害、入场陷阱、反作用伤害或
	 * 变化技能。若本次技能忽略目标特性，伤害请求会统一跳过该效果。
	 */
	data class DefendingStatMultiplier(
		val stat: BattleStat,
		val multiplier: Double,
		val requiredTerrain: BattleTerrain? = null,
		val requiresMajorStatus: Boolean = false,
	) : BattleAbilityEffect {
		init {
			require(stat == BattleStat.DEFENSE || stat == BattleStat.SPECIAL_DEFENSE) {
				"defending stat multiplier only supports defense or special defense"
			}
			require(multiplier > 0.0) { "multiplier must be positive" }
			require(requiredTerrain != BattleTerrain.NONE) { "requiredTerrain cannot be NONE" }
		}
	}

	/**
	 * 修正攻击方进入普通伤害公式的攻击侧能力值。
	 *
	 * 该效果用于表达现代规则中“拥有者使用物理或特殊直接技能造成伤害时，攻击或特攻能力值按固定倍率参与公式”的
	 * 稳定特性。它改变的是基础伤害公式中的攻击侧能力值，因此会影响基础伤害整数除法和取整位置；这与最终伤害
	 * 倍率类特性不同，也不会写回战斗快照中的真实能力值或能力阶级。
	 *
	 * `stat` 当前只允许 [BattleStat.ATTACK] 和 [BattleStat.SPECIAL_ATTACK]，因为其它能力项不会作为攻击侧能力值
	 * 进入普通伤害公式。`requiredTerrain` 和 `requiredWeather` 为空时表示无环境要求；不为空时必须与当前环境匹配。
	 * `requiresMajorStatus` 表示拥有者必须正处于任意主要异常状态，适合表达异常状态下才强化攻击的稳定特性。
	 *
	 * 该效果不改变技能威力、属性一致、属性相性、命中、变化技能或间接伤害。物理攻击在应用该倍率后通常仍会继续
	 * 进入灼伤物理伤害减半流程；`ignoresBurnAttackReduction` 只表示同一个结构化特性明确绕过该灼伤减半，不影响
	 * 灼伤的回合末伤害、其它异常状态行为或状态本身。
	 */
	data class AttackingStatMultiplier(
		val stat: BattleStat,
		val multiplier: Double,
		val requiredTerrain: BattleTerrain? = null,
		val requiredWeather: BattleWeather? = null,
		val requiresMajorStatus: Boolean = false,
		val requiredMajorStatuses: Set<BattleMajorStatus> = emptySet(),
		val maximumHpFraction: Double? = null,
		val ignoresBurnAttackReduction: Boolean = false,
	) : BattleAbilityEffect {
		init {
			require(stat == BattleStat.ATTACK || stat == BattleStat.SPECIAL_ATTACK) {
				"attacking stat multiplier only supports attack or special attack"
			}
			require(multiplier > 0.0) { "multiplier must be positive" }
			require(requiredTerrain != BattleTerrain.NONE) { "requiredTerrain cannot be NONE" }
			require(requiredWeather != BattleWeather.NONE) { "requiredWeather cannot be NONE" }
			require(maximumHpFraction == null || maximumHpFraction in 0.0..1.0) {
				"maximumHpFraction must be between zero and one"
			}
		}
	}

	/**
	 * 受到接触类技能成功命中后，按概率把主要异常状态附加给攻击方。
	 *
	 * 该效果用于表达现代规则中一类防守方受接触后反制攻击方的稳定特性。它只在普通技能已经命中并至少完成
	 * 本次目标结算后触发；若技能被属性免疫、保护、替身完全阻止、目标已经倒下前未发生有效接触，或技能槽没有
	 * `makesContact` 标签，则不会进入该效果。
	 *
	 * `chancePercent` 为 100 时不消费额外随机数；低于 100 时使用独立的接触状态随机掷点。状态真正附加给攻击方前，
	 * 仍会复用主要异常状态的属性、场地、特性、道具和已有状态阻止流程，因此它不会绕过攻击方自身免疫。
	 */
	data class ContactStatusOnAttacker(
		val status: BattleMajorStatus,
		val chancePercent: Int,
	) : BattleAbilityEffect {
		init {
			require(chancePercent in 0..100) { "chancePercent must be between 0 and 100" }
		}
	}

	/**
	 * 受到接触类技能成功命中后，让攻击方按其自身最大 HP 比例受到伤害。
	 *
	 * 该效果用于表达粗糙皮肤、铁刺这类接触反伤特性。它只在目标本体被接触技能实际命中并完成伤害写入后触发；
	 * 击中替身、技能没有有效接触、攻击方携带免疫接触副作用的道具、攻击方免疫间接伤害，或本次技能忽略目标特性
	 * 时都不会造成反伤。伤害基数始终是攻击方自身最大 HP，而不是目标受到的伤害量，因此不会被随机浮动、
	 * 属性克制、屏障、目标剩余 HP 或多段命中的溢出伤害影响。
	 *
	 * `damageDenominator` 表示最大 HP 分母；例如 8 表示 `floor(maxHp / 8)`，最少 1 点，并夹取到攻击方当前 HP。
	 * 多个同类反伤效果会按资料顺序逐条结算；如果攻击方在前一个效果后已经倒下，后续效果不会再追加伤害事件。
	 */
	data class ContactDamageToAttacker(
		val damageDenominator: Int,
	) : BattleAbilityEffect {
		init {
			require(damageDenominator > 0) { "damageDenominator must be positive" }
		}
	}

	/** 受到接触后按概率从候选主要异常状态中等概率选择一种施加给攻击者。 */
	data class RandomContactStatusOnAttacker(
		val statuses: List<BattleMajorStatus>,
		val chancePercent: Int,
	) : BattleAbilityEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
			require(statuses.distinct().size == statuses.size) { "statuses must not contain duplicates" }
			require(chancePercent in 1..100) { "chancePercent must be in 1..100" }
		}
	}

	/** 受到异性成员的有效接触后，按概率使攻击者陷入着迷状态。 */
	data class ContactInfatuationOnAttacker(val chancePercent: Int = 30) : BattleAbilityEffect {
		init {
			require(chancePercent in 0..100) { "chancePercent must be between 0 and 100" }
		}
	}

	/** 场上存在指定场地时，把持有者临时改为对应单一属性。 */
	data class TerrainElementIdentity(val elementIdsByTerrain: Map<BattleTerrain, Long>) : BattleAbilityEffect {
		init {
			require(elementIdsByTerrain.isNotEmpty()) { "elementIdsByTerrain must not be empty" }
			require(BattleTerrain.NONE !in elementIdsByTerrain) { "NONE terrain cannot provide an element identity" }
			require(elementIdsByTerrain.values.all { it > 0 }) { "terrain element ids must be positive" }
		}
	}

	/** 指定属性技能在满足体力条件时提升优先度。 */
	data class ElementSkillPriorityBoost(
		val elementId: Long,
		val priorityDelta: Int,
		val requiresFullHp: Boolean = false,
	) : BattleAbilityEffect {
		init {
			require(elementId > 0) { "elementId must be positive" }
			require(priorityDelta > 0) { "priorityDelta must be positive" }
		}
	}

	/** 具有回复效果的技能提升优先度。 */
	data class HealingSkillPriorityBoost(val priorityDelta: Int) : BattleAbilityEffect {
		init {
			require(priorityDelta > 0) { "priorityDelta must be positive" }
		}
	}

	/** 持有者作为防守方时，修正攻击方进入公式的指定能力。 */
	data class OpponentAttackingStatMultiplier(
		val stat: BattleStat,
		val multiplier: Double,
	) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 持有者作为攻击方时，修正防守方进入公式的指定能力。 */
	data class OpponentDefendingStatMultiplier(
		val stat: BattleStat,
		val multiplier: Double,
	) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 承受接触类技能时的最终伤害倍率。 */
	data class ContactBasedSkillDamageReduction(val multiplier: Double) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 回合末按固定能力阶级变化。 */
	data class EndTurnStatStageChange(
		val stat: BattleStat,
		val stageDelta: Int,
	) : BattleAbilityEffect {
		init {
			require(stageDelta != 0) { "stageDelta must not be zero" }
		}
	}

	/** 回合末随机提升一项能力并降低另一项能力。 */
	data class EndTurnRandomStatStageChange(
		val increase: Int,
		val decrease: Int,
	) : BattleAbilityEffect {
		init {
			require(increase > 0) { "increase must be positive" }
			require(decrease < 0) { "decrease must be negative" }
		}
	}

	/** 回合末按概率或指定天气治愈自身主要异常状态。 */
	data class EndTurnMajorStatusCure(
		val chancePercent: Int = 100,
		val requiredWeathers: Set<BattleWeather> = emptySet(),
	) : BattleAbilityEffect {
		init {
			require(chancePercent in 1..100) { "chancePercent must be between one and one hundred" }
			require(BattleWeather.NONE !in requiredWeathers) { "requiredWeathers cannot contain NONE" }
		}
	}

	/** 回合末按概率治愈一名异常状态队友。 */
	data class EndTurnAllyMajorStatusCure(val chancePercent: Int) : BattleAbilityEffect {
		init {
			require(chancePercent in 1..100) { "chancePercent must be between one and one hundred" }
		}
	}

	/** 以指定异常状态代替其通常扣血，并在回合末回复体力。 */
	data class MajorStatusEndTurnHeal(
		val statuses: Set<BattleMajorStatus>,
		val healDenominator: Int,
	) : BattleAbilityEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
			require(healDenominator > 0) { "healDenominator must be positive" }
		}
	}

	/** 指定天气下的回合末间接伤害。 */
	data class WeatherEndTurnDamage(
		val weathers: Set<BattleWeather>,
		val damageDenominator: Int,
	) : BattleAbilityEffect {
		init {
			require(weathers.isNotEmpty()) { "weathers must not be empty" }
			require(BattleWeather.NONE !in weathers) { "weathers cannot contain NONE" }
			require(damageDenominator > 0) { "damageDenominator must be positive" }
		}
	}

	/** 回合末对处于指定异常状态的每名对手造成间接伤害。 */
	data class OpponentMajorStatusEndTurnDamage(
		val statuses: Set<BattleMajorStatus>,
		val damageDenominator: Int,
	) : BattleAbilityEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
			require(damageDenominator > 0) { "damageDenominator must be positive" }
		}
	}

	/** 降低承受的指定属性技能伤害。 */
	data class ElementSkillDamageReduction(
		val elementIds: Set<Long>,
		val multiplier: Double,
	) : BattleAbilityEffect {
		init {
			require(elementIds.isNotEmpty() && elementIds.all { it > 0 }) { "elementIds must contain positive ids" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 按属性克制区间强化攻击伤害。 */
	data class EffectivenessDamageBoost(
		val multiplier: Double,
		val requiresSuperEffective: Boolean = false,
		val requiresNotVeryEffective: Boolean = false,
	) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
			require(requiresSuperEffective.xor(requiresNotVeryEffective)) { "exactly one effectiveness condition is required" }
		}
	}

	/** 击中要害时的额外伤害倍率。 */
	data class CriticalHitDamageBoost(
		val multiplier: Double,
	) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 承受实际技能伤害后，按命中条件改变携带者或攻击者的能力阶级。 */
	data class ReceivedDamageStatStageChange(
		val stageChanges: Map<BattleStat, Int>,
		val elementIds: Set<Long> = emptySet(),
		val damageClasses: Set<BattleDamageClass> = emptySet(),
		val requiresContact: Boolean = false,
		val changesAttacker: Boolean = false,
	) : BattleAbilityEffect {
		init {
			require(stageChanges.isNotEmpty() && stageChanges.values.none { it == 0 }) { "stageChanges must be non-empty and non-zero" }
			require(elementIds.all { it > 0 }) { "elementIds must contain positive ids" }
			require(BattleDamageClass.STATUS !in damageClasses) { "damageClasses cannot contain status" }
		}
	}

	/** 被接触类技能造成伤害后，为自身与攻击者附加灭亡倒计时。 */
	data class ContactSharedPerishCountdown(val turns: Int) : BattleAbilityEffect {
		init {
			require(turns > 0) { "turns must be positive" }
		}
	}

	/** 被要害伤害命中且仍可战斗时，将指定能力直接设到目标阶级。 */
	data class CriticalDamageSetStatStage(
		val stat: BattleStat,
		val stage: Int,
	) : BattleAbilityEffect {
		init {
			require(stage in -6..6) { "stage must be between minus six and six" }
		}
	}

	/** 受伤后首次跨过指定体力阈值时改变一组能力阶级。 */
	data class DamageCrossedHpThresholdStatStageChange(
		val stageChanges: Map<BattleStat, Int>,
		val thresholdNumerator: Int = 1,
		val thresholdDenominator: Int = 2,
	) : BattleAbilityEffect {
		init {
			require(stageChanges.isNotEmpty()) { "stageChanges must not be empty" }
			require(stageChanges.values.none { it == 0 }) { "stage changes must not contain zero" }
			require(thresholdNumerator > 0 && thresholdDenominator > 0) { "threshold must be positive" }
			require(thresholdNumerator <= thresholdDenominator) { "threshold cannot exceed full hp" }
		}
	}

	/** 受到直接伤害后设置天气。 */
	data class ReceivedDamageWeatherChange(
		val weather: BattleWeather,
		val turnsRemaining: Int = 5,
	) : BattleAbilityEffect {
		init {
			require(weather != BattleWeather.NONE) { "weather must be active" }
			require(turnsRemaining > 0) { "turnsRemaining must be positive" }
		}
	}

	/** 受到直接伤害后设置场地。 */
	data class ReceivedDamageTerrainChange(
		val terrain: BattleTerrain,
		val turnsRemaining: Int = 5,
	) : BattleAbilityEffect {
		init {
			require(terrain != BattleTerrain.NONE) { "terrain must be active" }
			require(turnsRemaining > 0) { "turnsRemaining must be positive" }
		}
	}

	/** 有成员倒下后提升指定能力；可限定必须由持有者造成倒下。 */
	data class FaintStatStageBoost(
		val stat: BattleStat,
		val stageDelta: Int,
		val requiresHolderCausedFaint: Boolean,
	) : BattleAbilityEffect {
		init {
			require(stageDelta > 0) { "stageDelta must be positive" }
		}
	}

	/** 持有者造成对手倒下后，一场战斗仅一次提升多项能力。 */
	data class OncePerBattleCausedFaintMultiStatBoost(
		val stats: Set<BattleStat>,
		val stageDelta: Int,
	) : BattleAbilityEffect {
		init {
			require(stats.isNotEmpty()) { "stats must not be empty" }
			require(stageDelta > 0) { "stageDelta must be positive" }
		}
	}

	/** 持有者造成对手倒下后提升原始数值最高的能力。 */
	data class FaintHighestStatBoost(private val marker: Unit = Unit) : BattleAbilityEffect

	/**
	 * 成员出场时修改当前对手上场成员的能力阶级。
	 *
	 * 该结构用于表达现代规则中“出场时令对手能力下降”的稳定特性。它不保存具体特性名称，也不保存本地化文本；
	 * 资料层把特性 policy 转换为要修改的能力项和阶级变化量。目标固定为当前对手上场成员，适合单打和双打中的
	 * 常见出场降攻规则。替身、反制能力提升、特性失效等复杂交互会在对应模型具备后继续扩展。
	 */
	data class SwitchInStatStageChange(
		val stat: BattleStat,
		val stageDelta: Int,
		val target: BattleEffectTarget = BattleEffectTarget.TARGET,
	) : BattleAbilityEffect {
		init {
			require(stageDelta in -6..6 && stageDelta != 0) { "stageDelta must be between -6 and 6 and not zero" }
		}
	}

	/** 出场时比较对手防御总和，并提升对应攻击能力。 */
	data class SwitchInOpponentDefenseComparisonBoost(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 出场时回复当前上场队友。 */
	data class SwitchInAllyHeal(val healDenominator: Int) : BattleAbilityEffect {
		init {
			require(healDenominator > 0) { "healDenominator must be positive" }
		}
	}

	/** 出场时复制当前上场队友的全部能力阶级。 */
	data class SwitchInAllyStatStageCopy(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 出场时清除当前上场队友的全部能力阶级变化。 */
	data class SwitchInAllyStatStageReset(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 出场时清除场上双方的伤害减免屏障。 */
	data class SwitchInClearAllSideDamageReductions(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 为当前上场队友的指定伤害分类提供最终伤害倍率。 */
	data class AllySkillDamageBoost(
		val multiplier: Double,
		val damageClasses: Set<BattleDamageClass> = setOf(BattleDamageClass.PHYSICAL, BattleDamageClass.SPECIAL),
	) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
			require(damageClasses.isNotEmpty() && BattleDamageClass.STATUS !in damageClasses) {
				"damageClasses must contain damaging classes"
			}
		}
	}

	/** 在场时为全场指定属性技能提供光环倍率，并声明被反转时的倍率。 */
	data class FieldElementSkillDamageAura(
		val elementId: Long,
		val multiplier: Double,
		val reversedMultiplier: Double,
	) : BattleAbilityEffect {
		init {
			require(elementId > 0) { "elementId must be positive" }
			require(multiplier > 0.0) { "multiplier must be positive" }
			require(reversedMultiplier > 0.0) { "reversedMultiplier must be positive" }
		}
	}

	/** 在场时反转全场属性伤害光环。 */
	data class FieldDamageAuraReversal(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 降低当前上场队友承受的公式伤害。 */
	data class AllyReceivedDamageReduction(val multiplier: Double) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 声明特性属于一个可被队友条件检测的互助组。 */
	data class AllyAbilityGroupMembership(val groupCode: String) : BattleAbilityEffect {
		init {
			require(groupCode.isNotBlank()) { "groupCode must not be blank" }
		}
	}

	/** 当当前上场队友属于指定互助组时，强化自身的攻击能力值。 */
	data class AllyAbilityPresenceAttackingStatMultiplier(
		val groupCode: String,
		val stat: BattleStat,
		val multiplier: Double,
	) : BattleAbilityEffect {
		init {
			require(groupCode.isNotBlank()) { "groupCode must not be blank" }
			require(stat == BattleStat.ATTACK || stat == BattleStat.SPECIAL_ATTACK) {
				"stat must be an attacking stat"
			}
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 免疫来自当前上场队友的伤害技能。 */
	data class AllyDamageImmunity(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 按技能原属性或声音标签覆盖本次技能属性，并可附带转换增伤。 */
	data class SkillElementOverride(
		val elementId: Long,
		val originalElementIds: Set<Long> = emptySet(),
		val requiresSoundBased: Boolean = false,
		val damageMultiplier: Double = 1.0,
	) : BattleAbilityEffect {
		init {
			require(elementId > 0) { "elementId must be positive" }
			require(originalElementIds.all { it > 0 }) { "originalElementIds must contain positive ids" }
			require(damageMultiplier > 0.0) { "damageMultiplier must be positive" }
		}
	}

	/** 绕过对手的替身与伤害减免屏障。 */
	data class OpponentBarrierBypass(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 为原本不附带畏缩的伤害技能追加畏缩概率。 */
	data class AdditionalFlinchChance(val chancePercent: Int) : BattleAbilityEffect {
		init {
			require(chancePercent in 1..100) { "chancePercent must be between one and one hundred" }
		}
	}

	/** 倍增伤害技能所携带的附加效果概率。 */
	data class SecondaryEffectChanceMultiplier(val multiplier: Double) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 阻止自身持有的道具被战斗效果转移。 */
	data class HeldItemTransferImmunity(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 阻止持有者的携带道具被消费、打落或转移。 */
	data class HeldItemRemovalImmunity(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 把携带的属性强化道具对应属性作为自身当前唯一属性。 */
	data class HeldItemElementIdentity(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 将对自身造成的吸取回复反转为对技能使用者的伤害。 */
	data class DrainHealingReversal(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 按己方已经倒下的成员数量提高自身造成的伤害。 */
	data class FaintedAllyDamageBoost(
		val incrementPerFaintedAlly: Double,
		val maximumFaintedAllies: Int,
	) : BattleAbilityEffect {
		init {
			require(incrementPerFaintedAlly > 0.0) { "incrementPerFaintedAlly must be positive" }
			require(maximumFaintedAllies > 0) { "maximumFaintedAllies must be positive" }
		}
	}

	/** 攻击本回合刚刚替换上场的目标时提高伤害。 */
	data class SwitchedInTargetDamageBoost(val multiplier: Double) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 目标本回合已经行动时提高自身造成的伤害。 */
	data class TargetAlreadyActedDamageBoost(val multiplier: Double) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 在相同技能优先度中强制最后行动。 */
	data class ForcedLastActionOrder(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 按概率把本回合技能行动提升到提前行动档位。 */
	data class RandomActionOrderBoost(val chancePercent: Int) : BattleAbilityEffect {
		init {
			require(chancePercent in 1..100) { "chancePercent must be between one and one hundred" }
		}
	}

	/** 上场后的最初若干回合按倍率修正指定能力值。 */
	data class InitialActiveTurnsStatMultiplier(
		val turns: Int,
		val stats: Set<BattleStat>,
		val multiplier: Double,
	) : BattleAbilityEffect {
		init {
			require(turns > 0) { "turns must be positive" }
			require(stats.isNotEmpty()) { "stats must not be empty" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 满体力时把本次伤害技能的属性克制倍率覆盖为指定值。 */
	data class FullHpEffectivenessOverride(val multiplier: Double) : BattleAbilityEffect {
		init {
			require(multiplier >= 0.0) { "multiplier must not be negative" }
		}
	}

	/** 受到物理伤害后在攻击者一侧布置指定入口陷阱。 */
	data class ReceivedPhysicalDamageOpponentSideHazard(val kind: BattleSideEntryHazardKind) : BattleAbilityEffect

	/** 保护自身所在一侧的当前上场成员免受指定临时状态。 */
	data class SideVolatileStatusImmunity(val statuses: Set<BattleVolatileStatus>) : BattleAbilityEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
		}
	}

	/** 免疫弹类与球类技能。 */
	data class ProjectileSkillImmunity(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 提高波动类技能造成的伤害。 */
	data class PulseBasedSkillDamageBoost(val multiplier: Double) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 提高啃咬类技能造成的伤害。 */
	data class BiteBasedSkillDamageBoost(val multiplier: Double) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 自身没有道具且伤害命中后夺取目标持有的道具。 */
	data class DamagingSkillStealTargetHeldItem(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 自身没有道具且受到接触伤害后夺取攻击者持有的道具。 */
	data class ContactStealAttackerHeldItem(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 受到伤害后把自身当前属性改为本次技能的有效属性。 */
	data class ReceivedDamageElementChange(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 受到直接伤害后充能，使下一次指定属性伤害翻倍。 */
	data class ReceivedDamageNextElementDamageBoost(
		val elementId: Long,
		val multiplier: Double,
		val windOnly: Boolean = false,
	) : BattleAbilityEffect {
		init {
			require(elementId > 0) { "elementId must be positive" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 吸收风类技能并提升自身指定能力阶级。 */
	data class WindSkillImmunityStatStageChange(
		val stat: BattleStat,
		val stageDelta: Int,
	) : BattleAbilityEffect {
		init {
			require(stageDelta != 0) { "stageDelta must not be zero" }
		}
	}

	/** 每次上场后从第二个回合开始隔回合阻止技能行动。 */
	data class EveryOtherActiveTurnActionBlock(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 保护一侧指定属性的成员免受主要异常状态。 */
	data class SideElementMajorStatusImmunity(
		val elementId: Long,
		val statuses: Set<BattleMajorStatus>,
	) : BattleAbilityEffect {
		init {
			require(elementId > 0) { "elementId must be positive" }
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
		}
	}

	/** 保护一侧指定属性的成员免受对手造成的能力下降。 */
	data class SideElementStatDropImmunity(val elementId: Long) : BattleAbilityEffect {
		init {
			require(elementId > 0) { "elementId must be positive" }
		}
	}

	/** 仅阻止出场特性造成的指定能力下降。 */
	data class SwitchInStatStageReductionImmunity(val stats: Set<BattleStat>) : BattleAbilityEffect {
		init {
			require(stats.isNotEmpty()) { "stats must not be empty" }
		}
	}

	/** 出场特性尝试降低指定能力时，阻止下降并改为提升自身能力。 */
	data class SwitchInStatReductionReactiveBoost(
		val triggerStat: BattleStat,
		val boostStat: BattleStat,
		val stageDelta: Int,
	) : BattleAbilityEffect {
		init {
			require(stageDelta > 0) { "stageDelta must be positive" }
		}
	}

	/** 放弃伤害技能的附加状态与能力变化，以换取伤害提升。 */
	data class SecondaryEffectsSuppressedDamageBoost(val multiplier: Double) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 吸收指定属性技能，并在本次上场期间强化自身同属性伤害。 */
	data class ElementSkillAbsorbDamageBoost(
		val elementId: Long,
		val multiplier: Double,
	) : BattleAbilityEffect {
		init {
			require(elementId > 0) { "elementId must be positive" }
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 出场时复制一名当前上场对手的特性。 */
	data class SwitchInCopyOpponentAbility(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 受到接触伤害后把攻击者特性替换为持有者当前特性。 */
	data class ContactReplaceAttackerAbilityWithHolder(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 受到接触伤害后与攻击者交换当前特性。 */
	data class ContactSwapAbilities(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 当前上场队友倒下时复制该队友的运行时特性。 */
	data class FaintedAllyAbilityCopy(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 被对手附加灼伤、中毒或麻痹后，把同一异常状态反射给来源。 */
	data class OpponentMajorStatusReflection(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 每次上场首次使用不同属性技能前，把自身属性改为该技能的有效属性。 */
	data class FirstSkillElementChangeSinceSwitchIn(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 符合条件的单段单目标伤害技能追加一次四分之一伤害的第二段。 */
	data class SingleTargetSecondHit(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 成功使对手中毒或剧毒后，立即使该目标混乱。 */
	data class PoisonApplicationConfusion(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 太晶化时清除当前天气与场地。 */
	data class TerastallizationEnvironmentClear(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 对手提升能力阶级后，复制相同的正向阶级变化。 */
	data class OpponentStatStageIncreaseCopy(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 把对手施加给自身的能力下降反射回来源。 */
	data class OpponentStatStageReductionReflection(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 把四分之一体力触发的树果类携带道具阈值扩大为二分之一。 */
	data class LowHpItemTriggerThresholdHalf(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 每次成功消费树果后回复最大 HP 的指定分数。 */
	data class BerryConsumptionHeal(val healDenominator: Int) : BattleAbilityEffect {
		init {
			require(healDenominator > 0) { "healDenominator must be positive" }
		}
	}

	/** 回合结束时按概率恢复最近消费的树果；指定天气下必定成功。 */
	data class EndTurnConsumedBerryRestore(
		val chancePercent: Int,
		val guaranteedWeather: BattleWeather? = null,
	) : BattleAbilityEffect {
		init {
			require(chancePercent in 1..100) { "chancePercent must be in 1..100" }
		}
	}

	/** 在消费树果后的下一个回合结束时再次执行该树果效果。 */
	data class EndTurnConsumedBerryReplay(val delayTurns: Int = 1) : BattleAbilityEffect {
		init {
			require(delayTurns > 0) { "delayTurns must be positive" }
		}
	}

	/** 放大持有者消费树果时产生的数值效果。 */
	data class BerryEffectMultiplier(val multiplier: Double) : BattleAbilityEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
		}
	}

	/** 阻止对手当前上场成员消费树果。 */
	data class OpponentBerryConsumptionPrevention(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 同侧上场成员消费道具后，把自身道具交给该成员。 */
	data class AllyItemConsumptionTransfer(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 回合结束时捡起本回合最后一个仍可取得的已消费道具。 */
	data class EndTurnPickupConsumedItem(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 在指定天气或场地下强化原始数值最高的能力。 */
	data class EnvironmentHighestStatMultiplier(
		val requiredWeather: BattleWeather? = null,
		val requiredTerrain: BattleTerrain? = null,
	) : BattleAbilityEffect {
		init {
			require((requiredWeather != null) xor (requiredTerrain != null)) {
				"exactly one environment requirement must be configured"
			}
			require(requiredWeather != BattleWeather.NONE) { "required weather must be active" }
			require(requiredTerrain != BattleTerrain.NONE) { "required terrain must be active" }
		}
	}

	/**
	 * 成员出场时设置全场天气。
	 *
	 * 该结构用于表达现代规则中“进入场地后改变天气”的稳定特性。引擎只保存目标天气和持续回合，不保存具体
	 * 特性名称、技能名或本地化文本。`turnsRemaining` 为 null 时表示永久天气或测试用例不要求递减；
	 * 现代主系列普通天气特性通常传入 5，让回合末持续时间系统统一递减和结束。
	 */
	data class SwitchInWeatherChange(
		val weather: BattleWeather,
		val turnsRemaining: Int? = 5,
	) : BattleAbilityEffect {
		init {
			require(weather != BattleWeather.NONE) { "switch-in weather change requires an active weather" }
			require(turnsRemaining == null || turnsRemaining > 0) {
				"turnsRemaining must be positive when present"
			}
		}
	}

	/** 出场时公开所有当前对手的携带道具。 */
	data class SwitchInRevealOpponentHeldItems(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 出场时公开当前对手威力最高的一个技能。 */
	data class SwitchInRevealOpponentHighestPowerSkill(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 出场时变身为当前对手。 */
	data class SwitchInTransformIntoOpponent(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 入场时伪装为己方队尾仍可战斗的其他成员。 */
	data class SwitchInDisguiseAsLastHealthyAlly(private val marker: Unit = Unit) : BattleAbilityEffect

	/** 出场时侦测对手的效果绝佳或一击必杀技能。 */
	data class SwitchInDetectDangerousOpponentSkill(private val marker: Unit = Unit) : BattleAbilityEffect

	/**
	 * 成员出场时设置全场场地。
	 *
	 * 该结构覆盖现代规则中“进入场地后改变场地”的稳定特性。场地具体如何影响伤害、状态免疫、回合末回复或
	 * 先制封锁，仍由环境规则和状态机在对应阶段解释；出场特性只负责把环境事实写入战斗状态。
	 */
	data class SwitchInTerrainChange(
		val terrain: BattleTerrain,
		val turnsRemaining: Int? = 5,
	) : BattleAbilityEffect {
		init {
			require(terrain != BattleTerrain.NONE) { "switch-in terrain change requires an active terrain" }
			require(turnsRemaining == null || turnsRemaining > 0) {
				"turnsRemaining must be positive when present"
			}
		}
	}
}
