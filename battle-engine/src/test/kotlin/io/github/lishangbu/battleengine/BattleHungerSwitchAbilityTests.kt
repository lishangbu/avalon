package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleFormProfile
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证饱了又饿会在每个完整回合结束时交替切换莫鲁贝可形态。 */
class BattleHungerSwitchAbilityTests {
	@Test
	fun `hunger switch alternates full belly and hangry forms after each turn`() {
		val fullBelly = BattleFormProfile(877, 100, 100, 100, 100, 100, 97, 30, setOf(13, 17))
		val hangry = BattleFormProfile(10187, 100, 100, 100, 100, 100, 97, 30, setOf(13, 17))
		val holder = participant(
			actorId = "morpeko",
			speed = fullBelly.speed,
			creatureId = fullBelly.creatureId,
			battleFormProfiles = mapOf("morpeko-full-belly" to fullBelly, "morpeko-hangry" to hangry),
			abilityEffects = listOf(
				BattleAbilityEffect.EndTurnFormToggle("morpeko-full-belly", "morpeko-hangry"),
			),
		)
		val engine = BattleEngine()
		val started = engine.start(initialState(holder, participant("target", 40)))

		val afterFirstTurn = engine.resolveTurn(started, emptyList(), ScriptedBattleRandom(emptyList()))
		val afterSecondTurn = engine.resolveTurn(afterFirstTurn, emptyList(), ScriptedBattleRandom(emptyList()))

		assertEquals(hangry.creatureId, afterFirstTurn.participant("morpeko")?.creatureId)
		assertEquals(fullBelly.creatureId, afterSecondTurn.participant("morpeko")?.creatureId)
	}
}
