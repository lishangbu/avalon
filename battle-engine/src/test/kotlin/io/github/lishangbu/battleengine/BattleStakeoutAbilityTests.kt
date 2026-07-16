package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertTrue

class BattleStakeoutAbilityTests {
	@Test
	fun `stakeout doubles damage against a target switched in this turn`() {
		val skill = damagingSkill(skillId = 981, power = 40)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant(
						"attacker",
						100,
						skill = skill,
						abilityEffects = listOf(BattleAbilityEffect.SwitchedInTargetDamageBoost(2.0)),
					),
					second = participant("starter", 50),
					secondBench = listOf(participant("reserve", 40)),
				),
			),
			listOf(
				BattleAction.SwitchParticipant("starter", "reserve"),
				BattleAction.UseSkill("attacker", skill.skillId, "starter"),
			),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertTrue(resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount > 28)
	}
}
