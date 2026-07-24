package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
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

	@Test
	fun `neutralizing gas rolls forecast back and reapplies it after leaving`() {
		val normal = form(351, setOf(1))
		val sunny = form(10013, setOf(10))
		val effect = forecastEffect()
		val castform = forecastParticipant(normal, sunny, effect)
		val gas = participant("gas", 100, abilityEffects = listOf(BattleAbilityEffect.FieldAbilitySuppression()))
		val reserve = participant("reserve", 80)
		val engine = BattleEngine()
		val started = engine.start(
			initialState(
				first = gas,
				second = castform,
				firstBench = listOf(reserve),
				environment = BattleEnvironment(weather = BattleWeather.SUN, weatherTurnsRemaining = 5),
			),
		)

		assertEquals(normal.creatureId, started.participant(castform.actorId)?.creatureId)

		val restored = engine.resolveTurn(
			started,
			listOf(BattleAction.SwitchParticipant(gas.actorId, reserve.actorId)),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(sunny.creatureId, restored.participant(castform.actorId)?.creatureId)
	}

	@Test
	fun `weather suppression keeps forecast in its normal form`() {
		val normal = form(351, setOf(1))
		val sunny = form(10013, setOf(10))
		val castform = forecastParticipant(normal, sunny, forecastEffect())
		val reserve = participant("reserve", 80)
		val suppressor = participant(
			"suppressor",
			100,
			abilityEffects = listOf(BattleAbilityEffect.WeatherEffectSuppression()),
		)

		val started = BattleEngine().start(
			initialState(
				first = suppressor,
				second = castform,
				firstBench = listOf(reserve),
				environment = BattleEnvironment(weather = BattleWeather.SUN, weatherTurnsRemaining = 5),
			),
		)

		assertEquals(normal.creatureId, started.participant(castform.actorId)?.creatureId)

		val restored = BattleEngine().resolveTurn(
			started,
			listOf(BattleAction.SwitchParticipant(suppressor.actorId, reserve.actorId)),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(sunny.creatureId, restored.participant(castform.actorId)?.creatureId)
	}

	private fun forecastEffect() = BattleAbilityEffect.WeatherFormChange(
		defaultFormCode = "castform",
		formCodesByWeather = mapOf(BattleWeather.SUN to "castform-sunny"),
	)

	private fun forecastParticipant(
		normal: BattleFormProfile,
		sunny: BattleFormProfile,
		effect: BattleAbilityEffect.WeatherFormChange,
	) = participant(
		"castform",
		100,
		creatureId = normal.creatureId,
		abilityEffects = listOf(effect),
		battleFormProfiles = mapOf("castform" to normal, "castform-sunny" to sunny),
	)

	private fun form(creatureId: Long, elementIds: Set<Long>) = BattleFormProfile(
		creatureId, 100, 100, 100, 100, 100, 70, 8, elementIds,
	)
}
