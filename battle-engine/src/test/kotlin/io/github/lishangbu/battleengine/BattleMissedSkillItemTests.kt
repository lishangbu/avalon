package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 验证技能因命中判定落空后触发的使用者侧一次性能力提升道具。 */
class BattleMissedSkillItemTests {
	private val engine = BattleEngine()

	@Test
	fun `blunder policy raises speed after accuracy miss`() {
		val state = engine.start(
			initialState(
				first = participant(
					"policy-user",
					speed = 100,
					skill = damagingSkill(accuracy = 50),
					itemId = 1121,
					itemEffects = listOf(
						BattleItemEffect.AccuracyMissStatStageBoost(
							stat = BattleStat.SPEED,
							stageDelta = 2,
						),
					),
				),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("policy-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(listOf(99)),
		)

		val user = requireNotNull(resolved.participant("policy-user"))
		assertEquals(2, user.statStage(BattleStat.SPEED))
		assertNull(user.itemId)
	}
}
