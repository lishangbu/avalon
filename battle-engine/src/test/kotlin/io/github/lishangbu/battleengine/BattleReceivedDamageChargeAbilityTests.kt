package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleReceivedDamageChargeAbilityTests {
	@Test
	fun `electromorphosis doubles the next electric attack after taking damage`() {
		val triggerSkill = damagingSkill(skillId = 4500, power = null, fixedDamage = BattleFixedDamage.FixedAmount(1))
		val electricSkill = damagingSkill(skillId = 4501, elementId = 13, power = 40)
		val engine = BattleEngine()
		val afterTrigger = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", 100, skill = triggerSkill),
					second = participant(
						"holder",
						50,
						skill = electricSkill,
						abilityEffects = listOf(BattleAbilityEffect.ReceivedDamageNextElementDamageBoost(13, 2.0)),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", triggerSkill.skillId, "holder")),
			ScriptedBattleRandom(emptyList()),
		)
		assertEquals(13, afterTrigger.participant("holder")?.chargedElementId)

		val charged = engine.resolveTurn(
			afterTrigger,
			listOf(BattleAction.UseSkill("holder", electricSkill.skillId, "attacker")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val damage = charged.events.drop(afterTrigger.events.size)
			.filterIsInstance<BattleEvent.DamageApplied>().single().amount

		assertEquals(38, damage)
		assertEquals(null, charged.participant("holder")?.chargedElementId)
		assertEquals(1.0, charged.participant("holder")?.chargedDamageMultiplier)
	}
}
