package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.damage.BattleDamageRequest
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 验证属性伤害增减与击中要害伤害特性的计算结果。 */
class BattleElementDamageAbilityTests {
	private val calculator = BattleDamageCalculator()

	@Test
	fun `element reduction and boost multiply only matching skill element`() {
		val fire = damagingSkill(elementId = 10)
		val attacker = participant("attacker", 100, skill = fire)
			.replaceAbilityEffects(listOf(BattleAbilityEffect.ElementSkillDamageBoost(setOf(10), 1.5)))
		val defender = participant("defender", 50)
			.replaceAbilityEffects(listOf(BattleAbilityEffect.ElementSkillDamageReduction(setOf(10, 15), 0.5)))
		val result = calculator.calculate(
			BattleDamageRequest(attacker, defender, fire, neutralRules(), randomPercent = 100),
		)
		val neutral = calculator.calculate(
			BattleDamageRequest(
				attacker.replaceAbilityEffects(emptyList()),
				defender.replaceAbilityEffects(emptyList()),
				fire,
				neutralRules(),
				randomPercent = 100,
			),
		)

		assertEquals(0.75, result.abilityMultiplier)
		assertTrue(result.amount < neutral.amount)
	}

	@Test
	fun `critical hit ability adds its own damage multiplier`() {
		val skill = damagingSkill()
		val attacker = participant("attacker", 100, skill = skill)
			.replaceAbilityEffects(listOf(BattleAbilityEffect.CriticalHitDamageBoost(1.5)))
		val result = calculator.calculate(
			BattleDamageRequest(attacker, participant("target", 50), skill, neutralRules(), randomPercent = 100, criticalHit = true),
		)

		assertEquals(1.5, result.abilityMultiplier)
	}
}
