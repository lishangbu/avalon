package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleMimicryAbilityTests {
	private val effect = BattleAbilityEffect.TerrainElementIdentity(
		mapOf(
			BattleTerrain.ELECTRIC to 13L,
			BattleTerrain.GRASSY to 12L,
			BattleTerrain.MISTY to 18L,
			BattleTerrain.PSYCHIC to 14L,
		),
	)

	@Test
	fun `active terrain changes the holders elements and leaving restores the original elements`() {
		val engine = BattleEngine()
		val started = engine.start(
			initialState(
				first = participant("holder", 100, abilityEffects = listOf(effect)),
				second = participant("opponent", 50),
				environment = BattleEnvironment(terrain = BattleTerrain.ELECTRIC),
			),
		)

		val holder = requireNotNull(started.participant("holder"))
		assertEquals(setOf(13L), holder.elementIds)
		assertEquals(setOf(1L), holder.originalElementIds)
		assertEquals(setOf(1L), holder.leaveBattlefield().elementIds)
	}

	@Test
	fun `terrain expiration restores the original elements`() {
		val engine = BattleEngine()
		val started = engine.start(
			initialState(
				first = participant("holder", 100, abilityEffects = listOf(effect)),
				second = participant("opponent", 50),
				environment = BattleEnvironment(
					terrain = BattleTerrain.GRASSY,
					terrainTurnsRemaining = 1,
				),
			),
		)

		val resolved = engine.resolveTurn(started, emptyList(), ScriptedBattleRandom(emptyList()))

		assertEquals(BattleTerrain.NONE, resolved.environment.terrain)
		assertEquals(setOf(1L), resolved.participant("holder")?.elementIds)
		assertEquals(setOf(1L), resolved.participant("holder")?.originalElementIds)
	}
}
