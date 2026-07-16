package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.damage.BattleDamageRequest
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import kotlin.test.Test
import kotlin.test.assertTrue

/** 验证进化奇石只强化仍可进化成员的防御与特防。 */
class BattleEvioliteItemTests {
	@Test
	fun `eviolite reduces damage only for evolvable holder`() {
		val effect = BattleItemEffect.EvolvableDefendingStatMultiplier(
			setOf(BattleStat.DEFENSE, BattleStat.SPECIAL_DEFENSE),
			1.5,
		)
		val attacker = participant("attacker", 100)
		val holder = participant("holder", 50, itemId = 538, itemEffects = listOf(effect))
		val calculator = BattleDamageCalculator()
		fun damage(canEvolve: Boolean) = calculator.calculate(
			BattleDamageRequest(
				attacker = attacker,
				defender = holder.copy(canEvolve = canEvolve),
				skill = damagingSkill(),
				rules = neutralRules(),
				randomPercent = 100,
			),
		).amount

		assertTrue(damage(canEvolve = true) < damage(canEvolve = false))
	}
}
