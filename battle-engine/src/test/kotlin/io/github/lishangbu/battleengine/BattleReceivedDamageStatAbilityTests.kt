package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleReceivedDamageStatAbilityTests {
	@Test
	fun `received contact damage changes holder and attacker stat stages`() {
		val contactSkill = damagingSkill(makesContact = true)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = contactSkill),
					second = participant(
						"holder",
						50,
						abilityEffects = listOf(
							BattleAbilityEffect.ReceivedDamageStatStageChange(mapOf(BattleStat.DEFENSE to 1)),
							BattleAbilityEffect.ReceivedDamageStatStageChange(
								mapOf(BattleStat.SPEED to -1),
								requiresContact = true,
								changesAttacker = true,
							),
						),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", contactSkill.skillId, "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(1, resolved.participant("holder")?.statStage(BattleStat.DEFENSE))
		assertEquals(-1, resolved.participant("attacker")?.statStage(BattleStat.SPEED))
	}

	@Test
	fun `received element stat change requires matching effective element`() {
		val fireSkill = damagingSkill(elementId = 10)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = fireSkill),
					second = participant(
						"holder",
						50,
						abilityEffects = listOf(
							BattleAbilityEffect.ReceivedDamageStatStageChange(
								mapOf(BattleStat.SPEED to 6),
								elementIds = setOf(10, 11),
							),
						),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", fireSkill.skillId, "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(6, resolved.participant("holder")?.statStage(BattleStat.SPEED))
	}
}
