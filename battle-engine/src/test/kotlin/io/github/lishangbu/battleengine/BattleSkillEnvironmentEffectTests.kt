package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSkillEnvironmentEffect
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证技能成功后改写全场环境的效果。
 *
 * 场景类型：技能环境效果 场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代普通天气技能成功后设置对应天气，并按普通持续回合计时。
 * 验证重点：技能成功后环境快照更新，事件流中写入天气开始事实，后续天气规则可以读取该环境。
 */
class BattleSkillEnvironmentEffectTests {
	private val engine = BattleEngine()

	@Test
	fun `status skill starts weather for five turns`() {
		val scenario = publicBattleRuleScenario(
			name = "status-skill-starts-weather-for-five-turns",
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

		scenario.assertNamed("status-skill-starts-weather-for-five-turns")
		assertEquals(BattleWeather.RAIN, resolved.environment.weather)
		assertEquals(4, resolved.environment.weatherTurnsRemaining)
		val event = resolved.events.filterIsInstance<BattleEvent.WeatherStarted>().single()
		assertEquals("weather-user", event.actorId)
		assertEquals(BattleWeather.RAIN, event.weather)
		assertEquals(5, event.turnsRemaining)
	}

	@Test
	fun `weather skill fails when same weather is already active`() {
		val scenario = publicBattleRuleScenario(
			name = "weather-skill-fails-when-same-weather-already-active",
			inputSummary = "使用者尝试使用设置下雨天气的变化技能，但场上已经处于下雨天气且还剩 3 回合。",
			expectedSummary = "技能按规则失败，不刷新天气持续回合，不产生新的天气开始事件；回合结束后原天气剩余 2 回合。",
		)
		val skill = damagingSkill(
			name = "重复天气测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			affectedByProtect = false,
			environmentEffects = listOf(BattleSkillEnvironmentEffect.SetWeather(BattleWeather.RAIN, turnsRemaining = 5)),
		)
		val state = engine.start(
			initialState(
				first = participant("weather-user", speed = 100, skill = skill),
				second = participant("observer", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.RAIN, weatherTurnsRemaining = 3),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("weather-user", skillId = 1, targetActorId = "weather-user")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("weather-skill-fails-when-same-weather-already-active")
		assertEquals(BattleWeather.RAIN, resolved.environment.weather)
		assertEquals(2, resolved.environment.weatherTurnsRemaining)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.WeatherStarted>())
		val event = resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single()
		assertEquals("weather-user", event.actorId)
		assertEquals("weather-user", event.targetActorId)
		assertEquals(1, event.skillId)
		assertEquals("weather-already-active", event.reason)
	}

	@Test
	fun `status skill starts terrain for five turns`() {
		val scenario = publicBattleRuleScenario(
			name = "status-skill-starts-terrain-for-five-turns",
			inputSummary = "使用者成功使用设置场地的变化技能，当前全场没有场地。",
			expectedSummary = "全场场地变为目标场地，剩余回合为 5，并产生场地开始事件。",
		)
		val skill = damagingSkill(
			name = "场地设置测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			affectedByProtect = false,
			environmentEffects = listOf(BattleSkillEnvironmentEffect.SetTerrain(BattleTerrain.ELECTRIC, turnsRemaining = 5)),
		)
		val state = engine.start(
			initialState(
				first = participant("terrain-user", speed = 100, skill = skill),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("terrain-user", skillId = 1, targetActorId = "terrain-user")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("status-skill-starts-terrain-for-five-turns")
		assertEquals(BattleTerrain.ELECTRIC, resolved.environment.terrain)
		assertEquals(4, resolved.environment.terrainTurnsRemaining)
		val event = resolved.events.filterIsInstance<BattleEvent.TerrainStarted>().single()
		assertEquals("terrain-user", event.actorId)
		assertEquals(BattleTerrain.ELECTRIC, event.terrain)
		assertEquals(5, event.turnsRemaining)
	}

	@Test
	fun `terrain skill fails when same terrain is already active`() {
		val scenario = publicBattleRuleScenario(
			name = "terrain-skill-fails-when-same-terrain-already-active",
			inputSummary = "使用者尝试使用设置电气场地的变化技能，但场上已经处于电气场地且还剩 3 回合。",
			expectedSummary = "技能按规则失败，不刷新场地持续回合，不产生新的场地开始事件；回合结束后原场地剩余 2 回合。",
		)
		val skill = damagingSkill(
			name = "重复场地测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			affectedByProtect = false,
			environmentEffects = listOf(BattleSkillEnvironmentEffect.SetTerrain(BattleTerrain.ELECTRIC, turnsRemaining = 5)),
		)
		val state = engine.start(
			initialState(
				first = participant("terrain-user", speed = 100, skill = skill),
				second = participant("observer", speed = 50),
				environment = BattleEnvironment(terrain = BattleTerrain.ELECTRIC, terrainTurnsRemaining = 3),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("terrain-user", skillId = 1, targetActorId = "terrain-user")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("terrain-skill-fails-when-same-terrain-already-active")
		assertEquals(BattleTerrain.ELECTRIC, resolved.environment.terrain)
		assertEquals(2, resolved.environment.terrainTurnsRemaining)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.TerrainStarted>())
		val event = resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single()
		assertEquals("terrain-user", event.actorId)
		assertEquals("terrain-user", event.targetActorId)
		assertEquals(1, event.skillId)
		assertEquals("terrain-already-active", event.reason)
	}

	@Test
	fun `weather extending item makes weather skill last eight turns`() {
		val scenario = publicBattleRuleScenario(
			name = "weather-extending-item-makes-weather-skill-last-eight-turns",
			inputSummary = "使用者携带匹配下雨天气的延长道具，成功使用设置下雨的变化技能。",
			expectedSummary = "下雨天气按 8 回合建立；同一回合结束后剩余 7 回合，并产生记录 8 回合的天气开始事件。",
		)
		val skill = damagingSkill(
			name = "天气延长测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			affectedByProtect = false,
			environmentEffects = listOf(BattleSkillEnvironmentEffect.SetWeather(BattleWeather.RAIN, turnsRemaining = 5)),
		)
		val state = engine.start(
			initialState(
				first = participant(
					"weather-user",
					speed = 100,
					skill = skill,
					itemId = 262,
					itemEffects = listOf(
						BattleItemEffect.WeatherDurationExtension(
							weathers = setOf(BattleWeather.RAIN),
							turnsRemaining = 8,
						),
					),
				),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("weather-user", skillId = 1, targetActorId = "weather-user")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("weather-extending-item-makes-weather-skill-last-eight-turns")
		assertEquals(BattleWeather.RAIN, resolved.environment.weather)
		assertEquals(7, resolved.environment.weatherTurnsRemaining)
		val event = resolved.events.filterIsInstance<BattleEvent.WeatherStarted>().single()
		assertEquals("weather-user", event.actorId)
		assertEquals(BattleWeather.RAIN, event.weather)
		assertEquals(8, event.turnsRemaining)
	}

	@Test
	fun `terrain extending item makes terrain skill last eight turns`() {
		val scenario = publicBattleRuleScenario(
			name = "terrain-extending-item-makes-terrain-skill-last-eight-turns",
			inputSummary = "使用者携带场地延长道具，成功使用设置青草场地的变化技能。",
			expectedSummary = "青草场地按 8 回合建立；同一回合结束后剩余 7 回合，并产生记录 8 回合的场地开始事件。",
		)
		val skill = damagingSkill(
			name = "场地延长测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			affectedByProtect = false,
			environmentEffects = listOf(BattleSkillEnvironmentEffect.SetTerrain(BattleTerrain.GRASSY, turnsRemaining = 5)),
		)
		val state = engine.start(
			initialState(
				first = participant(
					"terrain-user",
					speed = 100,
					skill = skill,
					itemId = 896,
					itemEffects = listOf(
						BattleItemEffect.TerrainDurationExtension(
							terrains = setOf(
								BattleTerrain.ELECTRIC,
								BattleTerrain.GRASSY,
								BattleTerrain.MISTY,
								BattleTerrain.PSYCHIC,
							),
							turnsRemaining = 8,
						),
					),
				),
				second = participant("observer", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("terrain-user", skillId = 1, targetActorId = "terrain-user")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("terrain-extending-item-makes-terrain-skill-last-eight-turns")
		assertEquals(BattleTerrain.GRASSY, resolved.environment.terrain)
		assertEquals(7, resolved.environment.terrainTurnsRemaining)
		val event = resolved.events.filterIsInstance<BattleEvent.TerrainStarted>().single()
		assertEquals("terrain-user", event.actorId)
		assertEquals(BattleTerrain.GRASSY, event.terrain)
		assertEquals(8, event.turnsRemaining)
	}
}
