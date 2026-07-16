package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleItemEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleSwitchRestrictionAbilityTests {
	@Test
	fun `arena trap blocks grounded opponent switching and shed shell bypasses it`() {
		val trapper = participant(
			"trapper",
			50,
			abilityEffects = listOf(BattleAbilityEffect.OpponentSwitchRestriction(requiresGroundedTarget = true)),
		)
		val actor = participant("actor", 100)
		val reserve = participant("reserve", 80)
		val state = BattleEngine().start(
			initialState(first = actor, firstBench = listOf(reserve), second = trapper),
		)
		val blocked = BattleActionValidator().validate(
			state,
			listOf(BattleAction.SwitchParticipant("actor", "reserve")),
		)
		val shedShellState = state.replaceParticipant(
			actor.copy(itemId = 1, itemEffects = listOf(BattleItemEffect.SwitchRestrictionImmunity())),
		)

		assertEquals(listOf("ability-prevents-switch"), blocked.map { it.code })
		assertTrue(
			BattleActionValidator().validate(
				shedShellState,
				listOf(BattleAction.SwitchParticipant("actor", "reserve")),
			).isEmpty(),
		)
	}
}
