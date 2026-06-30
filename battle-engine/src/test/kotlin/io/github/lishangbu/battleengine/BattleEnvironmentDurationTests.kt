package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证天气和场地持续回合推进。
 *
 * 场景类型：回合末环境状态 fixture。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。天气与场地在回合末推进剩余回合；
 * 剩余回合归零时环境结束并产生可复盘事件，永久环境或未指定持续回合的 fixture 不会被递减。
 * 验证重点：环境结束事件发生在 `TurnEnded` 前，并且只在持续回合耗尽时出现。
 */
class BattleEnvironmentDurationTests {
	private val engine = BattleEngine()

	@Test
	fun `weather duration decrements and emits end event when exhausted`() {
		val fixture = publicBattleRuleFixture(
			name = "weather-duration-decrements-and-ends",
			inputSummary = "初始晴天剩余 2 回合，连续结算两个空回合。",
			expectedSummary = "第一回合后剩余 1 回合且不产生结束事件；第二回合后天气恢复为无并产生结束事件。",
		)
		val state = engine.start(
			initialState(
				environment = BattleEnvironment(weather = BattleWeather.SUN, weatherTurnsRemaining = 2),
			),
		)

		val afterFirst = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))
		val afterSecond = engine.resolveTurn(afterFirst, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("weather-duration-decrements-and-ends")
		assertEquals(BattleWeather.SUN, afterFirst.environment.weather)
		assertEquals(1, afterFirst.environment.weatherTurnsRemaining)
		assertEquals(emptyList(), afterFirst.events.filterIsInstance<BattleEvent.WeatherEnded>())
		assertEquals(BattleWeather.NONE, afterSecond.environment.weather)
		assertEquals(null, afterSecond.environment.weatherTurnsRemaining)
		val ended = afterSecond.events.filterIsInstance<BattleEvent.WeatherEnded>().single()
		assertEquals(BattleWeather.SUN, ended.weather)
		assertEquals(BattleEvent.TurnEnded::class, afterSecond.events.last()::class)
	}

	@Test
	fun `terrain duration emits end event when exhausted`() {
		val fixture = publicBattleRuleFixture(
			name = "terrain-duration-ends-at-turn-end",
			inputSummary = "初始电气场地剩余 1 回合，结算一个空回合。",
			expectedSummary = "回合末场地恢复为无并产生场地结束事件。",
		)
		val state = engine.start(
			initialState(
				environment = BattleEnvironment(terrain = BattleTerrain.ELECTRIC, terrainTurnsRemaining = 1),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("terrain-duration-ends-at-turn-end")
		assertEquals(BattleTerrain.NONE, resolved.environment.terrain)
		assertEquals(null, resolved.environment.terrainTurnsRemaining)
		val ended = resolved.events.filterIsInstance<BattleEvent.TerrainEnded>().single()
		assertEquals(BattleTerrain.ELECTRIC, ended.terrain)
		assertEquals(BattleEvent.TurnEnded::class, resolved.events.last()::class)
	}
}
