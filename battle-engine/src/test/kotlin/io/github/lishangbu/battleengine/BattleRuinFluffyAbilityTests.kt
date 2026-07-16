package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertTrue

class BattleRuinFluffyAbilityTests {
	@Test
	fun `ruin abilities reduce the opposing formula stat`() {
		val skill = damagingSkill(skillId = 851, power = 80)
		val neutral = damage(skill)
		val tablets = damage(
			skill,
			defenderEffects = listOf(BattleAbilityEffect.OpponentAttackingStatMultiplier(BattleStat.ATTACK, 0.75)),
		)
		val sword = damage(
			skill,
			attackerEffects = listOf(BattleAbilityEffect.OpponentDefendingStatMultiplier(BattleStat.DEFENSE, 0.75)),
		)

		assertTrue(tablets < neutral)
		assertTrue(sword > neutral)
	}

	@Test
	fun `fluffy halves contact damage and doubles fire damage`() {
		val contact = damagingSkill(skillId = 852, power = 80, makesContact = true)
		val fire = damagingSkill(skillId = 853, power = 80, elementId = 10, damageClass = BattleDamageClass.SPECIAL)
		val fluffyContact = damage(
			contact,
			defenderEffects = listOf(BattleAbilityEffect.ContactBasedSkillDamageReduction(0.5)),
		)
		val fluffyFire = damage(
			fire,
			defenderEffects = listOf(BattleAbilityEffect.ElementSkillDamageReduction(setOf(10), 2.0)),
		)

		assertTrue(fluffyContact < damage(contact))
		assertTrue(fluffyFire > damage(fire))
	}

	private fun damage(
		skill: io.github.lishangbu.battleengine.model.BattleSkillSlot,
		attackerEffects: List<BattleAbilityEffect> = emptyList(),
		defenderEffects: List<BattleAbilityEffect> = emptyList(),
	): Int {
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(
					initialState(
						first = participant("attacker", 100, skill = skill, abilityEffects = attackerEffects),
						second = participant("defender", 50, abilityEffects = defenderEffects),
					),
				),
				listOf(BattleAction.UseSkill("attacker", skill.skillId, "defender")),
				ScriptedBattleRandom(listOf(1, 15)),
			)
		}
		return resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount
	}
}
