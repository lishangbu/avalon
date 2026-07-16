package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertTrue

/** 验证对焦镜只在目标已经完成本回合技能行动后提升命中率。 */
class BattleZoomLensItemTests {
	@Test
	fun `zoom lens boosts accuracy after target has acted`() {
		val waitSkill = damagingSkill(skillId = 211, name = "等待", damageClass = BattleDamageClass.STATUS, power = null)
		val inaccurateSkill = damagingSkill(skillId = 212, name = "低命中攻击", accuracy = 60)
		val state = BattleEngine().start(
			initialState(
				first = participant(
					actorId = "holder",
					speed = 50,
					skill = inaccurateSkill,
					itemId = 276,
					itemEffects = listOf(BattleItemEffect.AccuracyMultiplierAfterTargetActed(1.2)),
				),
				second = participant("target", speed = 100, skill = waitSkill),
			),
		)

		val resolved = BattleEngine().resolveTurn(
			state,
			listOf(
				BattleAction.UseSkill("holder", 212, "target"),
				BattleAction.UseSkill("target", 211, "target"),
			),
			ScriptedBattleRandom(listOf(65, 1, 15)),
		)

		assertTrue(requireNotNull(resolved.participant("target")).currentHp < 100)
	}
}
