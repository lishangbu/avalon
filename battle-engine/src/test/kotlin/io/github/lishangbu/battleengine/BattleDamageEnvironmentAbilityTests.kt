package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleDamageEnvironmentAbilityTests {
	@Test
	fun `damage can start sandstorm and grassy terrain from holder abilities`() {
		val skill = damagingSkill(
			skillId = 841,
			power = null,
			fixedDamage = BattleFixedDamage.FixedAmount(20),
		)
		val holder = participant(
			"holder",
			50,
			abilityEffects = listOf(
				BattleAbilityEffect.ReceivedDamageWeatherChange(BattleWeather.SANDSTORM),
				BattleAbilityEffect.ReceivedDamageTerrainChange(BattleTerrain.GRASSY),
			),
		)
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(initialState(first = participant("attacker", 100, skill = skill), second = holder)),
				listOf(BattleAction.UseSkill("attacker", skill.skillId, "holder")),
				ScriptedBattleRandom(emptyList()),
			)
		}

		assertEquals(BattleWeather.SANDSTORM, resolved.environment.weather)
		assertEquals(BattleTerrain.GRASSY, resolved.environment.terrain)
	}
}
