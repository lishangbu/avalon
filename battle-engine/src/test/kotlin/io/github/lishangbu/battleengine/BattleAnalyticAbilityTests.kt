package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertTrue

class BattleAnalyticAbilityTests {
	@Test
	fun `analytic boosts damage when the target has already acted`() {
		val skill = damagingSkill(skillId = 991, power = 40)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant(
						"attacker",
						50,
						skill = skill,
						abilityEffects = listOf(BattleAbilityEffect.TargetAlreadyActedDamageBoost(1.3)),
					),
					second = participant("defender", 100),
				),
			),
			listOf(
				BattleAction.UseSkill("attacker", skill.skillId, "defender"),
				BattleAction.UseSkill("defender", 1, "attacker"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>()
			.single { it.actorId == "attacker" }.amount

		assertTrue(damage > 28)
	}
}
