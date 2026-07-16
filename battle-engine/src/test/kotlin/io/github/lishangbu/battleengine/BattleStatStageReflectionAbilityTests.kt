package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleStatStageReflectionAbilityTests {
	@Test
	fun `mirror armor reflects a switch in stat reduction back to its source`() {
		val resolved = BattleEngine().start(
			initialState(
				first = participant(
					"intimidator",
					100,
					abilityEffects = listOf(
						BattleAbilityEffect.SwitchInStatStageChange(BattleStat.ATTACK, -1),
					),
				),
				second = participant(
					"mirror-holder",
					50,
					abilityEffects = listOf(BattleAbilityEffect.OpponentStatStageReductionReflection()),
				),
			),
		)

		assertEquals(-1, resolved.participant("intimidator")?.statStage(BattleStat.ATTACK))
		assertEquals(0, resolved.participant("mirror-holder")?.statStage(BattleStat.ATTACK))
	}

	@Test
	fun `mirror armor reflects an opponents stat reduction back to its source`() {
		val skill = damagingSkill(
			skillId = 4200,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statStageEffects = listOf(
				BattleStatStageEffect(BattleStat.ATTACK, BattleEffectTarget.TARGET, -1, 100),
			),
		)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill),
					second = participant(
						"mirror-holder",
						50,
						abilityEffects = listOf(BattleAbilityEffect.OpponentStatStageReductionReflection()),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "mirror-holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(-1, resolved.participant("attacker")?.statStage(BattleStat.ATTACK))
		assertEquals(0, resolved.participant("mirror-holder")?.statStage(BattleStat.ATTACK))
	}
}
