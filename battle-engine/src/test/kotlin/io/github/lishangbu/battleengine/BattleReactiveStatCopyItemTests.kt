package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BattleReactiveStatCopyItemTests {
	@Test
	fun `adrenaline orb raises speed after opposing switch in ability lowers a stat`() {
		val state = BattleEngine().start(
			initialState(
				first = participant(
					"intimidator",
					speed = 100,
					abilityEffects = listOf(BattleAbilityEffect.SwitchInStatStageChange(BattleStat.ATTACK, -1)),
				),
				second = participant(
					"holder",
					speed = 50,
					itemId = 883,
					itemEffects = listOf(BattleItemEffect.AbilityStatReductionReactiveBoost(BattleStat.SPEED, 1)),
				),
			),
		)

		assertEquals(-1, state.participant("holder")?.statStage(BattleStat.ATTACK))
		assertEquals(1, state.participant("holder")?.statStage(BattleStat.SPEED))
		assertNull(state.participant("holder")?.itemId)
	}

	@Test
	fun `mirror herb copies all opponent positive stat changes once`() {
		val skill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statStageEffects = listOf(
				BattleStatStageEffect(BattleStat.ATTACK, BattleEffectTarget.USER, 2, 100),
				BattleStatStageEffect(BattleStat.SPEED, BattleEffectTarget.USER, 1, 100),
			),
		)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("booster", speed = 100, skill = skill),
					second = participant(
						"holder",
						speed = 50,
						itemId = 1699,
						itemEffects = listOf(BattleItemEffect.OpponentPositiveStatStageCopy()),
					),
				),
			),
			listOf(BattleAction.UseSkill("booster", skill.skillId, "holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(2, resolved.participant("holder")?.statStage(BattleStat.ATTACK))
		assertEquals(1, resolved.participant("holder")?.statStage(BattleStat.SPEED))
		assertNull(resolved.participant("holder")?.itemId)
	}
}
