package io.github.lishangbu.battleengine.model

/**
 * 携带道具在战斗中的可执行效果。
 *
 * 第一批覆盖几类常见 hook：造成伤害时提升倍率并按伤害反伤、回合末按最大 HP 比例回复、天气伤害免疫，
 * 以及稳定状态免疫。
 * 是否消耗、锁招、低体力树果和复杂触发顺序会继续扩展为新的结构化效果，而不是在引擎中解析自由文本。
 */
sealed interface BattleItemEffect {
	/**
	 * 携带道具提供的一组主要异常状态免疫。
	 *
	 * 道具是否被消耗、能否被拍落等物品生命周期不在该效果中表达；这里仅描述进入状态附加流程前的稳定阻止条件。
	 */
	data class MajorStatusImmunity(
		val statuses: Set<BattleMajorStatus>,
	) : BattleItemEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
		}
	}

	/**
	 * 携带道具提供的一组临时状态免疫。
	 */
	data class VolatileStatusImmunity(
		val statuses: Set<BattleVolatileStatus>,
	) : BattleItemEffect {
		init {
			require(statuses.isNotEmpty()) { "statuses must not be empty" }
		}
	}

	/**
	 * 携带道具提供的一组天气伤害免疫。
	 *
	 * 该效果只阻止天气在回合末直接造成的固定伤害，不表达粉末免疫、道具消耗或其它道具生命周期。
	 */
	data class WeatherDamageImmunity(
		val weathers: Set<BattleWeather>,
	) : BattleItemEffect {
		init {
			require(weathers.isNotEmpty()) { "weathers must not be empty" }
			require(BattleWeather.NONE !in weathers) { "weather damage immunity cannot target NONE" }
		}
	}

	data class DamageBoostWithRecoil(
		val multiplier: Double,
		val recoilDenominator: Int,
	) : BattleItemEffect {
		init {
			require(multiplier > 0.0) { "multiplier must be positive" }
			require(recoilDenominator > 0) { "recoilDenominator must be positive" }
		}
	}

	data class HeldEndTurnHeal(
		val healDenominator: Int,
	) : BattleItemEffect {
		init {
			require(healDenominator > 0) { "healDenominator must be positive" }
		}
	}
}
