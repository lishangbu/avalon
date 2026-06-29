package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证技能在指定天气下覆盖本次结算属性的现代规则。
 *
 * 场景类型：天气影响技能属性 fixture。
 * 参考来源类型：公开成熟模拟器技能资料。气象球在晴天、雨天、沙暴和雪天分别变为火、水、岩石和冰属性，
 * 并在非无天气下把基础威力翻倍。属性变化不是展示文案，它会影响属性一致加成、天气火水伤害倍率、属性克制、
 * 属性吸收特性、抗性道具、低体力属性增伤和火属性伤害解冻等后续判断。
 */
class BattleWeatherElementOverrideTests {
	private val engine = BattleEngine()

	@Test
	fun `weather element override participates in stab weather and effectiveness damage`() {
		val fixture = publicBattleRuleFixture(
			name = "weather-element-override-participates-in-stab-weather-and-effectiveness",
			sourceUrls = listOf("https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts"),
			inputSummary = "水属性使用者在雨天使用基础一般属性 50 威力技能；该技能在雨天覆盖为水属性并威力翻倍。",
			expectedSummary = "本次技能按水属性 100 威力结算，获得水属性一致加成和雨天水伤害 1.5 倍，最终造成 103 点伤害。",
		)
		val weatherBall = weatherBallLikeSkill()
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, elementId = 11, skill = weatherBall),
				second = participant("defender", speed = 50).copy(maxHp = 200, currentHp = 200),
				environment = BattleEnvironment(weather = BattleWeather.RAIN),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 311, targetActorId = "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()

		fixture.assertNamed("weather-element-override-participates-in-stab-weather-and-effectiveness")
		assertEquals(103, damage.amount)
		assertEquals(97, resolved.participant("defender")?.currentHp)
	}

	@Test
	fun `weather element override can be absorbed by matching element ability`() {
		val fixture = publicBattleRuleFixture(
			name = "weather-element-override-can-be-absorbed-by-matching-element-ability",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/abilities.ts",
			),
			inputSummary = "目标拥有吸收水属性技能并回复的特性，攻击方在雨天使用基础一般属性但雨天覆盖为水属性的技能。",
			expectedSummary = "技能在命中前视为水属性，被目标特性吸收并阻止后续伤害；满 HP 目标不会溢出回复。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = weatherBallLikeSkill()),
				second = participant(
					"absorber",
					speed = 50,
					abilityId = 11,
					abilityEffects = listOf(BattleAbilityEffect.ElementSkillAbsorbHeal(elementId = 11)),
				),
				environment = BattleEnvironment(weather = BattleWeather.RAIN),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 311, targetActorId = "absorber")),
			ScriptedBattleRandom(listOf(1)),
		)
		val absorbed = resolved.events.filterIsInstance<BattleEvent.SkillAbsorbedByAbility>().single()

		fixture.assertNamed("weather-element-override-can-be-absorbed-by-matching-element-ability")
		assertEquals(11, absorbed.elementId)
		assertEquals(0, absorbed.healAmount)
		assertEquals(100, resolved.participant("absorber")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
	}

	@Test
	fun `weather element override to fire thaws frozen target after damage`() {
		val fixture = publicBattleRuleFixture(
			name = "weather-element-override-to-fire-thaws-frozen-target",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/moves.ts",
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
			),
			inputSummary = "目标处于冰冻，攻击方在晴天使用基础一般属性但晴天覆盖为火属性的技能并造成伤害。",
			expectedSummary = "本次技能按火属性伤害命中，目标在伤害后仍可战斗，因此冰冻状态被解除。",
		)
		val state = engine.start(
			initialState(
				first = participant("attacker", speed = 100, skill = weatherBallLikeSkill()),
				second = participant("frozen-target", speed = 50)
					.copy(currentHp = 100, majorStatus = BattleMajorStatus.FREEZE),
				environment = BattleEnvironment(weather = BattleWeather.SUN),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", skillId = 311, targetActorId = "frozen-target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val cleared = resolved.events.filterIsInstance<BattleEvent.StatusCleared>().single()

		fixture.assertNamed("weather-element-override-to-fire-thaws-frozen-target")
		assertEquals(BattleMajorStatus.FREEZE, cleared.status)
		assertEquals(null, resolved.participant("frozen-target")?.majorStatus)
		assertEquals(69, resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount)
	}

	private fun weatherBallLikeSkill() =
		damagingSkill(
			skillId = 311,
			name = "气象球测试",
			elementId = 1,
			power = 50,
			powerMultipliersByWeather = mapOf(
				BattleWeather.SUN to 2.0,
				BattleWeather.RAIN to 2.0,
				BattleWeather.SANDSTORM to 2.0,
				BattleWeather.SNOW to 2.0,
			),
			elementOverridesByWeather = mapOf(
				BattleWeather.SUN to 10,
				BattleWeather.RAIN to 11,
				BattleWeather.SANDSTORM to 6,
				BattleWeather.SNOW to 15,
			),
		)
}
