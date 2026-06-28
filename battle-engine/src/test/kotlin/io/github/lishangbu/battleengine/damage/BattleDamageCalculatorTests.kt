package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.damagingSkill
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.model.ElementEffectivenessChart
import io.github.lishangbu.battleengine.neutralRules
import io.github.lishangbu.battleengine.participant
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证第一版普通伤害公式。
 *
 * 场景类型：公式级 fixture。
 * 参考来源类型：公开主系列普通伤害公式的通用结构；本测试只覆盖可独立确认的等级、威力、攻防、随机、
 * 属性一致加成和属性克制取整，不声称已经覆盖击中要害、天气、状态、道具或特性。
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

		assertEquals(1.5, fireResult.weatherMultiplier)
		assertEquals(42, fireResult.amount)
		assertEquals(0.5, waterResult.weatherMultiplier)
		assertEquals(14, waterResult.amount)
	}

	@Test
	fun `rain boosts water damage and weakens fire damage`() {
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

		assertEquals(1.5, waterResult.weatherMultiplier)
		assertEquals(42, waterResult.amount)
		assertEquals(0.5, fireResult.weatherMultiplier)
		assertEquals(14, fireResult.amount)
	}
}
