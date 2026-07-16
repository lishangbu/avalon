package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleSingleTargetSecondHitAbilityTests {
	@Test
	fun `parental bond adds a quarter damage second hit to a single hit move`() {
		val skill = damagingSkill(skillId = 1088, power = 40)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant(
						"parental-bond-user",
						100,
						skill = skill,
						abilityEffects = listOf(BattleAbilityEffect.SingleTargetSecondHit()),
					),
					second = participant("target", 50),
				),
			),
			listOf(BattleAction.UseSkill("parental-bond-user", skill.skillId, "target")),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		assertEquals(listOf(28, 7), resolved.events.filterIsInstance<BattleEvent.DamageApplied>().map { it.amount })
	}
}
