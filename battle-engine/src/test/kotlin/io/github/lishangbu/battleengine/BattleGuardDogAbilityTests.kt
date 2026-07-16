package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleStat
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleGuardDogAbilityTests {
	@Test
	fun `guard dog blocks intimidate and raises attack instead`() {
		val started = BattleEngine().start(
			initialState(
				first = participant(
					"guard-dog",
					100,
					abilityEffects = listOf(
						BattleAbilityEffect.SwitchInStatReductionReactiveBoost(
							triggerStat = BattleStat.ATTACK,
							boostStat = BattleStat.ATTACK,
							stageDelta = 1,
						),
					),
				),
				second = participant(
					"intimidator",
					50,
					abilityEffects = listOf(
						BattleAbilityEffect.SwitchInStatStageChange(BattleStat.ATTACK, -1),
					),
				),
			),
		)

		assertEquals(1, started.participant("guard-dog")?.statStage(BattleStat.ATTACK))
	}
}
