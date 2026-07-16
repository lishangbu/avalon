package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleItemEffect
import kotlin.test.Test
import kotlin.test.assertFalse

/** 验证特性护具阻止对手绕过持有者特性。 */
class BattleAbilityShieldItemTests {
	@Test
	fun `ability shield prevents target ability effects from being ignored`() {
		val actor = participant(
			"actor", 100, abilityId = 1,
			abilityEffects = listOf(BattleAbilityEffect.IgnoreTargetAbilityEffects()),
		)
		val target = participant(
			"target", 50, itemId = 1881,
			itemEffects = listOf(BattleItemEffect.AbilityIgnoreProtection()),
		)
		val state = BattleEngine().start(initialState(first = actor, second = target))

		assertFalse(BattleTargetDefenseEffects().skillIgnoresTargetAbilityEffects(state, actor, target))
	}
}
