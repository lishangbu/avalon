package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleQuickDrawAbilityTests {
	@Test
	fun `quick draw can move a slower actor to the early action bracket`() {
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant(
						"quick-draw-user",
						50,
						abilityEffects = listOf(BattleAbilityEffect.RandomActionOrderBoost(30)),
					),
					second = participant("opponent", 100),
				),
			),
			listOf(
				BattleAction.UseSkill("quick-draw-user", 1, "opponent"),
				BattleAction.UseSkill("opponent", 1, "quick-draw-user"),
			),
			ScriptedBattleRandom(listOf(0, 1, 15, 1, 15)),
		)

		assertEquals(
			listOf("quick-draw-user", "opponent"),
			resolved.events.filterIsInstance<BattleEvent.SkillUsed>().map { it.actorId },
		)
	}
}
