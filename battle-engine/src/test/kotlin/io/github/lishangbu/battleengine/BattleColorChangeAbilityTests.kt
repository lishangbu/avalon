package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleColorChangeAbilityTests {
	@Test
	fun `color change replaces the holders elements with the damaging skill element`() {
		val fireSkill = damagingSkill(skillId = 1041, elementId = 10, power = 40)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = fireSkill),
					second = participant(
						"holder",
						50,
						elementId = 1,
						abilityEffects = listOf(BattleAbilityEffect.ReceivedDamageElementChange()),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", fireSkill.skillId, "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(setOf(10L), resolved.participant("holder")?.elementIds)
	}
}
