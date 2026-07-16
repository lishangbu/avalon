package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BattlePerishBodyAbilityTests {
	@Test
	fun `contact damage starts a shared perish countdown and both participants faint at zero`() {
		val skill = damagingSkill(skillId = 5200, power = 1, makesContact = true)
		val engine = BattleEngine()
		val started = engine.start(
			initialState(
				first = participant("attacker", 100, skill = skill),
				second = participant(
					"perish-body-holder",
					50,
					abilityEffects = listOf(BattleAbilityEffect.ContactSharedPerishCountdown(3)),
				),
			),
		)

		val firstTurn = engine.resolveTurn(
			started,
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "perish-body-holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		assertEquals(2, firstTurn.participant("attacker")?.perishTurnsRemaining)
		assertEquals(2, firstTurn.participant("perish-body-holder")?.perishTurnsRemaining)

		val secondTurn = engine.resolveTurn(firstTurn, emptyList(), ScriptedBattleRandom(emptyList()))
		assertEquals(1, secondTurn.participant("attacker")?.perishTurnsRemaining)
		assertEquals(1, secondTurn.participant("perish-body-holder")?.perishTurnsRemaining)

		val thirdTurn = engine.resolveTurn(secondTurn, emptyList(), ScriptedBattleRandom(emptyList()))
		assertFalse(requireNotNull(thirdTurn.participant("attacker")).canBattle())
		assertFalse(requireNotNull(thirdTurn.participant("perish-body-holder")).canBattle())
		assertEquals(null, thirdTurn.result?.winningSideId)
		assertEquals(
			listOf("attacker", "perish-body-holder"),
			thirdTurn.events.filterIsInstance<BattleEvent.ParticipantFainted>().map { it.actorId },
		)
	}

	@Test
	fun `leaving the battlefield clears perish countdown`() {
		val participant = participant("holder", 100).copy(perishTurnsRemaining = 2)

		assertEquals(0, participant.leaveBattlefield().perishTurnsRemaining)
	}
}
