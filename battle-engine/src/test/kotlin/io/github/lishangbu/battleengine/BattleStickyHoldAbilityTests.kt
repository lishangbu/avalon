package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleStickyHoldAbilityTests {
	@Test
	fun `sticky hold prevents contact item transfer`() {
		val skill = damagingSkill(skillId = 951, power = 40, makesContact = true)
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = skill),
					second = participant(
						"holder",
						50,
						itemId = 77,
						itemEffects = listOf(BattleItemEffect.ContactTransferToAttacker()),
						abilityEffects = listOf(BattleAbilityEffect.HeldItemTransferImmunity()),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skill.skillId, "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(77, resolved.participant("holder")?.itemId)
		assertEquals(null, resolved.participant("attacker")?.itemId)
		assertEquals(0, resolved.events.filterIsInstance<BattleEvent.HeldItemTransferred>().size)
	}
}
