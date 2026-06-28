package io.github.lishangbu.battleengine.model

/**
 * 携带道具在战斗中的可执行效果。
 *
 * 第一批只覆盖两类常见 hook：造成伤害时提升倍率并按伤害反伤，以及回合末按最大 HP 比例回复。
 * 是否消耗、锁招、低体力树果和复杂触发顺序会继续扩展为新的结构化效果，而不是在引擎中解析自由文本。
 */
sealed interface BattleItemEffect {
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
