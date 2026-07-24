package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.damage.BattleDamageRequest
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 验证笨拙与化学变化气体使用统一的可恢复被动压制状态。 */
class BattlePassiveSuppressionAbilityTests {
	@Test
	fun `klutz keeps held item identity but disables its damage effect`() {
		val skill = damagingSkill(elementId = 10)
		val itemEffect = BattleItemEffect.ElementDamageBoost(elementId = 10, multiplier = 1.2)
		val klutzUser = participant(
			"klutz-user",
			100,
			elementId = 10,
			skill = skill,
			abilityEffects = listOf(BattleAbilityEffect.HeldItemEffectSuppression()),
			itemId = 226,
			itemEffects = listOf(itemEffect),
		)
		val ordinaryUser = klutzUser.copy(actorId = "ordinary-user")
			.replaceAbilityEffects(emptyList())
		val target = participant("target", 50)
		val klutzState = BattleEngine().start(initialState(klutzUser, target))
		val ordinaryState = BattleEngine().start(initialState(ordinaryUser, target))
		val synchronizedKlutzUser = klutzState.participant(klutzUser.actorId) ?: error("klutz user missing")

		assertEquals(226, synchronizedKlutzUser.itemId)
		assertTrue(synchronizedKlutzUser.itemEffects.isEmpty())
		assertEquals(listOf(itemEffect), synchronizedKlutzUser.suppressedItemEffects)
		assertTrue(
			calculateDamage(klutzState, synchronizedKlutzUser, target, skill) <
				calculateDamage(ordinaryState, ordinaryUser, target, skill),
		)
	}

	@Test
	fun `neutralizing gas suppresses opposing ability and restores it after switching out`() {
		val gas = participant(
			"gas",
			100,
			abilityEffects = listOf(BattleAbilityEffect.FieldAbilitySuppression()),
		)
		val reserve = participant("reserve", 80)
		val damageBoost = BattleAbilityEffect.ElementSkillDamageBoost(setOf(1), multiplier = 1.5)
		val opponent = participant("opponent", 50, abilityEffects = listOf(damageBoost))
		val engine = BattleEngine()
		val started = engine.start(initialState(gas, opponent, firstBench = listOf(reserve)))
		val suppressedOpponent = started.participant(opponent.actorId) ?: error("opponent missing")

		assertTrue(suppressedOpponent.abilityEffects.isEmpty())
		assertEquals(listOf(damageBoost), suppressedOpponent.suppressedAbilityEffects)

		val restored = engine.resolveTurn(
			started,
			listOf(BattleAction.SwitchParticipant(gas.actorId, reserve.actorId)),
			ScriptedBattleRandom(emptyList()),
		).participant(opponent.actorId) ?: error("opponent missing after switch")

		assertEquals(listOf(damageBoost), restored.abilityEffects)
		assertTrue(restored.suppressedAbilityEffects.isEmpty())
	}

	@Test
	fun `another neutralizing gas holder keeps abilities suppressed`() {
		val gasEffect = BattleAbilityEffect.FieldAbilitySuppression()
		val firstGas = participant("first-gas", 100, abilityEffects = listOf(gasEffect))
		val secondGas = participant("second-gas", 90, abilityEffects = listOf(gasEffect))
		val targetEffect = BattleAbilityEffect.AlwaysHit()
		val target = participant("target", 50, abilityEffects = listOf(targetEffect))
		val observer = participant("observer", 40)
		val started = BattleEngine().start(doubleInitialState(firstGas, secondGas, target, observer))
		val fainted = started.participant(firstGas.actorId)?.copy(currentHp = 0) ?: error("gas holder missing")

		val synchronized = started.replaceParticipant(fainted).synchronizePassiveSuppressions()

		assertTrue(synchronized.participant(target.actorId)?.abilityEffects.isNullOrEmpty())
		assertEquals(listOf(targetEffect), synchronized.participant(target.actorId)?.suppressedAbilityEffects)
	}

	@Test
	fun `neutralizing gas temporarily reactivates item suppressed by klutz`() {
		val gas = participant("gas", 100, abilityEffects = listOf(BattleAbilityEffect.FieldAbilitySuppression()))
		val itemEffect = BattleItemEffect.ElementDamageBoost(1, 1.2)
		val klutz = participant(
			"klutz",
			50,
			abilityEffects = listOf(BattleAbilityEffect.HeldItemEffectSuppression()),
			itemId = 226,
			itemEffects = listOf(itemEffect),
		)

		val underGas = BattleEngine().start(initialState(gas, klutz)).participant(klutz.actorId)
			?: error("klutz holder missing")

		assertTrue(underGas.abilityEffects.isEmpty())
		assertEquals(listOf(itemEffect), underGas.itemEffects)
		assertTrue(underGas.suppressedItemEffects.isEmpty())
	}

	@Test
	fun `neutralizing gas keeps identity defining abilities active`() {
		val gas = participant("gas", 100, abilityEffects = listOf(BattleAbilityEffect.FieldAbilitySuppression()))
		val identityEffect = BattleAbilityEffect.AlwaysTreatedAsleep()
		val identityHolder = participant("identity-holder", 50, abilityEffects = listOf(identityEffect))

		val started = BattleEngine().start(initialState(gas, identityHolder))
		val synchronized = started.participant(identityHolder.actorId) ?: error("identity holder missing")

		assertEquals(listOf(identityEffect), synchronized.abilityEffects)
		assertTrue(synchronized.suppressedAbilityEffects.isEmpty())
	}

	@Test
	fun `neutralizing gas keeps held item element identity active`() {
		val gas = participant("gas", 100, abilityEffects = listOf(BattleAbilityEffect.FieldAbilitySuppression()))
		val identityEffect = BattleAbilityEffect.HeldItemElementIdentity()
		val identityHolder = participant(
			"identity-holder",
			50,
			itemId = 4700,
			itemEffects = listOf(BattleItemEffect.ElementDamageBoost(10, 1.2)),
			abilityEffects = listOf(identityEffect, BattleAbilityEffect.HeldItemRemovalImmunity()),
		)

		val synchronized = BattleEngine().start(initialState(gas, identityHolder))
			.participant(identityHolder.actorId) ?: error("identity holder missing")

		assertEquals(setOf(10L), synchronized.elementIds)
		assertEquals(listOf(identityEffect, BattleAbilityEffect.HeldItemRemovalImmunity()), synchronized.abilityEffects)
		assertTrue(synchronized.suppressedAbilityEffects.isEmpty())
	}

	private fun calculateDamage(
		state: BattleState,
		attacker: BattleParticipant,
		defender: BattleParticipant,
		skill: BattleSkillSlot,
	): Int = BattleDamageCalculator().calculate(
		BattleDamageRequest(
			attacker = attacker,
			defender = defender,
			skill = skill,
			rules = state.rules,
			environment = state.environment,
			randomPercent = 100,
		),
	).amount
}
