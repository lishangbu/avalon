package io.github.lishangbu.battleengine.damage

import io.github.lishangbu.battleengine.damagingSkill
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.neutralRules
import io.github.lishangbu.battleengine.participant
import kotlin.test.Test
import kotlin.test.assertTrue

/** 验证限定种族携带道具对攻击、防守和属性伤害的修正边界。 */
class BattleSpeciesItemTests {
	private val calculator = BattleDamageCalculator()

	@Test
	fun `species stat item only boosts matching holder`() {
		val baseAttacker = participant("attacker", speed = 100).copy(creatureId = 25)
		val defender = participant("defender", speed = 50)
		val skill = damagingSkill(elementId = 13)
		val normal = damage(baseAttacker, defender, skill)
		val lightBall = damage(
			baseAttacker.copy(itemId = 213).replaceItemEffects(
				listOf(BattleItemEffect.CreatureStatMultiplier(setOf(25), setOf(BattleStat.ATTACK, BattleStat.SPECIAL_ATTACK), 2.0)),
			),
			defender,
			skill,
		)
		val wrongSpecies = damage(
			baseAttacker.copy(creatureId = 26, itemId = 213).replaceItemEffects(
				listOf(BattleItemEffect.CreatureStatMultiplier(setOf(25), setOf(BattleStat.ATTACK), 2.0)),
			),
			defender,
			skill,
		)

		assertTrue(lightBall > normal)
		assertTrue(wrongSpecies == normal)
	}

	@Test
	fun `species element item only boosts matching holder and skill element`() {
		val attacker = participant("attacker", speed = 100).copy(creatureId = 483)
		val defender = participant("defender", speed = 50)
		val steelSkill = damagingSkill(elementId = 9)
		val fireSkill = damagingSkill(elementId = 10)
		val effect = BattleItemEffect.CreatureElementDamageBoost(
			creatureIds = setOf(483),
			elementIds = setOf(9, 16),
			multiplier = 1.2,
		)
		val normal = damage(attacker, defender, steelSkill)
		val matching = damage(attacker.replaceItemEffects(listOf(effect)), defender, steelSkill)
		val wrongSpecies = damage(attacker.copy(creatureId = 484).replaceItemEffects(listOf(effect)), defender, steelSkill)
		val wrongElement = damage(attacker.replaceItemEffects(listOf(effect)), defender, fireSkill)

		assertTrue(matching > normal)
		assertTrue(wrongSpecies == normal)
		assertTrue(wrongElement == damage(attacker, defender, fireSkill))
	}

	@Test
	fun `species damage item boosts every damaging element for matching holder`() {
		val attacker = participant("attacker", speed = 100).copy(creatureId = 10273)
		val defender = participant("defender", speed = 50)
		val grassSkill = damagingSkill(elementId = 12)
		val fireSkill = damagingSkill(elementId = 10)
		val effect = BattleItemEffect.CreatureDamageBoost(setOf(10273), 1.2)

		assertTrue(
			damage(attacker.replaceItemEffects(listOf(effect)), defender, grassSkill) >
				damage(attacker, defender, grassSkill),
		)
		assertTrue(
			damage(attacker.replaceItemEffects(listOf(effect)), defender, fireSkill) >
				damage(attacker, defender, fireSkill),
		)
		assertTrue(
			damage(attacker.copy(creatureId = 10272).replaceItemEffects(listOf(effect)), defender, grassSkill) ==
				damage(attacker, defender, grassSkill),
		)
	}

	private fun damage(attacker: io.github.lishangbu.battleengine.model.BattleParticipant, defender: io.github.lishangbu.battleengine.model.BattleParticipant, skill: io.github.lishangbu.battleengine.model.BattleSkillSlot): Int =
		calculator.calculate(BattleDamageRequest(attacker, defender, skill, neutralRules(), randomPercent = 100)).amount
}
