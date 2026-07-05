package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证扣减目标最近技能 PP 的变化技能。
 *
 * 场景类型：技能后效级公开规则场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。怨恨会读取目标最近一次成功使用的技能，并让该技能剩余 PP
 * 最多减少 4；若目标没有最近技能、对应技能槽不存在，或该技能剩余 PP 已为 0，则怨恨失败。公开资料还说明怨恨
 * 会忽略目标替身，因此成功用例刻意让目标带替身，避免以后把它误接到通用“对手替身阻挡变化效果”分支。
 */
class BattleSkillPpReductionTests {
	private val engine = BattleEngine()

	@Test
	fun `spite style skill reduces target last successful skill pp and ignores substitute`() {
		val scenario = publicBattleRuleScenario(
			name = "spite-style-skill-reduces-target-last-successful-skill-pp-and-ignores-substitute",
			inputSummary = "目标最近成功使用的技能还剩 8 PP，并且当前有替身；使用者命中怨恨式变化技能。",
			expectedSummary = "目标最近技能剩余 PP 从 8 扣到 4，替身不阻止该效果。",
		)
		val targetSkill = damagingSkill(skillId = 10, name = "最近技能").copy(remainingPp = 8, maxPp = 10)
		val spite = spiteLikeSkill()
		val state = engine.start(
			initialState(
				first = participant("spite-user", speed = 100, skill = spite),
				second = participant("target", speed = 50, skill = targetSkill)
					.copy(lastSuccessfulSkillId = 10, substituteHp = 20),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("spite-user", skillId = 180, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("spite-style-skill-reduces-target-last-successful-skill-pp-and-ignores-substitute")
		assertEquals(4, resolved.participant("target")?.skillSlot(10)?.remainingPp)
		assertEquals(34, resolved.participant("spite-user")?.skillSlot(180)?.remainingPp)
		val reduction = resolved.events.filterIsInstance<BattleEvent.SkillPpReduced>().single()
		assertEquals("spite-user", reduction.actorId)
		assertEquals("target", reduction.targetActorId)
		assertEquals(180, reduction.skillId)
		assertEquals(10, reduction.reducedSkillId)
		assertEquals(4, reduction.amount)
		assertEquals(8, reduction.previousRemainingPp)
		assertEquals(4, reduction.currentRemainingPp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillFailed>())
	}

	@Test
	fun `spite style skill fails when target has no last successful skill`() {
		val scenario = publicBattleRuleScenario(
			name = "spite-style-skill-fails-without-target-last-successful-skill",
			inputSummary = "目标本场还没有成功使用过技能；使用者命中怨恨式变化技能。",
			expectedSummary = "技能消耗自身 PP 后失败，不会扣减目标任何技能 PP。",
		)
		val targetSkill = damagingSkill(skillId = 10, name = "未使用技能").copy(remainingPp = 8, maxPp = 10)
		val state = engine.start(
			initialState(
				first = participant("spite-user", speed = 100, skill = spiteLikeSkill()),
				second = participant("target", speed = 50, skill = targetSkill),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("spite-user", skillId = 180, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("spite-style-skill-fails-without-target-last-successful-skill")
		assertEquals(8, resolved.participant("target")?.skillSlot(10)?.remainingPp)
		assertEquals(34, resolved.participant("spite-user")?.skillSlot(180)?.remainingPp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillPpReduced>())
		assertEquals(
			"target-has-no-last-skill-with-pp",
			resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason,
		)
	}

	@Test
	fun `spite style skill fails when target last successful skill has no pp`() {
		val scenario = publicBattleRuleScenario(
			name = "spite-style-skill-fails-when-target-last-successful-skill-has-no-pp",
			inputSummary = "目标最近成功使用的技能已经没有剩余 PP；使用者命中怨恨式变化技能。",
			expectedSummary = "技能消耗自身 PP 后失败，不会产生扣减 0 PP 的伪成功事件。",
		)
		val targetSkill = damagingSkill(skillId = 10, name = "空 PP 技能").copy(remainingPp = 0, maxPp = 10)
		val state = engine.start(
			initialState(
				first = participant("spite-user", speed = 100, skill = spiteLikeSkill()),
				second = participant("target", speed = 50, skill = targetSkill).copy(lastSuccessfulSkillId = 10),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("spite-user", skillId = 180, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("spite-style-skill-fails-when-target-last-successful-skill-has-no-pp")
		assertEquals(0, resolved.participant("target")?.skillSlot(10)?.remainingPp)
		assertEquals(34, resolved.participant("spite-user")?.skillSlot(180)?.remainingPp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillPpReduced>())
		assertEquals(
			"target-has-no-last-skill-with-pp",
			resolved.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason,
		)
	}

	private fun spiteLikeSkill(): io.github.lishangbu.battleengine.model.BattleSkillSlot =
		damagingSkill(
			skillId = 180,
			name = "怨恨",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			accuracy = 100,
			targetLastSkillPpReduction = 4,
		)
}
