package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证幻觉在入场时伪装为队尾可战斗成员，并在本体受到伤害后解除。 */
class BattleIllusionAbilityTests {
	private val engine = BattleEngine()

	@Test
	fun `illusion disguises as the last healthy ally and breaks after direct damage`() {
		val illusion = participant(
			"illusion",
			100,
			abilityEffects = listOf(BattleAbilityEffect.SwitchInDisguiseAsLastHealthyAlly()),
		).copy(creatureId = 571)
		val lastHealthyAlly = participant("last-healthy", 40).copy(creatureId = 25)
		val state = engine.start(initialState(
			first = illusion,
			firstBench = listOf(lastHealthyAlly),
			second = participant("opponent", 80, skill = damagingSkill()),
		))

		assertEquals(25, state.participant("illusion")?.apparentCreatureId)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("opponent", 1, "illusion")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(null, resolved.participant("illusion")?.apparentCreatureId)
	}
}
