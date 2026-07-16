package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleBerryConsumptionAbilityTests {
	@Test
	fun `cheek pouch heals after the holder consumes a low hp berry`() {
		val skill = damagingSkill(skillId = 1093, power = 40)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill),
					second = participant(
						"cheek-pouch-holder",
						50,
						currentHp = 60,
						abilityEffects = listOf(BattleAbilityEffect.BerryConsumptionHeal(3)),
						itemId = 1093,
						itemEffects = listOf(
							BattleItemEffect.BerryMarker(),
							BattleItemEffect.LowHpHeal(healDenominator = 4),
						),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "cheek-pouch-holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(90, resolved.participant("cheek-pouch-holder")?.currentHp)
		assertEquals(null, resolved.participant("cheek-pouch-holder")?.itemId)
	}
}
