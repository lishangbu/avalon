package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
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

	@Test
	fun `weather speed ability changes skill action order`() {
		val fixture = publicBattleRuleFixture(
			name = "weather-speed-ability-changes-skill-action-order",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Rain",
			),
			inputSummary = "下雨环境下，低速成员拥有雨天速度翻倍特性，高速成员没有天气速度特性。",
			expectedSummary = "低速成员的有效速度翻倍后先行动，事件流中的技能使用顺序随之改变。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"rain-boosted",
					speed = 60,
					abilityEffects = listOf(
						BattleAbilityEffect.WeatherSpeedMultiplier(
							weather = BattleWeather.RAIN,
							multiplier = 2.0,
						),
					),
				),
				second = participant("naturally-fast", speed = 100),
				environment = BattleEnvironment(weather = BattleWeather.RAIN),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("rain-boosted", skillId = 1, targetActorId = "naturally-fast"),
				BattleAction.UseSkill("naturally-fast", skillId = 1, targetActorId = "rain-boosted"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		fixture.assertNamed("weather-speed-ability-changes-skill-action-order")
		assertEquals(
			listOf("rain-boosted", "naturally-fast"),
			resolved.events.filterIsInstance<BattleEvent.SkillUsed>().map { it.actorId },
		)
	}

	@Test
	fun `weather accuracy overrides support sure hit and lowered accuracy`() {
		val fixture = publicBattleRuleFixture(
			name = "weather-accuracy-overrides-support-sure-hit-and-lowered-accuracy",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Weather",
			),
			inputSummary = "同一类天气命中覆盖分别表达雨天下必中、晴天下固定低命中。",
			expectedSummary = "必中覆盖不消费命中随机数；固定低命中覆盖会消费命中随机数并可导致技能未命中。",
		)
		val rainSureHitSkill = damagingSkill(
			name = "雨天命中测试",
			accuracy = 50,
			accuracyOverridesByWeather = mapOf(BattleWeather.RAIN to null),
		)
		val rainState = engine.start(
			initialState(
				first = participant("rain-user", speed = 100, skill = rainSureHitSkill),
				second = participant("rain-target", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.RAIN),
			),
		)
		val rainRandom = ScriptedBattleRandom(listOf(1, 15))

		val rainResolved = engine.resolveTurn(
			rainState,
			listOf(BattleAction.UseSkill("rain-user", skillId = 1, targetActorId = "rain-target")),
			rainRandom,
		)

		val sunLowAccuracySkill = damagingSkill(
			name = "晴天命中测试",
			accuracy = null,
			accuracyOverridesByWeather = mapOf(BattleWeather.SUN to 50),
		)
		val sunState = engine.start(
			initialState(
				first = participant("sun-user", speed = 100, skill = sunLowAccuracySkill),
				second = participant("sun-target", speed = 50),
				environment = BattleEnvironment(weather = BattleWeather.SUN),
			),
		)
		val sunRandom = ScriptedBattleRandom(listOf(99))

		val sunResolved = engine.resolveTurn(
			sunState,
			listOf(BattleAction.UseSkill("sun-user", skillId = 1, targetActorId = "sun-target")),
			sunRandom,
		)

		fixture.assertNamed("weather-accuracy-overrides-support-sure-hit-and-lowered-accuracy")
		assertEquals(72, rainResolved.participant("rain-target")?.currentHp)
		assertEquals(listOf("critical hit for 1", "damage random for 1"), rainRandom.consumedReasons())
		assertEquals(100, sunResolved.participant("sun-target")?.currentHp)
		assertEquals(listOf("accuracy for 1"), sunRandom.consumedReasons())
		assertEquals(100, sunResolved.events.filterIsInstance<BattleEvent.SkillMissed>().single().accuracyRoll)
	}

	@Test
	fun `ability and item immunities block sandstorm damage`() {
		val fixture = publicBattleRuleFixture(
			name = "ability-and-item-immunities-block-sandstorm-damage",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/items.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Sandstorm_(weather_condition)",
			),
			inputSummary = "沙暴环境下，普通属性双方当前上场成员分别拥有天气伤害免疫特性和道具。",
			expectedSummary = "双方都不会受到沙暴回合末固定伤害，也不会产生天气伤害事件。",
		)
		val state = engine.start(
			initialState(
				first = participant(
					"ability-immune",
					speed = 100,
					elementId = 1,
					abilityEffects = listOf(
						BattleAbilityEffect.WeatherDamageImmunity(setOf(BattleWeather.SANDSTORM)),
					),
				),
				second = participant(
					"item-immune",
					speed = 50,
					elementId = 1,
					itemEffects = listOf(
						BattleItemEffect.WeatherDamageImmunity(setOf(BattleWeather.SANDSTORM)),
					),
				),
				environment = BattleEnvironment(weather = BattleWeather.SANDSTORM),
			),
		)

		val resolved = engine.resolveTurn(state, emptyList(), ScriptedBattleRandom(emptyList()))

		fixture.assertNamed("ability-and-item-immunities-block-sandstorm-damage")
		assertEquals(100, resolved.participant("ability-immune")?.currentHp)
		assertEquals(100, resolved.participant("item-immune")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.WeatherDamageApplied>())
	}
}
