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
	fun `species stat and element items only boost matching holder`() {
		val baseAttacker = participant("attacker", speed = 100).copy(creatureId = 25)
		val defender = participant("defender", speed = 50)
		val skill = damagingSkill(elementId = 13)
		val normal = damage(baseAttacker, defender, skill)
		val lightBall = damage(
			baseAttacker.copy(itemId = 213, itemEffects = listOf(BattleItemEffect.CreatureStatMultiplier(setOf(25), setOf(BattleStat.ATTACK, BattleStat.SPECIAL_ATTACK), 2.0))),
			defender,
			skill,
		)
		val wrongSpecies = damage(
			baseAttacker.copy(creatureId = 26, itemId = 213, itemEffects = listOf(BattleItemEffect.CreatureStatMultiplier(setOf(25), setOf(BattleStat.ATTACK), 2.0))),
			defender,
			skill,
		)

		assertTrue(lightBall > normal)
		assertTrue(wrongSpecies == normal)
	}

	private fun damage(attacker: io.github.lishangbu.battleengine.model.BattleParticipant, defender: io.github.lishangbu.battleengine.model.BattleParticipant, skill: io.github.lishangbu.battleengine.model.BattleSkillSlot): Int =
		calculator.calculate(BattleDamageRequest(attacker, defender, skill, neutralRules(), randomPercent = 100)).amount
}
