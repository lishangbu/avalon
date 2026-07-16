package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleSkillShapeAbilityTests {
	@Test
	fun `skill link selects maximum hit count without consuming random`() {
		val actor = participant(
			"holder",
			100,
			abilityEffects = listOf(BattleAbilityEffect.MultiHitMaximum()),
		)
		val skill = damagingSkill(minHits = 2, maxHits = 5)

		assertEquals(5, determineHitCount(actor, skill, ScriptedBattleRandom(emptyList())))
	}

	@Test
	fun `technician boosts damage only at or below sixty base power`() {
		val weak = damagingSkill(skillId = 861, power = 60)
		val strong = damagingSkill(skillId = 862, power = 61)
		val effect = listOf(BattleAbilityEffect.BasePowerAtMostDamageBoost(60, 1.5))

		assertTrue(damage(weak, effect) > damage(weak))
		assertEquals(damage(strong), damage(strong, effect))
	}

	private fun damage(
		skill: io.github.lishangbu.battleengine.model.BattleSkillSlot,
		effects: List<BattleAbilityEffect> = emptyList(),
	): Int {
		val resolved = BattleEngine().let { engine ->
			engine.resolveTurn(
				engine.start(
					initialState(
						first = participant("attacker", 100, skill = skill, abilityEffects = effects),
						second = participant("target", 50),
					),
				),
				listOf(BattleAction.UseSkill("attacker", skill.skillId, "target")),
				ScriptedBattleRandom(listOf(1, 15)),
			)
		}
		return resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount
	}
}
