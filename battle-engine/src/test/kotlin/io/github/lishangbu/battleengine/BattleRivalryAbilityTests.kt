package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.damage.BattleDamageRequest
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleGender
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleRivalryAbilityTests {
	private val calculator = BattleDamageCalculator()

	@Test
	fun `rivalry strengthens same gender damage and weakens opposite gender damage`() {
		val skill = damagingSkill()
		val attacker = participant("attacker", 100, skill = skill).copy(
			gender = BattleGender.MALE,
			abilityEffects = listOf(BattleAbilityEffect.TargetGenderDamageMultiplier()),
		)

		val sameGender = calculate(attacker, participant("same", 50).copy(gender = BattleGender.MALE), skill)
		val oppositeGender = calculate(attacker, participant("opposite", 50).copy(gender = BattleGender.FEMALE), skill)
		val genderless = calculate(attacker, participant("genderless", 50), skill)

		assertEquals(1.25, sameGender.abilityMultiplier)
		assertEquals(0.75, oppositeGender.abilityMultiplier)
		assertEquals(1.0, genderless.abilityMultiplier)
	}

	private fun calculate(
		attacker: io.github.lishangbu.battleengine.model.BattleParticipant,
		defender: io.github.lishangbu.battleengine.model.BattleParticipant,
		skill: io.github.lishangbu.battleengine.model.BattleSkillSlot,
	) = calculator.calculate(
		BattleDamageRequest(attacker, defender, skill, neutralRules(), randomPercent = 100),
	)
}
