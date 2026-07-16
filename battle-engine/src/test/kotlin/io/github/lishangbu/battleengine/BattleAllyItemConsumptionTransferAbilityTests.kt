package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleAllyItemConsumptionTransferAbilityTests {
	@Test
	fun `symbiosis gives its held item to an ally after that ally consumes an item`() {
		val consumer = participant(
			"consumer",
			100,
			itemId = 4900,
			itemEffects = listOf(BattleItemEffect.BerryMarker()),
		)
		val donor = participant(
			"donor",
			90,
			itemId = 4901,
			itemEffects = listOf(BattleItemEffect.SpeedMultiplier(1.5)),
			abilityEffects = listOf(BattleAbilityEffect.AllyItemConsumptionTransfer()),
		)
		val started = BattleEngine().start(
			doubleInitialState(
				firstA = consumer,
				firstB = donor,
			),
		)
		val resolved = started.replaceParticipant(requireNotNull(started.participant("consumer")).consumeHeldItem())

		assertEquals(4901, resolved.participant("consumer")?.itemId)
		assertEquals(null, resolved.participant("donor")?.itemId)
		val event = resolved.events.filterIsInstance<BattleEvent.HeldItemTransferred>().single()
		assertEquals("donor", event.fromActorId)
		assertEquals("consumer", event.toActorId)
	}
}
