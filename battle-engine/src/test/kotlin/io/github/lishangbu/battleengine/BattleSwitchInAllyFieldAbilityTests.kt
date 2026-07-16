package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.model.BattleStat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleSwitchInAllyFieldAbilityTests {
	@Test
	fun `switch in ally and field effects heal copy reset and clear screens`() {
		val holder = participant(
			"holder",
			100,
			abilityEffects = listOf(
				BattleAbilityEffect.SwitchInAllyHeal(4),
				BattleAbilityEffect.SwitchInAllyStatStageCopy(),
				BattleAbilityEffect.SwitchInAllyStatStageReset(),
				BattleAbilityEffect.SwitchInClearAllSideDamageReductions(),
			),
		)
		val ally = participant("ally", 80, currentHp = 50).changeStatStage(BattleStat.ATTACK, 2)
		val state = BattleEngine().start(
			doubleInitialState(
				firstA = holder,
				firstB = ally,
				secondA = participant("opponent-a", 70),
				secondB = participant("opponent-b", 60),
				firstSideDamageReductions = listOf(
					BattleSideDamageReduction(BattleSideDamageReductionKind.PHYSICAL, 5),
				),
				secondSideDamageReductions = listOf(
					BattleSideDamageReduction(BattleSideDamageReductionKind.SPECIAL, 5),
				),
			),
		)

		assertEquals(75, state.participant("ally")?.currentHp)
		assertEquals(2, state.participant("holder")?.statStage(BattleStat.ATTACK))
		assertEquals(0, state.participant("ally")?.statStage(BattleStat.ATTACK))
		assertTrue(state.sides.all { it.damageReductions.isEmpty() })
	}
}
