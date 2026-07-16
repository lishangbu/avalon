package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.SkillPreventionReason
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/** 验证伤害技能命中后由携带道具追加的畏缩概率。 */
class BattleFlinchChanceItemTests {
	private val engine = BattleEngine()

	@Test
	fun `flinch chance item can prevent slower target action`() {
		val state = engine.start(
			initialState(
				first = participant(
					"item-user",
					speed = 100,
					itemId = 198,
					itemEffects = listOf(BattleItemEffect.AdditionalFlinchChance(chancePercent = 10)),
				),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("item-user", skillId = 1, targetActorId = "target"),
				BattleAction.UseSkill("target", skillId = 1, targetActorId = "item-user"),
			),
			ScriptedBattleRandom(listOf(1, 15, 0)),
		)

		assertEquals(100, resolved.participant("item-user")?.currentHp)
		assertEquals(
			SkillPreventionReason.VOLATILE_STATUS,
			resolved.events.filterIsInstance<BattleEvent.SkillPrevented>().single().reason,
		)
	}
}
