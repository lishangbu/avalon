package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证“本次上场后的第一次技能行动才成功”的技能族。
 *
 * 场景类型：技能使用前 gate 级公开规则场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。公开引擎以 `activeMoveActions` 判断 Fake Out / First
 * Impression 是否仍处于本次上场后的首个技能行动；本引擎用 `activeSkillActionCount` 承载同一事实。
 * 验证重点：失败发生在技能宣告和 PP 消耗之后、命中/保护/伤害之前；行动前状态阻止也会消耗首行动资格；
 * 换出再换入会重置资格。
 */
class BattleFirstSkillActionTests {
	private val engine = BattleEngine()

	@Test
	fun `first action only skill succeeds once then fails before damage`() {
		val scenario = publicBattleRuleScenario(
			name = "first-action-only-succeeds-once-then-fails",
			inputSummary = "较快成员连续两回合使用仅限本次上场后第一次技能行动成功的伤害技能。",
			expectedSummary = "第一回合造成伤害并附加畏缩；第二回合技能已宣告并消耗 PP，但在命中和伤害前失败。",
		)
		val firstActionSkill = firstActionOnlySkill(
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.FLINCH,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val state = engine.start(
			initialState(
				first = participant("opener", speed = 100, skill = firstActionSkill),
				second = participant("target", speed = 50),
			),
		)

		val afterFirst = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("opener", skillId = 252, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val afterSecond = engine.resolveTurn(
			afterFirst,
			listOf(BattleAction.UseSkill("opener", skillId = 252, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("first-action-only-succeeds-once-then-fails")
		assertEquals(72, afterFirst.participant("target")?.currentHp)
		assertEquals(1, afterFirst.participant("opener")?.activeSkillActionCount)
		assertEquals(34, afterFirst.participant("opener")?.skillSlot(252)?.remainingPp)
		assertEquals("target", afterFirst.events.filterIsInstance<BattleEvent.VolatileStatusApplied>().single().targetActorId)

		assertEquals(72, afterSecond.participant("target")?.currentHp)
		assertEquals(2, afterSecond.participant("opener")?.activeSkillActionCount)
		assertEquals(33, afterSecond.participant("opener")?.skillSlot(252)?.remainingPp)
		assertEquals(
			"not-first-skill-action-since-entering",
			afterSecond.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason,
		)
	}

	@Test
	fun `before move prevention consumes first action only eligibility without pp loss`() {
		val scenario = publicBattleRuleScenario(
			name = "before-move-prevention-consumes-first-action-only-eligibility",
			inputSummary = "成员带着畏缩状态尝试使用首行动限定技能；下一回合再次尝试同一技能。",
			expectedSummary = "第一次尝试被行动前状态阻止且不消耗 PP，但已经消耗首行动资格；第二次宣告后失败。",
		)
		val firstActionSkill = firstActionOnlySkill()
		val state = engine.start(
			initialState(
				first = participant("opener", speed = 100, skill = firstActionSkill).copy(flinched = true),
				second = participant("target", speed = 50),
			),
		)

		val afterBlocked = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("opener", skillId = 252, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)
		val afterRetry = engine.resolveTurn(
			afterBlocked,
			listOf(BattleAction.UseSkill("opener", skillId = 252, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("before-move-prevention-consumes-first-action-only-eligibility")
		assertEquals(1, afterBlocked.participant("opener")?.activeSkillActionCount)
		assertEquals(35, afterBlocked.participant("opener")?.skillSlot(252)?.remainingPp)
		assertEquals(false, afterBlocked.participant("opener")?.flinched)
		assertEquals(100, afterBlocked.participant("target")?.currentHp)

		assertEquals(2, afterRetry.participant("opener")?.activeSkillActionCount)
		assertEquals(34, afterRetry.participant("opener")?.skillSlot(252)?.remainingPp)
		assertEquals(100, afterRetry.participant("target")?.currentHp)
		assertEquals(
			"not-first-skill-action-since-entering",
			afterRetry.events.filterIsInstance<BattleEvent.SkillFailed>().single().reason,
		)
	}

	@Test
	fun `switching out and back in resets first action only eligibility`() {
		val scenario = publicBattleRuleScenario(
			name = "switching-resets-first-action-only-eligibility",
			inputSummary = "成员首次使用首行动限定技能后主动换下，再由同侧后备成员换回场上。",
			expectedSummary = "离场和重新入场会重置首行动资格，成员再次上场后的第一次技能行动可以成功造成伤害。",
		)
		val firstActionSkill = firstActionOnlySkill()
		val state = engine.start(
			initialState(
				first = participant("opener", speed = 100, skill = firstActionSkill),
				firstBench = listOf(participant("reserve", speed = 90)),
				second = participant("target", speed = 50),
			),
		)

		val afterFirstUse = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("opener", skillId = 252, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)
		val afterSwitchOut = engine.resolveTurn(
			afterFirstUse,
			listOf(BattleAction.SwitchParticipant("opener", targetActorId = "reserve")),
			ScriptedBattleRandom(emptyList()),
		)
		val afterSwitchBack = engine.resolveTurn(
			afterSwitchOut,
			listOf(BattleAction.SwitchParticipant("reserve", targetActorId = "opener")),
			ScriptedBattleRandom(emptyList()),
		)
		val afterSecondUse = engine.resolveTurn(
			afterSwitchBack,
			listOf(BattleAction.UseSkill("opener", skillId = 252, targetActorId = "target")),
			ScriptedBattleRandom(listOf(1, 15)),
		)

		scenario.assertNamed("switching-resets-first-action-only-eligibility")
		assertEquals(72, afterFirstUse.participant("target")?.currentHp)
		assertEquals(1, afterFirstUse.participant("opener")?.activeSkillActionCount)
		assertEquals(0, afterSwitchOut.participant("opener")?.activeSkillActionCount)
		assertEquals(0, afterSwitchBack.participant("opener")?.activeSkillActionCount)
		assertEquals(44, afterSecondUse.participant("target")?.currentHp)
		assertEquals(1, afterSecondUse.participant("opener")?.activeSkillActionCount)
		assertEquals(emptyList(), afterSecondUse.events.filterIsInstance<BattleEvent.SkillFailed>())
	}

	private fun firstActionOnlySkill(
		volatileStatusApplications: List<BattleVolatileStatusApplication> = emptyList(),
	) =
		damagingSkill(
			skillId = 252,
			name = "击掌奇袭",
			power = 40,
			makesContact = true,
			usableOnlyFirstSkillActionSinceEntering = true,
			priority = 3,
			volatileStatusApplications = volatileStatusApplications,
		)
}
