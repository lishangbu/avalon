package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.github.lishangbu.battleengine.model.BattleFixedDamage

class BattleAbilityReplacementTests {
	@Test
	fun `receiver copies an ally ability when that ally faints`() {
		val knockout = damagingSkill(
			skillId = 1083,
			power = null,
			fixedDamage = BattleFixedDamage.FixedAmount(200),
		)
		val copiedEffect = BattleAbilityEffect.ForcedLastActionOrder()
		val engine = BattleEngine()
		val resolved = engine.resolveTurn(
			engine.start(
				doubleInitialState(
					firstA = participant(
						"receiver",
						80,
						abilityId = 7,
						abilityEffects = listOf(BattleAbilityEffect.FaintedAllyAbilityCopy()),
					),
					firstB = participant("ally", 70, abilityId = 8, abilityEffects = listOf(copiedEffect)),
					secondA = participant("attacker", 100, skill = knockout),
					secondB = participant("opponent-ally", 60),
				),
			),
			listOf(BattleAction.UseSkill("attacker", knockout.skillId, "ally")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(8, resolved.participant("receiver")?.abilityId)
		assertEquals(listOf(copiedEffect), resolved.participant("receiver")?.abilityEffects)
	}

	@Test
	fun `trace copies an active opponents ability on switch in`() {
		val copiedEffect = BattleAbilityEffect.RandomActionOrderBoost(30)
		val started = BattleEngine().start(
			initialState(
				first = participant(
					"trace-user",
					100,
					abilityId = 1,
					abilityEffects = listOf(BattleAbilityEffect.SwitchInCopyOpponentAbility()),
				),
				second = participant("opponent", 50, abilityId = 2, abilityEffects = listOf(copiedEffect)),
			),
		)

		assertEquals(2, started.participant("trace-user")?.abilityId)
		assertEquals(listOf(copiedEffect), started.participant("trace-user")?.abilityEffects)
	}

	@Test
	fun `mummy replaces the contact attackers ability with the holders ability`() {
		val skill = damagingSkill(skillId = 1081, power = 40, makesContact = true)
		val resolved = resolveContact(
			participant(
				"attacker",
				100,
				skill = skill,
				abilityId = 3,
				abilityEffects = listOf(BattleAbilityEffect.ForcedLastActionOrder()),
			),
			participant(
				"holder",
				50,
				abilityId = 4,
				abilityEffects = listOf(BattleAbilityEffect.ContactReplaceAttackerAbilityWithHolder()),
			),
		)

		assertEquals(4, resolved.participant("attacker")?.abilityId)
		assertTrue(resolved.events.any { it is BattleEvent.AbilityChanged })
	}

	@Test
	fun `wandering spirit swaps abilities after contact damage`() {
		val skill = damagingSkill(skillId = 1082, power = 40, makesContact = true)
		val resolved = resolveContact(
			participant("attacker", 100, skill = skill, abilityId = 5),
			participant(
				"holder",
				50,
				abilityId = 6,
				abilityEffects = listOf(BattleAbilityEffect.ContactSwapAbilities()),
			),
		)

		assertEquals(6, resolved.participant("attacker")?.abilityId)
		assertEquals(5, resolved.participant("holder")?.abilityId)
	}

	private fun resolveContact(
		attacker: io.github.lishangbu.battleengine.model.BattleParticipant,
		holder: io.github.lishangbu.battleengine.model.BattleParticipant,
	): io.github.lishangbu.battleengine.model.BattleState {
		val engine = BattleEngine()
		return engine.resolveTurn(
			engine.start(initialState(first = attacker, second = holder)),
			listOf(BattleAction.UseSkill(attacker.actorId, attacker.skillSlots.single().skillId, holder.actorId)),
			ScriptedBattleRandom(listOf(1, 15)),
		)
	}
}
