package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleItemEffect
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleHeldItemElementIdentityAbilityTests {
	@Test
	fun `multitype adopts its plates element and prevents item removal`() {
		val resolved = BattleEngine().start(
			initialState(
				first = participant(
					"holder",
					100,
					itemId = 4700,
					itemEffects = listOf(BattleItemEffect.ElementDamageBoost(10, 1.2)),
					abilityEffects = listOf(
						BattleAbilityEffect.HeldItemElementIdentity(),
						BattleAbilityEffect.HeldItemRemovalImmunity(),
					),
				),
			),
		)
		val holder = requireNotNull(resolved.participant("holder"))

		assertEquals(setOf(10L), holder.elementIds)
		assertEquals(4700, holder.removeHeldItem().itemId)
		assertEquals(4700, holder.consumeHeldItem().itemId)
	}
}
