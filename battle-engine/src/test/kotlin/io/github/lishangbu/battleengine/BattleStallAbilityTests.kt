package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleStallAbilityTests {
	@Test
	fun `stall moves after an opponent in the same priority bracket`() {
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant(
						"stall-user",
						100,
						abilityEffects = listOf(BattleAbilityEffect.ForcedLastActionOrder()),
					),
					second = participant("opponent", 50),
				),
			),
			listOf(
				BattleAction.UseSkill("stall-user", 1, "opponent"),
				BattleAction.UseSkill("opponent", 1, "stall-user"),
			),
			ScriptedBattleRandom(listOf(1, 15, 1, 15)),
		)

		assertEquals(
			listOf("opponent", "stall-user"),
			resolved.events.filterIsInstance<BattleEvent.SkillUsed>().map { it.actorId },
		)
	}
}
