package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleSwitchOutAbilityTests {
	@Test
	fun `natural cure and regenerator apply before holder leaves the field`() {
		val holder = participant(
			"holder",
			100,
			currentHp = 40,
			abilityEffects = listOf(
				BattleAbilityEffect.SwitchOutMajorStatusCure(),
				BattleAbilityEffect.SwitchOutHeal(3),
			),
		).copy(majorStatus = BattleMajorStatus.POISON)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = holder,
					second = participant("opponent", 50),
					firstBench = listOf(participant("reserve", 80)),
				),
			),
			listOf(BattleAction.SwitchParticipant("holder", "reserve")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(73, resolved.participant("holder")?.currentHp)
		assertEquals(null, resolved.participant("holder")?.majorStatus)
	}
}
