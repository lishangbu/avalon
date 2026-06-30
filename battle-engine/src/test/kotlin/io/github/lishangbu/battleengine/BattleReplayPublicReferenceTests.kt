package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleRandomTraceEntry
import io.github.lishangbu.battleengine.random.RecordingBattleRandom
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 验证随机、事件流和 replay 复算基础。
 *
 * 场景类型：确定性随机源、随机 trace、事件片段和严格 replay 场景。
 * 参考来源类型：公开成熟对战引擎的战斗流和行动队列实现。现代规则实现会大量依赖随机数，例如同速排序、
 * 命中、击中要害、伤害浮动、状态持续回合和附加效果概率；如果随机消费顺序不稳定，即使最终 HP 偶然相同，
 * replay 和公开对照测试也会失去可信度。
 * 验证重点：固定随机脚本必须拒绝额外消费或越界值；录制随机源必须保存上界、原因和值；严格 replay 必须
 * 复算出相同事件片段和最终状态；任何随机 trace 篡改或规则实现导致的消费顺序偏移都应被立即发现。
 */
class BattleReplayPublicReferenceTests {
	private val recorder = BattleReplayRecorder()

	@Test
	fun `scripted random rejects extra consumption`() {
		val scenario = publicBattleRuleScenario(
			name = "scripted-random-rejects-extra-consumption",
			inputSummary = "固定随机脚本只提供一个随机值，调用方尝试消费第二个随机值。",
			expectedSummary = "随机源立即失败，暴露实现比 scenario 预期多消费了随机数。",
		)
		val random = ScriptedBattleRandom(listOf(0))

		random.nextInt(2, "first scripted roll")

		scenario.assertNamed("scripted-random-rejects-extra-consumption")
		assertFailsWith<IllegalStateException> {
			random.nextInt(2, "unexpected extra roll")
		}
	}

	@Test
	fun `recording random preserves bound reason and value sequence`() {
		val scenario = publicBattleRuleScenario(
			name = "recording-random-preserves-bound-reason-and-value-sequence",
			inputSummary = "录制随机源包裹固定脚本，依次消费要害随机和伤害随机。",
			expectedSummary = "trace 按消费顺序保存 sequence、bound、reason 和 value，供 replay 严格复算。",
		)
		val random = RecordingBattleRandom(ScriptedBattleRandom(listOf(1, 15)))

		random.nextInt(24, "critical hit for 1")
		random.nextInt(16, "damage random for 1")

		scenario.assertNamed("recording-random-preserves-bound-reason-and-value-sequence")
		assertEquals(
			listOf(
				BattleRandomTraceEntry(1, bound = 24, reason = "critical hit for 1", value = 1),
				BattleRandomTraceEntry(2, bound = 16, reason = "damage random for 1", value = 15),
			),
			random.trace(),
		)
	}

	@Test
	fun `strict replay reproduces event fragment and final state`() {
		val scenario = publicBattleRuleScenario(
			name = "strict-replay-reproduces-event-fragment-and-final-state",
			inputSummary = "录制一回合普通单体伤害行动，保存提交行动、随机 trace 和本回合新增事件。",
			expectedSummary = "严格 replay 使用 trace 复算后得到完全相同的事件片段和最终战斗状态。",
		)
		val turnActions = listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender"))
		val replay = recorder.record(
			initialState = initialState(
				first = participant("attacker", speed = 100),
				second = participant("defender", speed = 50),
			),
			turns = listOf(turnActions),
			random = ScriptedBattleRandom(listOf(1, 15)),
		)

		val replayed = recorder.replay(replay)

		scenario.assertNamed("strict-replay-reproduces-event-fragment-and-final-state")
		assertEquals(replay.finalState, replayed)
		assertEquals(
			listOf(
				BattleEvent.TurnStarted(1),
				BattleEvent.SkillUsed(1, "attacker", "defender", 1, "撞击"),
				BattleEvent.DamageApplied(1, "attacker", "defender", 1, 28, 1.0, 1.0, false),
				BattleEvent.TurnEnded(1),
			),
			replay.turns.single().events,
		)
	}

	@Test
	fun `golden replay pins random trace event fragment and final hp`() {
		val scenario = publicBattleRuleScenario(
			name = "golden-replay-pins-random-trace-event-fragment-and-final-hp",
			inputSummary = "保存一回合普通单体伤害的提交行动、随机消费 trace、事件片段和最终 HP。",
			expectedSummary = "后续引擎实现必须复现同一随机消费顺序、同一事件片段，并把目标 HP 固定到黄金值。",
		)
		val replay = recorder.record(
			initialState = initialState(
				first = participant("attacker", speed = 100),
				second = participant("defender", speed = 50),
			),
			turns = listOf(listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender"))),
			random = ScriptedBattleRandom(listOf(1, 15)),
		)
		val goldenTurn = replay.turns.single()

		scenario.assertNamed("golden-replay-pins-random-trace-event-fragment-and-final-hp")
		assertEquals(
			listOf(
				BattleRandomTraceEntry(1, bound = 24, reason = "critical hit for 1", value = 1),
				BattleRandomTraceEntry(2, bound = 16, reason = "damage random for 1", value = 15),
			),
			goldenTurn.randomTrace,
		)
		assertEquals(
			listOf(
				BattleEvent.TurnStarted(1),
				BattleEvent.SkillUsed(1, "attacker", "defender", 1, "撞击"),
				BattleEvent.DamageApplied(1, "attacker", "defender", 1, 28, 1.0, 1.0, false),
				BattleEvent.TurnEnded(1),
			),
			goldenTurn.events,
		)
		assertEquals(72, replay.finalState.participant("defender")?.currentHp)
		assertEquals(replay.finalState, recorder.replay(replay))
	}

	@Test
	fun `strict replay rejects tampered random trace`() {
		val scenario = publicBattleRuleScenario(
			name = "strict-replay-rejects-tampered-random-trace",
			inputSummary = "录制一回合普通伤害行动后，把第一条随机 trace 的 reason 改成另一个阶段。",
			expectedSummary = "严格 replay 在消费随机数时立即失败，避免被篡改 trace 产生看似可用的复盘。",
		)
		val replay = recorder.record(
			initialState = initialState(
				first = participant("attacker", speed = 100),
				second = participant("defender", speed = 50),
			),
			turns = listOf(listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender"))),
			random = ScriptedBattleRandom(listOf(1, 15)),
		)
		val originalTurn = replay.turns.single()
		val tamperedReplay = replay.copy(
			turns = listOf(
				originalTurn.copy(
					randomTrace = listOf(
						originalTurn.randomTrace.first().copy(reason = "damage random for 1"),
					) + originalTurn.randomTrace.drop(1),
				),
			),
		)

		scenario.assertNamed("strict-replay-rejects-tampered-random-trace")
		assertFailsWith<IllegalStateException> {
			recorder.replay(tamperedReplay)
		}
	}
}
