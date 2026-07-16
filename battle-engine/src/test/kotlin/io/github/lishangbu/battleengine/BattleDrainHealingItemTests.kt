package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证吸取类回复��到携带道具倍率修正。 */
class BattleDrainHealingItemTests {
	@Test
	fun `big root increases drain healing by thirty percent`() {
		val skill = damagingSkill(hpEffects = listOf(BattleSkillHpEffect.DrainDamage(1, 2)))
		val state = BattleEngine().start(
			initialState(
				first = participant("user", speed = 100, currentHp = 20, skill = skill, itemId = 273, itemEffects = listOf(BattleItemEffect.DrainHealingMultiplier(13, 10))),
				second = participant("target", speed = 50),
			),
		)

		val resolved = BattleEngine().resolveTurn(state, listOf(BattleAction.UseSkill("user", 1, "target")), ScriptedBattleRandom(listOf(1, 15)))
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single().amount

		assertEquals(((damage / 2) * 13) / 10, resolved.events.filterIsInstance<BattleEvent.SkillHealingApplied>().single().amount)
	}
}
