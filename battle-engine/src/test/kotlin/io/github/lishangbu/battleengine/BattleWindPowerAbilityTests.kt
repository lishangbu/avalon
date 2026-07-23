package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证风力发电只在受到风类技能伤害后为下一次电属性攻击充能。 */
class BattleWindPowerAbilityTests {
	@Test
	fun `wind power charges electric damage after taking wind skill damage`() {
		val state = BattleEngine().start(initialState(
			first = participant("user", 100, skill = damagingSkill(windBased = true)),
			second = participant(
				"holder",
				80,
				abilityEffects = listOf(BattleAbilityEffect.ReceivedDamageNextElementDamageBoost(13, 2.0, windOnly = true)),
			),
		))

		val resolved = BattleEngine().resolveTurn(
			state,
			listOf(BattleAction.UseSkill("user", 1, "holder")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		assertEquals(13, resolved.participant("holder")?.chargedElementId)
		assertEquals(2.0, resolved.participant("holder")?.chargedDamageMultiplier)
	}
}
