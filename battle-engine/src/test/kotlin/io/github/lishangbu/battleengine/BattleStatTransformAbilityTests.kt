package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleStatTransformAbilityTests {
	@Test
	fun `contrary reverses and simple doubles stage deltas`() {
		val contrary = participant(
			"contrary",
			100,
			abilityEffects = listOf(BattleAbilityEffect.StatStageDeltaMultiplier(-1)),
		).changeStatStage(BattleStat.ATTACK, 1)
		val simple = participant(
			"simple",
			80,
			abilityEffects = listOf(BattleAbilityEffect.StatStageDeltaMultiplier(2)),
		).changeStatStage(BattleStat.DEFENSE, -1)

		assertEquals(-1, contrary.statStage(BattleStat.ATTACK))
		assertEquals(-2, simple.statStage(BattleStat.DEFENSE))
	}

	@Test
	fun `opponent stat drop triggers defiant once for the skill`() {
		val skill = damagingSkill(
			skillId = 821,
			statStageEffects = listOf(
				BattleStatStageEffect(BattleStat.DEFENSE, BattleEffectTarget.TARGET, -1, 100),
				BattleStatStageEffect(BattleStat.SPEED, BattleEffectTarget.TARGET, -1, 100),
			),
		)
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(
					initialState(
						first = participant("attacker", 100, skill = skill),
						second = participant(
							"holder",
							50,
							abilityEffects = listOf(
								BattleAbilityEffect.OpponentStatReductionReactiveBoost(BattleStat.ATTACK, 2),
							),
						),
					),
				),
				listOf(BattleAction.UseSkill("attacker", skill.skillId, "holder")),
				ScriptedBattleRandom(listOf(1, 15)),
			)
		}

		assertEquals(-1, resolved.participant("holder")?.statStage(BattleStat.DEFENSE))
		assertEquals(-1, resolved.participant("holder")?.statStage(BattleStat.SPEED))
		assertEquals(2, resolved.participant("holder")?.statStage(BattleStat.ATTACK))
	}
}
