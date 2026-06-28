package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.damagingSkill
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.neutralRules
import io.github.lishangbu.battleengine.participant
import io.github.lishangbu.battleengine.publicBattleRuleFixture
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证第一版普通伤害公式。
 *
 * 场景类型：公式级 fixture。
 * 参考来源类型：公开主系列普通伤害公式的通用结构；本测试覆盖可独立确认的等级、威力、攻防、随机、
 * 属性一致加成、属性克制、击中要害、天气、状态、道具和特性。
 * 验证重点：基础伤害中间值稳定、随机百分比可控、0 倍免疫不产生最小 1 点伤害。
 */
class BattleDamageCalculatorTests {
	private val calculator = BattleDamageCalculator()

	@Test
	fun `standard damage applies same element bonus and effectiveness`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1),
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = BattleRuleSnapshot(
					elementChart = ElementEffectivenessChart(
						mapOf(1L to mapOf(2L to 2.0)),
					),
				),
				randomPercent = 100,
			),
		)

		assertEquals(19, result.baseDamage)
		assertEquals(1.5, result.sameElementBonus)
		assertEquals(2.0, result.effectiveness)
		assertEquals(57, result.amount)
	}

	@Test
	fun `immunity effectiveness produces zero damage`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1),
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = BattleRuleSnapshot(
					elementChart = ElementEffectivenessChart(
						mapOf(1L to mapOf(2L to 0.0)),
					),
				),
				randomPercent = 100,
			),
		)

		assertEquals(0.0, result.effectiveness)
		assertEquals(0, result.amount)
	}

	@Test
	fun `neutral non matching element damage keeps minimum integer formula`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 2),
				defender = participant("defender", speed = 80, elementId = 3),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		assertEquals(19, result.baseDamage)
		assertEquals(1.0, result.sameElementBonus)
		assertEquals(1.0, result.effectiveness)
		assertEquals(19, result.amount)
	}

	@Test
	fun `multi target damage modifier reduces spread damage`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1),
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
				targetMultiplier = 0.75,
			),
		)

		assertEquals(19, result.baseDamage)
		assertEquals(0.75, result.targetMultiplier)
		assertEquals(21, result.amount)
	}

	@Test
	fun `burn halves physical attacking stat before damage`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1).copy(majorStatus = BattleMajorStatus.BURN),
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		assertEquals(10, result.baseDamage)
		assertEquals(15, result.amount)
	}

	@Test
	fun `attack stage modifies physical damage`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1).copy(
					statStages = mapOf(BattleStat.ATTACK to 2),
				),
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		assertEquals(37, result.baseDamage)
		assertEquals(55, result.amount)
	}

	@Test
	fun `critical hit multiplies damage and ignores unfavorable attack and favorable defense stages`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1).copy(
					statStages = mapOf(BattleStat.ATTACK to -2),
				),
				defender = participant("defender", speed = 80, elementId = 2).copy(
					statStages = mapOf(BattleStat.DEFENSE to 2),
				),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
				criticalHit = true,
			),
		)

		assertEquals(19, result.baseDamage)
		assertEquals(1.5, result.criticalHitMultiplier)
		assertEquals(42, result.amount)
	}

	@Test
	fun `critical hit still keeps burn physical penalty`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 1).copy(majorStatus = BattleMajorStatus.BURN),
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
				criticalHit = true,
			),
		)

		assertEquals(10, result.baseDamage)
		assertEquals(1.5, result.criticalHitMultiplier)
		assertEquals(22, result.amount)
	}

	@Test
	fun `low hp ability and damage boost item multiply damage`() {
		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant(
					"attacker",
					speed = 100,
					currentHp = 30,
					elementId = 1,
					abilityEffects = listOf(BattleAbilityEffect.LowHpElementDamageBoost(elementId = 1)),
					itemEffects = listOf(BattleItemEffect.DamageBoostWithRecoil(multiplier = 1.3, recoilDenominator = 10)),
				),
				defender = participant("defender", speed = 80, elementId = 2),
				skill = damagingSkill(elementId = 1, power = 40),
				rules = neutralRules(),
				randomPercent = 100,
			),
		)

		assertEquals(1.5, result.abilityMultiplier)
		assertEquals(1.3, result.itemMultiplier)
		assertEquals(55, result.amount)
	}

	@Test
	fun `sun boosts fire damage and weakens water damage`() {
		val fixture = publicBattleRuleFixture(
			name = "sun-boosts-fire-and-weakens-water-damage",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Harsh_sunlight",
			),
			inputSummary = "晴天环境下分别计算火属性技能和水属性技能的普通伤害。",
			expectedSummary = "火属性伤害使用 1.5 倍天气倍率，水属性伤害使用 0.5 倍天气倍率。",
		)
		val rules = neutralRules().copy(fireElementId = 10, waterElementId = 11)
		val sun = BattleEnvironment(weather = BattleWeather.SUN)

		val fireResult = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("fire-attacker", speed = 100, elementId = 10),
				defender = participant("defender", speed = 80, elementId = 1),
				skill = damagingSkill(elementId = 10, power = 40),
				rules = rules,
				environment = sun,
				randomPercent = 100,
			),
		)
		val waterResult = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("water-attacker", speed = 100, elementId = 11),
				defender = participant("defender", speed = 80, elementId = 1),
				skill = damagingSkill(elementId = 11, power = 40),
				rules = rules,
				environment = sun,
				randomPercent = 100,
			),
		)

		fixture.assertNamed("sun-boosts-fire-and-weakens-water-damage")
		assertEquals(1.5, fireResult.weatherMultiplier)
		assertEquals(42, fireResult.amount)
		assertEquals(0.5, waterResult.weatherMultiplier)
		assertEquals(14, waterResult.amount)
	}

	@Test
	fun `rain boosts water damage and weakens fire damage`() {
		val fixture = publicBattleRuleFixture(
			name = "rain-boosts-water-and-weakens-fire-damage",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Rain",
			),
			inputSummary = "下雨环境下分别计算水属性技能和火属性技能的普通伤害。",
			expectedSummary = "水属性伤害使用 1.5 倍天气倍率，火属性伤害使用 0.5 倍天气倍率。",
		)
		val rules = neutralRules().copy(fireElementId = 10, waterElementId = 11)
		val rain = BattleEnvironment(weather = BattleWeather.RAIN)

		val waterResult = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("water-attacker", speed = 100, elementId = 11),
				defender = participant("defender", speed = 80, elementId = 1),
				skill = damagingSkill(elementId = 11, power = 40),
				rules = rules,
				environment = rain,
				randomPercent = 100,
			),
		)
		val fireResult = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("fire-attacker", speed = 100, elementId = 10),
				defender = participant("defender", speed = 80, elementId = 1),
				skill = damagingSkill(elementId = 10, power = 40),
				rules = rules,
				environment = rain,
				randomPercent = 100,
			),
		)

		fixture.assertNamed("rain-boosts-water-and-weakens-fire-damage")
		assertEquals(1.5, waterResult.weatherMultiplier)
		assertEquals(42, waterResult.amount)
		assertEquals(0.5, fireResult.weatherMultiplier)
		assertEquals(14, fireResult.amount)
	}

	@Test
	fun `sandstorm boosts rock special defense before special damage`() {
		val fixture = publicBattleRuleFixture(
			name = "sandstorm-boosts-rock-special-defense",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Sandstorm_(weather_condition)",
			),
			inputSummary = "沙暴环境下，普通特殊技能命中岩属性目标。",
			expectedSummary = "目标特防按 1.5 倍参与伤害公式，最终伤害低于普通天气。",
		)

		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 2),
				defender = participant("rock-defender", speed = 80, elementId = 6),
				skill = damagingSkill(elementId = 1, damageClass = BattleDamageClass.SPECIAL, power = 40),
				rules = neutralRules(),
				environment = BattleEnvironment(weather = BattleWeather.SANDSTORM),
				randomPercent = 100,
			),
		)

		fixture.assertNamed("sandstorm-boosts-rock-special-defense")
		assertEquals(13, result.baseDamage)
		assertEquals(13, result.amount)
	}

	@Test
	fun `snow boosts ice physical defense before physical damage`() {
		val fixture = publicBattleRuleFixture(
			name = "snow-boosts-ice-physical-defense",
			sourceUrls = listOf(
				"https://github.com/smogon/pokemon-showdown/blob/master/data/conditions.ts",
				"https://bulbapedia.bulbagarden.net/wiki/Snow",
			),
			inputSummary = "雪景环境下，普通物理技能命中冰属性目标。",
			expectedSummary = "目标物防按 1.5 倍参与伤害公式，最终伤害低于普通天气。",
		)

		val result = calculator.calculate(
			BattleDamageRequest(
				attacker = participant("attacker", speed = 100, elementId = 2),
				defender = participant("ice-defender", speed = 80, elementId = 15),
				skill = damagingSkill(elementId = 1, damageClass = BattleDamageClass.PHYSICAL, power = 40),
				rules = neutralRules(),
				environment = BattleEnvironment(weather = BattleWeather.SNOW),
				randomPercent = 100,
			),
		)

		fixture.assertNamed("snow-boosts-ice-physical-defense")
		assertEquals(13, result.baseDamage)
		assertEquals(13, result.amount)
	}
}
