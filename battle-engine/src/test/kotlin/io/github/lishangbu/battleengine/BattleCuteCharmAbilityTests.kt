package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleGender
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证迷人之躯只会在异性成员有效接触并通过概率判定后触发。 */
class BattleCuteCharmAbilityTests {
	private val engine = BattleEngine()

	@Test
	fun `cute charm infatuates an opposite gender contact attacker`() {
		val random = ScriptedBattleRandom(listOf(1, 15, 0))
		val state = engine.start(initialState(
			first = participant("attacker", 100, skill = damagingSkill(makesContact = true)).copy(gender = BattleGender.MALE),
			second = participant(
				"holder",
				80,
				abilityEffects = listOf(BattleAbilityEffect.ContactInfatuationOnAttacker(30)),
			).copy(gender = BattleGender.FEMALE),
		))

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("attacker", 1, "holder")),
			random,
		)

		assertEquals("holder", resolved.participant("attacker")?.infatuatedByActorId)
		assertEquals(
			BattleVolatileStatus.INFATUATION,
			resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplied>().single().status,
		)
	}

	@Test
	fun `cute charm ignores same gender and genderless contact attackers`() {
		listOf(BattleGender.FEMALE, BattleGender.GENDERLESS).forEach { attackerGender ->
			val state = engine.start(initialState(
				first = participant("attacker", 100, skill = damagingSkill(makesContact = true)).copy(gender = attackerGender),
				second = participant(
					"holder",
					80,
					abilityEffects = listOf(BattleAbilityEffect.ContactInfatuationOnAttacker(30)),
				).copy(gender = BattleGender.FEMALE),
			))

			val resolved = engine.resolveTurn(
				state,
				listOf(BattleAction.UseSkill("attacker", 1, "holder")),
				ScriptedBattleRandom(listOf(1, 15)),
			)

			assertEquals(null, resolved.participant("attacker")?.infatuatedByActorId)
		}
	}
}
