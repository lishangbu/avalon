package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleAbilityItemStealTests {
	@Test
	fun `magician steals the damaged targets held item`() {
		val resolved = resolve(
			attacker = participant(
				"attacker",
				100,
				abilityEffects = listOf(BattleAbilityEffect.DamagingSkillStealTargetHeldItem()),
			),
			target = participant("target", 50, itemId = 71, itemEffects = listOf(BattleItemEffect.SpeedMultiplier(1.5))),
		)

		assertEquals(71, resolved.participant("attacker")?.itemId)
		assertEquals(null, resolved.participant("target")?.itemId)
	}

	@Test
	fun `pickpocket steals the contact attackers held item`() {
		val contactSkill = damagingSkill(skillId = 1031, power = 40, makesContact = true)
		val resolved = resolve(
			attacker = participant(
				"attacker",
				100,
				skill = contactSkill,
				itemId = 72,
				itemEffects = listOf(BattleItemEffect.SpeedMultiplier(1.5)),
			),
			target = participant(
				"target",
				50,
				abilityEffects = listOf(BattleAbilityEffect.ContactStealAttackerHeldItem()),
			),
		)

		assertEquals(null, resolved.participant("attacker")?.itemId)
		assertEquals(72, resolved.participant("target")?.itemId)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.HeldItemTransferred>().size)
	}

	private fun resolve(
		attacker: io.github.lishangbu.battleengine.model.BattleParticipant,
		target: io.github.lishangbu.battleengine.model.BattleParticipant,
	): io.github.lishangbu.battleengine.model.BattleState {
		val engine = BattleEngine()
		val skillId = attacker.skillSlots.single().skillId
		return engine.resolveTurn(
			engine.start(initialState(first = attacker, second = target)),
			listOf(BattleAction.UseSkill(attacker.actorId, skillId, target.actorId)),
			ScriptedBattleRandom(listOf(1, 15)),
		)
	}
}
