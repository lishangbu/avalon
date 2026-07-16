package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertTrue

class BattleSupremeOverlordAbilityTests {
	@Test
	fun `supreme overlord boosts damage for fainted allies`() {
		val skill = damagingSkill(skillId = 971, power = 80)
		val neutral = damage(skill, emptyList())
		val boosted = damage(
			skill,
			listOf(BattleAbilityEffect.FaintedAllyDamageBoost(0.1, 5)),
		)

		assertTrue(boosted > neutral)
	}

	private fun damage(skill: io.github.lishangbu.battleengine.model.BattleSkillSlot, effects: List<BattleAbilityEffect>): Int {
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill, abilityEffects = effects),
					firstBench = listOf(
						participant("fainted-one", 40, currentHp = 0),
						participant("fainted-two", 30, currentHp = 0),
					),
					second = participant("defender", 50),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "defender")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		return resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount
	}
}
