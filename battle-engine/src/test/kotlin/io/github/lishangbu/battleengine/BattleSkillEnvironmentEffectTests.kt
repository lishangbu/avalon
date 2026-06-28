package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleSkillEnvironmentEffect
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证技能成功后改写全场环境的效果。
 *
 * 场景类型：技能环境效果 fixture。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代普通天气技能成功后设置对应天气，并按普通持续回合计时。
 * 验证重点：技能成功后环境快照更新，事件流中写入天气开始事实，后续天气规则可以读取该环境。
 */
class BattleSkillEnvironmentEffectTests {
	private val engine = BattleEngine()

	@Test
	fun `status skill starts weather for five turns`() {
		val fixture = publicBattleRuleFixture(
			name = "status-skill-starts-weather-for-five-turns",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Rain_Dance_(move)",
				"https://bulbapedia.bulbagarden.net/wiki/Snowscape_(move)",
			),
			inputSummary = "使用者成功使用设置天气的变化技能，当前场上没有天气。",
			expectedSummary = "全场天气变为目标天气，剩余回合为 5，并产生天气开始事件。",
		)
		val skill = damagingSkill(
			name = "天气设置测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			affectedByProtect = false,
			environmentEffects = listOf(BattleSkillEnvironmentEffect.SetWeather(BattleWeather.RAIN, turnsRemaining = 5)),
		)
		val state = engine.start(
			initialState(
				first = participant("weather-user", speed = 100, skill = skill),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("weather-user", skillId = 1, targetActorId = "weather-user")),
			ScriptedBattleRandom(emptyList()),
		)

		fixture.assertNamed("status-skill-starts-weather-for-five-turns")
		assertEquals(BattleWeather.RAIN, resolved.environment.weather)
		assertEquals(4, resolved.environment.weatherTurnsRemaining)
		val event = resolved.events.filterIsInstance<BattleEvent.WeatherStarted>().single()
		assertEquals("weather-user", event.actorId)
		assertEquals(BattleWeather.RAIN, event.weather)
		assertEquals(5, event.turnsRemaining)
	}
}
