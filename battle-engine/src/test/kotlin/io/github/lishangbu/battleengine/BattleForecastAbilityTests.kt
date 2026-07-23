package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleFormProfile
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证阴晴不定会在天气建立与结束时即时同步形态。 */
class BattleForecastAbilityTests {
	@Test
	fun `forecast enters sunny form and returns to normal when weather expires`() {
		val normal = form(351, setOf(1))
		val sunny = form(10013, setOf(10))
		val rainy = form(10014, setOf(11))
		val snowy = form(10015, setOf(15))
		val effect = BattleAbilityEffect.WeatherFormChange(
			defaultFormCode = "castform",
			formCodesByWeather = mapOf(
				BattleWeather.SUN to "castform-sunny",
				BattleWeather.RAIN to "castform-rainy",
				BattleWeather.SNOW to "castform-snowy",
			),
		)
		val castform = participant(
			"castform",
			100,
			creatureId = normal.creatureId,
			abilityEffects = listOf(effect),
			battleFormProfiles = mapOf(
				"castform" to normal,
				"castform-sunny" to sunny,
				"castform-rainy" to rainy,
				"castform-snowy" to snowy,
			),
		)
		val engine = BattleEngine()
		val started = engine.start(
			initialState(
				castform,
				participant("opponent", 50),
				environment = BattleEnvironment(weather = BattleWeather.SUN, weatherTurnsRemaining = 1),
			),
		)

		val resolved = engine.resolveTurn(started, emptyList(), ScriptedBattleRandom(emptyList()))

		assertEquals(sunny.creatureId, started.participant("castform")?.creatureId)
		assertEquals(sunny.elementIds, started.participant("castform")?.elementIds)
		assertEquals(normal.creatureId, resolved.participant("castform")?.creatureId)
		assertEquals(normal.elementIds, resolved.participant("castform")?.elementIds)
	}

	private fun form(creatureId: Long, elementIds: Set<Long>) = BattleFormProfile(
		creatureId, 100, 100, 100, 100, 100, 70, 8, elementIds,
	)
}
