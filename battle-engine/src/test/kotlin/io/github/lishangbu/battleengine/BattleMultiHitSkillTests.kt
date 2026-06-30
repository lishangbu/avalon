package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证多段命中技能的基础结算。
 *
 * 场景类型：技能执行流程 fixture。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代 2..5 段技能在命中后决定实际段数；
 * 命中判定和 PP 消耗只发生一次，每一段独立结算要害、伤害浮动和倒下中断。
 * 验证重点：段数随机分布可复盘，目标提前倒下时不会继续消费后续段的随机数。
 */
class BattleMultiHitSkillTests {
	private val engine = BattleEngine()

	@Test
	fun `multi hit skill consumes pp once and applies scripted hit count`() {
		val fixture = publicBattleRuleFixture(
			name = "multi-hit-skill-consumes-pp-once-and-applies-scripted-hit-count",
			inputSummary = "2..5 段技能命中目标，固定段数随机数落入 3 段区间。",
			expectedSummary = "技能只消耗 1 点 PP，产生 3 段伤害事件，每段独立消费要害和伤害浮动随机数。",
		)
		val skill = damagingSkill(name = "多段测试", minHits = 2, maxHits = 5)
		val random = ScriptedBattleRandom(listOf(50, 1, 15, 1, 15, 1, 15))
		val state = engine.start(
			initialState(
				first = participant("multi-user", speed = 100, skill = skill),
				second = participant("target", speed = 50),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("multi-user", skillId = 1, targetActorId = "target")),
			random,
		)

		fixture.assertNamed("multi-hit-skill-consumes-pp-once-and-applies-scripted-hit-count")
		assertEquals(34, resolved.participant("multi-user")?.skillSlot(1)?.remainingPp)
		assertEquals(16, resolved.participant("target")?.currentHp)
		assertEquals(3, resolved.events.filterIsInstance<BattleEvent.MultiHitCountDetermined>().single().hitCount)
		assertEquals(listOf(28, 28, 28), resolved.events.filterIsInstance<BattleEvent.DamageApplied>().map { it.amount })
		assertEquals(
			listOf(
				"multi-hit count for 1",
				"critical hit for 1",
				"damage random for 1",
				"critical hit for 1",
				"damage random for 1",
				"critical hit for 1",
				"damage random for 1",
			),
			random.consumedReasons(),
		)
	}

	@Test
	fun `multi hit skill stops consuming hit randoms after target faints`() {
		val fixture = publicBattleRuleFixture(
			name = "multi-hit-skill-stops-after-target-faints",
			inputSummary = "2..5 段技能抽到 5 段，但目标在第 2 段后倒下。",
			expectedSummary = "事件记录抽到 5 段，但只实际结算 2 段伤害，不再消费第 3 段之后的随机数。",
		)
		val skill = damagingSkill(name = "多段击倒测试", minHits = 2, maxHits = 5)
		val random = ScriptedBattleRandom(listOf(99, 1, 15, 1, 15))
		val state = engine.start(
			initialState(
				first = participant("multi-user", speed = 100, skill = skill),
				second = participant("target", speed = 50, currentHp = 40),
			),
		)

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("multi-user", skillId = 1, targetActorId = "target")),
			random,
		)

		fixture.assertNamed("multi-hit-skill-stops-after-target-faints")
		assertEquals(5, resolved.events.filterIsInstance<BattleEvent.MultiHitCountDetermined>().single().hitCount)
		assertEquals(listOf(28, 12), resolved.events.filterIsInstance<BattleEvent.DamageApplied>().map { it.amount })
		assertEquals(0, resolved.participant("target")?.currentHp)
		assertEquals("side-a", resolved.result?.winningSideId)
		assertEquals(
			listOf(
				"multi-hit count for 1",
				"critical hit for 1",
				"damage random for 1",
				"critical hit for 1",
				"damage random for 1",
			),
			random.consumedReasons(),
		)
	}
}
