package io.github.lishangbu.battleengine.model

/**
 * 携带道具在战斗中的可执行效果。
 *
 * 第一批覆盖几类常见 hook：造成伤害时提升倍率并按伤害反伤、回合末按最大 HP 比例回复，以及稳定状态免疫。
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
