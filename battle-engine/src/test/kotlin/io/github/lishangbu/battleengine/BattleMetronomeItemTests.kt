package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 验证节拍器按连续成功使用同一技能的次数提高伤害。 */
class BattleMetronomeItemTests {
	@Test
	fun `metronome increases damage on consecutive use of the same skill`() {
		val skill = damagingSkill(skillId = 351)
		val initial = BattleEngine().start(
			initialState(
				first = participant(
					"holder", 100, skill = skill, itemId = 277,
					itemEffects = listOf(BattleItemEffect.ConsecutiveSkillDamageBoost()),
				),
				second = participant("target", 50),
			),
		)
		val first = BattleEngine().resolveTurn(
			initial,
			listOf(BattleAction.UseSkill("holder", 351, "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val second = BattleEngine().resolveTurn(
			first,
			listOf(BattleAction.UseSkill("holder", 351, "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		val damages = second.events.filterIsInstance<BattleEvent.DamageApplied>()
		assertTrue(damages[1].amount > damages[0].amount)
		assertEquals(2, second.participant("holder")?.consecutiveSuccessfulSkillUses)
	}
}
