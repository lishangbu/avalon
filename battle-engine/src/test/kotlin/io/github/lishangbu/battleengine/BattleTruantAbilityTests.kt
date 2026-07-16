package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.SkillPreventionReason
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleTruantAbilityTests {
	@Test
	fun `truant blocks every second active turn`() {
		val engine = BattleEngine()
		val started = engine.start(
			initialState(
				first = participant(
					"truant-user",
					100,
					abilityEffects = listOf(BattleAbilityEffect.EveryOtherActiveTurnActionBlock()),
				),
				second = participant("opponent", 50),
			),
		)
		val first = engine.resolveTurn(
			started,
			listOf(BattleAction.UseSkill("truant-user", 1, "opponent")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val second = engine.resolveTurn(
			first,
			listOf(BattleAction.UseSkill("truant-user", 1, "opponent")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(
			SkillPreventionReason.ABILITY,
			second.events.filterIsInstance<BattleEvent.SkillPrevented>().last().reason,
		)
	}
}
