package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleEnvironmentHighestStatAbilityTests {
	@Test
	fun `protosynthesis strengthens the highest attack stat in sun`() {
		val skill = damagingSkill(skillId = 1100, power = 40)
		val holder = participant(
			"holder",
			100,
			skill = skill,
			abilityEffects = listOf(
				BattleAbilityEffect.EnvironmentHighestStatMultiplier(requiredWeather = BattleWeather.SUN),
			),
		).copy(attack = 150)
		val engine = BattleEngine()
		val sunny = engine.resolveTurn(
			engine.start(
				initialState(
					first = holder,
					second = participant("target", 50),
					environment = BattleEnvironment(weather = BattleWeather.SUN, weatherTurnsRemaining = 5),
				),
			),
			listOf(BattleAction.UseSkill("holder", skill.skillId, "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val neutral = engine.resolveTurn(
			engine.start(initialState(first = holder, second = participant("target", 50))),
			listOf(BattleAction.UseSkill("holder", skill.skillId, "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		val sunnyDamage = sunny.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount
		val neutralDamage = neutral.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount
		assertTrue(sunnyDamage > neutralDamage)
	}

	@Test
	fun `quark drive strengthens the highest speed stat on electric terrain`() {
		val skill = damagingSkill(skillId = 1101, power = null, fixedDamage = BattleFixedDamage.FixedAmount(1))
		val holder = participant(
			"holder",
			120,
			skill = skill,
			abilityEffects = listOf(
				BattleAbilityEffect.EnvironmentHighestStatMultiplier(requiredTerrain = BattleTerrain.ELECTRIC),
			),
		)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = holder,
					second = participant("opponent", 150, skill = skill),
					environment = BattleEnvironment(terrain = BattleTerrain.ELECTRIC, terrainTurnsRemaining = 5),
				),
			),
			listOf(
				BattleAction.UseSkill("holder", skill.skillId, "opponent"),
				BattleAction.UseSkill("opponent", skill.skillId, "holder"),
			),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals("holder", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().first().actorId)
	}
}
