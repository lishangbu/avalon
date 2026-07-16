package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleSwitchInDangerDetectionAbilityTests {
	@Test
	fun `anticipation detects an opponents super effective damaging skill`() {
		val fireSkill = damagingSkill(skillId = 4400, elementId = 10, power = 60)
		val resolved = BattleEngine().start(
			initialState(
				first = participant(
					"holder",
					100,
					elementId = 12,
					abilityEffects = listOf(BattleAbilityEffect.SwitchInDetectDangerousOpponentSkill()),
				),
				second = participant("opponent", 50, skill = fireSkill),
				rules = neutralRules().copy(
					elementChart = ElementEffectivenessChart(mapOf(10L to mapOf(12L to 2.0))),
				),
			),
		)

		val event = resolved.events.filterIsInstance<BattleEvent.DangerousOpponentSkillDetected>().single()
		assertEquals("opponent", event.targetActorId)
		assertEquals(4400, event.skillId)
	}

	@Test
	fun `anticipation stays silent when opponents have no dangerous skill`() {
		val resolved = BattleEngine().start(
			initialState(
				first = participant(
					"holder",
					100,
					abilityEffects = listOf(BattleAbilityEffect.SwitchInDetectDangerousOpponentSkill()),
				),
				second = participant("opponent", 50),
			),
		)

		assertEquals(0, resolved.events.filterIsInstance<BattleEvent.DangerousOpponentSkillDetected>().size)
	}
}
