package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSkillEnvironmentEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 验证场地种子在场地生效与成员换入已有场地时提升能力并消费。 */
class BattleTerrainSeedItemTests {
	private val engine = BattleEngine()

	@Test
	fun `new terrain activates matching seeds for every grounded active holder`() {
		val terrainSkill = damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			environmentEffects = listOf(BattleSkillEnvironmentEffect.SetTerrain(BattleTerrain.PSYCHIC)),
		)
		val state = engine.start(
			initialState(
				first = participant("terrain-user", speed = 100, skill = terrainSkill),
				second = participant(
					"psychic-seed-holder",
					speed = 50,
					itemId = 882,
					itemEffects = listOf(
						BattleItemEffect.TerrainActivatedStatStageBoost(
							terrain = BattleTerrain.PSYCHIC,
							stat = BattleStat.SPECIAL_DEFENSE,
							stageDelta = 1,
						),
					),
				),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("terrain-user", skillId = 1, targetActorId = "psychic-seed-holder")),
			ScriptedBattleRandom(emptyList()),
		)

		val holder = requireNotNull(resolved.participant("psychic-seed-holder"))
		assertEquals(1, holder.statStage(BattleStat.SPECIAL_DEFENSE))
		assertNull(holder.itemId)
	}

	@Test
	fun `grounded holder consumes matching seed when battle starts in terrain`() {
		val state = engine.start(
			initialState(
				first = participant(
					"electric-seed-holder",
					speed = 100,
					itemId = 884,
					itemEffects = listOf(
						BattleItemEffect.TerrainActivatedStatStageBoost(
							terrain = BattleTerrain.ELECTRIC,
							stat = BattleStat.DEFENSE,
							stageDelta = 1,
						),
					),
				),
				second = participant("opponent", speed = 50),
				environment = BattleEnvironment(terrain = BattleTerrain.ELECTRIC),
			),
		)

		val holder = requireNotNull(state.participant("electric-seed-holder"))
		assertEquals(1, holder.statStage(BattleStat.DEFENSE))
		assertNull(holder.itemId)
	}

	@Test
	fun `grounded holder consumes matching seed after switching into active terrain`() {
		val seedHolder = participant(
			"grassy-seed-holder",
			speed = 80,
			itemId = 885,
			itemEffects = listOf(
				BattleItemEffect.TerrainActivatedStatStageBoost(
					terrain = BattleTerrain.GRASSY,
					stat = BattleStat.DEFENSE,
					stageDelta = 1,
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("front", speed = 100),
				firstBench = listOf(seedHolder),
				second = participant("opponent", speed = 50),
				environment = BattleEnvironment(terrain = BattleTerrain.GRASSY),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.SwitchParticipant("front", targetActorId = "grassy-seed-holder")),
			ScriptedBattleRandom(emptyList()),
		)

		val holder = requireNotNull(resolved.participant("grassy-seed-holder"))
		assertEquals(1, holder.statStage(BattleStat.DEFENSE))
		assertNull(holder.itemId)
	}
}
