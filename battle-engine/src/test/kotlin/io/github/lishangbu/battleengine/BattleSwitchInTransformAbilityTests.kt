package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BattleSwitchInTransformAbilityTests {
	@Test
	fun `imposter copies opponent battle traits and restores its own traits on leaving`() {
		val ownSkill = damagingSkill(skillId = 4300, power = 40)
		val copiedSkill = damagingSkill(skillId = 4301, power = 90).copy(remainingPp = 12, maxPp = 12)
		val imposter = participant(
			"imposter",
			80,
			skill = ownSkill,
			abilityId = 150,
			abilityEffects = listOf(BattleAbilityEffect.SwitchInTransformIntoOpponent()),
		).copy(creatureId = 132, attack = 70, elementIds = setOf(1), originalElementIds = setOf(1))
		val target = participant("target", 120, skill = copiedSkill, abilityId = 22)
			.copy(creatureId = 445, attack = 160, elementIds = setOf(16, 9), originalElementIds = setOf(16, 9))
		val transformed = requireNotNull(
			BattleEngine().start(initialState(first = imposter, second = target)).participant("imposter"),
		)

		assertEquals(445, transformed.creatureId)
		assertEquals(160, transformed.attack)
		assertEquals(setOf(16L, 9L), transformed.elementIds)
		assertEquals(4301, transformed.skillSlots.single().skillId)
		assertEquals(5, transformed.skillSlots.single().remainingPp)
		assertEquals(22, transformed.abilityId)
		assertNotNull(transformed.transformSnapshot)

		val restored = transformed.leaveBattlefield()
		assertEquals(132, restored.creatureId)
		assertEquals(70, restored.attack)
		assertEquals(setOf(1L), restored.elementIds)
		assertEquals(4300, restored.skillSlots.single().skillId)
		assertEquals(150, restored.abilityId)
		assertEquals(null, restored.transformSnapshot)
	}
}
