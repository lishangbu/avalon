package io.github.lishangbu.battleengine.model

/**
 * 特性在战斗中的可执行效果。
 *
 * 该 sealed 类型是规则资料 policy code 进入纯引擎后的结构化形态。第一批实现几个高价值 hook：
 * 低体力时强化指定属性伤害、天气下速度修正、天气伤害免疫、受到接触类技能后有概率给攻击方附加主要异常状态，
 * 以及稳定状态免疫。
 *
 * 后续每新增一种复杂特性，都应该先明确触发阶段、输入状态、不变量和对照 fixture，再扩展这里或拆分专门处理器。
 */
sealed interface BattleAbilityEffect {
	/**
	 * 免疫一组主要异常状态。
	 *
	 * 用于表达免疫中毒、免疫灼伤、免疫睡眠等稳定特性。具体特性名不进入引擎，避免把本地化文本或资料库名称
	 * 混进规则状态机；资料层负责把特性翻译成这类结构化效果。
	 */
	data class MajorStatusImmunity(
		val statuses: Set<BattleMajorStatus>,
	) : BattleAbilityEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
		}
	}

	/**
	 * 免疫一组临时状态。
	 *
	 * 当前主要用于表达混乱免疫；畏缩免疫、着迷免疫等后续临时状态增加后也可以复用同一结构。
	 */
	data class VolatileStatusImmunity(
		val statuses: Set<BattleVolatileStatus>,
	) : BattleAbilityEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
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

	data class ContactStatusOnAttacker(
		val status: BattleMajorStatus,
		val chancePercent: Int,
	) : BattleAbilityEffect {
		init {
			require(chancePercent in 0..100) { "chancePercent must be between 0 and 100" }
		}
	}

	/**
	 * 成员出场时修改当前对手上场成员的能力阶级。
	 *
	 * 该结构用于表达现代规则中“出场时令对手能力下降”的稳定特性。它不保存具体特性名称，也不保存本地化文本；
	 * 资料层把特性 policy 转换为要修改的能力项和阶级变化量。第一批只支持当前对手上场成员作为目标，
	 * 适合单打和双打中的常见出场降攻规则。替身、反制能力提升、特性失效等复杂交互会在对应模型具备后继续扩展。
	 */
	data class SwitchInStatStageChange(
		val stat: BattleStat,
		val stageDelta: Int,
	) : BattleAbilityEffect {
		init {
			require(stageDelta in -6..6 && stageDelta != 0) { "stageDelta must be between -6 and 6 and not zero" }
		}
	}
}
