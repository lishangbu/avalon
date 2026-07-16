package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleWeatherSuppressionAbilityTests {
	@Test
	fun `weather suppression keeps rain but disables weather speed effects`() {
		val suppressorSkill = damagingSkill(skillId = 901)
		val weatherSkill = damagingSkill(skillId = 902)
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(
					initialState(
						first = participant(
							"suppressor",
							100,
							skill = suppressorSkill,
							abilityEffects = listOf(BattleAbilityEffect.WeatherEffectSuppression()),
						),
						second = participant(
							"weather-user",
							60,
							skill = weatherSkill,
							abilityEffects = listOf(BattleAbilityEffect.WeatherSpeedMultiplier(BattleWeather.RAIN, 2.0)),
						),
						environment = BattleEnvironment(weather = BattleWeather.RAIN, weatherTurnsRemaining = 5),
					),
				),
				listOf(
					BattleAction.UseSkill("suppressor", suppressorSkill.skillId, "weather-user"),
					BattleAction.UseSkill("weather-user", weatherSkill.skillId, "suppressor"),
				),
				ScriptedBattleRandom(listOf(1, 15, 1, 15)),
			)
		}

		assertEquals("suppressor", resolved.events.filterIsInstance<BattleEvent.SkillUsed>().first().actorId)
		assertEquals(BattleWeather.RAIN, resolved.environment.weather)
	}
}
