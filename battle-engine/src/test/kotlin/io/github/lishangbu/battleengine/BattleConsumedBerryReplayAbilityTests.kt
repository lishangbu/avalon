package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleConsumedBerryReplayAbilityTests {
	@Test
	fun `cud chew repeats a healing berry at the end of the next turn`() {
		val triggerSkill = damagingSkill(skillId = 5000, power = 40)
		val weakSkill = damagingSkill(skillId = 5001, power = null, fixedDamage = BattleFixedDamage.FixedAmount(1))
		val engine = BattleEngine()
		val afterConsumption = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = triggerSkill),
					second = participant(
						"holder",
						50,
						currentHp = 50,
						abilityEffects = listOf(BattleAbilityEffect.EndTurnConsumedBerryReplay()),
						itemId = 5000,
						itemEffects = listOf(BattleItemEffect.BerryMarker(), BattleItemEffect.LowHpHeal(healDenominator = 4)),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", triggerSkill.skillId, "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(47, afterConsumption.participant("holder")?.currentHp)
		assertEquals(1, afterConsumption.participant("holder")?.lastConsumedItemTurn)

		val afterReplay = engine.resolveTurn(
			afterConsumption.replaceParticipant(
				requireNotNull(afterConsumption.participant("attacker")).copy(skillSlots = listOf(weakSkill)),
			),
			listOf(BattleAction.UseSkill("attacker", weakSkill.skillId, "holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(71, afterReplay.participant("holder")?.currentHp)
		assertEquals(null, afterReplay.participant("holder")?.lastConsumedItemId)
	}

	@Test
	fun `cud chew repeats a stat berry without restoring the item`() {
		val triggerSkill = damagingSkill(skillId = 5002, power = 40)
		val weakSkill = damagingSkill(skillId = 5003, power = null, fixedDamage = BattleFixedDamage.FixedAmount(1))
		val engine = BattleEngine()
		val afterConsumption = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = triggerSkill),
					second = participant(
						"holder",
						50,
						currentHp = 50,
						abilityEffects = listOf(BattleAbilityEffect.EndTurnConsumedBerryReplay()),
						itemId = 5002,
						itemEffects = listOf(
							BattleItemEffect.BerryMarker(),
							BattleItemEffect.LowHpStatStageBoost(BattleStat.ATTACK, 1),
						),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", triggerSkill.skillId, "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val afterReplay = engine.resolveTurn(
			afterConsumption.replaceParticipant(
				requireNotNull(afterConsumption.participant("attacker")).copy(skillSlots = listOf(weakSkill)),
			),
			listOf(BattleAction.UseSkill("attacker", weakSkill.skillId, "holder")),
			ScriptedBattleRandom(emptyList()),
		)

		assertEquals(2, afterReplay.participant("holder")?.statStage(BattleStat.ATTACK))
		assertEquals(null, afterReplay.participant("holder")?.itemId)
	}
}
