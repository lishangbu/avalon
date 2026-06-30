package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.random.RecordingBattleRandom
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证现代主系列随机相邻对手目标。
 *
 * 场景类型：随机目标候选收集、随机 trace、无候选目标取消 fixture。
 * 参考来源类型：公开成熟对战引擎的目标解析实现，以及公开技能目标资料中的 random-opponent 目标类型。
 * 随机目标不是“提交目标”的变体；技能执行时会重新读取当前对手侧可战斗上场成员，并在候选超过一名时消费
 * 一次可复盘随机数。若没有任何可战斗候选，行动在技能使用前取消，不消耗 PP，也不产生技能使用事件。
 * 验证重点：随机目标忽略提交的 targetActorId，随机消费原因稳定进入 replay trace，且目标为空时不污染随机轨迹。
 */
class BattleRandomTargetPublicReferenceTests {
	private val engine = BattleEngine()

	@Test
	fun `random adjacent opponent target chooses scripted capable opponent`() {
		val fixture = publicBattleRuleFixture(
			name = "random-adjacent-opponent-target-chooses-scripted-capable-opponent",
			inputSummary = "双打中随机相邻对手技能提交的 targetActorId 指向同侧伙伴，对手两名成员均可战斗。",
			expectedSummary = "技能忽略提交目标，在两个可战斗对手中消费一次目标随机并命中脚本指定的对手。",
		)
		val randomSkill = damagingSkill(targetScope = BattleSkillTargetScope.RANDOM_ADJACENT_OPPONENT)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("random-user", speed = 100, skill = randomSkill),
				firstB = participant("ally", speed = 90),
				secondA = participant("opponent-left", speed = 80),
				secondB = participant("opponent-right", speed = 70),
			),
		)
		val random = RecordingBattleRandom(ScriptedBattleRandom(listOf(1, 1, 15)))

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("random-user", skillId = 1, targetActorId = "ally")),
			random,
		)
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single()

		fixture.assertNamed("random-adjacent-opponent-target-chooses-scripted-capable-opponent")
		assertEquals(listOf("opponent-right"), resolved.events.filterIsInstance<BattleEvent.SkillUsed>().map { it.targetActorId })
		assertEquals("opponent-right", damage.targetActorId)
		assertEquals(1.0, damage.targetMultiplier)
		assertEquals(100, resolved.participant("opponent-left")?.currentHp)
		assertEquals(100, resolved.participant("ally")?.currentHp)
		assertEquals(
			listOf(
				"random adjacent opponent target for 1",
				"critical hit for 1",
				"damage random for 1",
			),
			random.trace().map { it.reason },
		)
		assertEquals(listOf(2, 24, 16), random.trace().map { it.bound })
	}

	@Test
	fun `random adjacent opponent with no capable opponent cancels before pp and random`() {
		val fixture = publicBattleRuleFixture(
			name = "random-adjacent-opponent-with-no-capable-opponent-cancels-before-pp-and-random",
			inputSummary = "双打中随机相邻对手技能执行时，对手两个上场成员都已经无法战斗。",
			expectedSummary = "候选目标为空，行动在技能使用前取消，不消耗 PP，也不消费目标随机数。",
		)
		val randomSkill = damagingSkill(targetScope = BattleSkillTargetScope.RANDOM_ADJACENT_OPPONENT)
		val state = engine.start(
			doubleInitialState(
				firstA = participant("random-user", speed = 100, skill = randomSkill),
				firstB = participant("ally", speed = 90),
				secondA = participant("fainted-left", speed = 80, currentHp = 0),
				secondB = participant("fainted-right", speed = 70, currentHp = 0),
			),
		)
		val random = ScriptedBattleRandom(emptyList())

		val resolved = engine.resolveTurn(
			state,
			listOf(BattleAction.UseSkill("random-user", skillId = 1, targetActorId = "fainted-left")),
			random,
		)

		fixture.assertNamed("random-adjacent-opponent-with-no-capable-opponent-cancels-before-pp-and-random")
		assertEquals(35, resolved.participant("random-user")?.skillSlot(1)?.remainingPp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillUsed>())
		assertTrue(random.isFullyConsumed())
	}
}
