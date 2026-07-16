package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 验证声音技能成功后触发的使用者侧一次性能力提升道具。 */
class BattleSoundSkillItemTests {
	private val engine = BattleEngine()

	@Test
	fun `throat spray raises special attack after successful sound skill`() {
		val soundSkill = damagingSkill(soundBased = true)
		val state = engine.start(
			initialState(
				first = participant(
					"sound-user",
					speed = 100,
					skill = soundSkill,
					itemId = 1118,
					itemEffects = listOf(
						BattleItemEffect.SuccessfulSkillStatStageBoost(
							requiresSoundBased = true,
							stat = BattleStat.SPECIAL_ATTACK,
							stageDelta = 1,
						),
					),
				),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("sound-user", skillId = 1, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		val user = requireNotNull(resolved.participant("sound-user"))
		assertEquals(1, user.statStage(BattleStat.SPECIAL_ATTACK))
		assertNull(user.itemId)
	}
}
