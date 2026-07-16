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

class BattleOpponentStatStageIncreaseCopyAbilityTests {
	@Test
	fun `opportunist copies an opponents positive stat stage change`() {
		val boostSkill = damagingSkill(
			skillId = 1091,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statStageEffects = listOf(
				BattleStatStageEffect(
					stat = BattleStat.ATTACK,
					target = BattleEffectTarget.USER,
					stageDelta = 2,
					chancePercent = 100,
				),
			),
		)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("booster", 100, skill = boostSkill),
					second = participant(
						"opportunist-holder",
						50,
						abilityEffects = listOf(BattleAbilityEffect.OpponentStatStageIncreaseCopy()),
					),
				),
			),
			listOf(BattleAction.UseSkill("booster", boostSkill.skillId, "opportunist-holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(2, resolved.participant("booster")?.statStage(BattleStat.ATTACK))
		assertEquals(2, resolved.participant("opportunist-holder")?.statStage(BattleStat.ATTACK))
	}
}
