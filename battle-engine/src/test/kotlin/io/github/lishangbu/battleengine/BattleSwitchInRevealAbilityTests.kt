package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleSwitchInRevealAbilityTests {
	@Test
	fun `frisk reveals the held item of each active opponent`() {
		val resolved = BattleEngine().start(
			initialState(
				first = participant(
					"holder",
					100,
					abilityEffects = listOf(BattleAbilityEffect.SwitchInRevealOpponentHeldItems()),
				),
				second = participant("opponent", 50, itemId = 4100),
			),
		)

		val event = resolved.events.filterIsInstance<BattleEvent.OpponentHeldItemRevealed>().single()
		assertEquals("holder", event.actorId)
		assertEquals("opponent", event.targetActorId)
		assertEquals(4100, event.itemId)
	}

	@Test
	fun `forewarn reveals the opponents highest power skill`() {
		val weak = damagingSkill(skillId = 4101, power = 40)
		val strong = damagingSkill(skillId = 4102, power = 120)
		val opponent = participant("opponent", 50, skill = weak).copy(skillSlots = listOf(weak, strong))
		val resolved = BattleEngine().start(
			initialState(
				first = participant(
					"holder",
					100,
					abilityEffects = listOf(BattleAbilityEffect.SwitchInRevealOpponentHighestPowerSkill()),
				),
				second = opponent,
			),
		)

		val event = resolved.events.filterIsInstance<BattleEvent.OpponentSkillRevealed>().single()
		assertEquals("opponent", event.targetActorId)
		assertEquals(4102, event.skillId)
	}
}
