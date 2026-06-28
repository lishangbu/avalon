package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证天气在回合末产生的战斗状态副作用。
 *
 * 场景类型：回合末天气 fixture。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代沙暴会在回合末伤害非岩、非地面、非钢属性的上场成员；
 * 岩/地面/钢属性成员天然免疫这部分固定伤害。
 * 验证重点：沙暴伤害在回复前写入事件流，并且不会误伤天然免疫属性成员。
 */
class BattleWeatherEffectTests {
	private val engine = BattleEngine()

	@Test
	fun `sandstorm damages only non immune active participants at end turn`() {
		val fixture = publicBattleRuleFixture(
			name = "sandstorm-damages-only-non-immune-active-participants",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Sandstorm_(weather_condition)",
			),
			inputSummary = "沙暴环境下，普通属性和岩属性当前上场成员都保持可战斗。",
			expectedSummary = "只有普通属性成员受到最大 HP 1/16 的沙暴伤害，岩属性成员不受影响。",
		)
		val state = engine.start(
			initialState(
				first = participant("ordinary", speed = 100, elementId = 1),
				second = participant("rock", speed = 50, elementId = 6),
				environment = BattleEnvironment(weather = BattleWeather.SANDSTORM),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("sandstorm-damages-only-non-immune-active-participants")
		assertEquals(94, resolved.participant("ordinary")?.currentHp)
		assertEquals(100, resolved.participant("rock")?.currentHp)
		val event = resolved.events.filterIsInstance<BattleEvent.WeatherDamageApplied>().single()
		assertEquals("ordinary", event.actorId)
		assertEquals(BattleWeather.SANDSTORM, event.weather)
		assertEquals(6, event.amount)
	}
}
