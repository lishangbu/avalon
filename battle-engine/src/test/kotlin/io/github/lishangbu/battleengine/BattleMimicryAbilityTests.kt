package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证拟态随场地、特性压制和离场正确改变属性身份。 */
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

	@Test
	fun `neutralizing gas rolls mimicry back and reapplies it after leaving`() {
		val gas = participant("gas", 100, abilityEffects = listOf(BattleAbilityEffect.FieldAbilitySuppression()))
		val reserve = participant("reserve", 80)
		val holder = participant("holder", 50, abilityEffects = listOf(effect))
		val engine = BattleEngine()
		val started = engine.start(
			initialState(
				first = gas,
				second = holder,
				firstBench = listOf(reserve),
				environment = BattleEnvironment(terrain = BattleTerrain.ELECTRIC),
			),
		)

		assertEquals(setOf(1L), started.participant(holder.actorId)?.elementIds)

		val restored = engine.resolveTurn(
			started,
			listOf(BattleAction.SwitchParticipant(gas.actorId, reserve.actorId)),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(setOf(13L), restored.participant(holder.actorId)?.elementIds)
	}
}
