package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattlePickupAbilityTests {
	@Test
	fun `pickup obtains an opponents item consumed during the current turn`() {
		val skill = damagingSkill(skillId = 5100, power = 40)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant(
						"pickup-holder",
						100,
						skill = skill,
						abilityEffects = listOf(BattleAbilityEffect.EndTurnPickupConsumedItem()),
					),
					second = participant(
						"berry-holder",
						50,
						currentHp = 50,
						itemId = 5100,
						itemEffects = listOf(BattleItemEffect.BerryMarker(), BattleItemEffect.LowHpHeal(healDenominator = 4)),
					),
				),
			),
			listOf(BattleAction.UseSkill("pickup-holder", skill.skillId, "berry-holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(5100, resolved.participant("pickup-holder")?.itemId)
		assertEquals(null, resolved.participant("berry-holder")?.itemId)
		val event = resolved.events.filterIsInstance<BattleEvent.ConsumedItemPickedUp>().single()
		assertEquals("berry-holder", event.sourceActorId)
	}
}
