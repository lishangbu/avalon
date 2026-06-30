package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleRandomTraceEntry
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 验证战斗 replay 的录制和严格回放。
 *
 * 场景类型：确定性 replay 基础设施。
 * 参考来源类型：公式级自有 场景；公开规则对照测试可以在此基础上保存完整随机 trace 和事件片段。
 * 验证重点：录制器必须保留初始事件、提交行动、每次随机消费的上界/原因/值，以及每回合新增事件；严格回放
 * 必须能发现随机 trace 被篡改或引擎消费顺序发生偏移。
 */
class BattleReplayRecorderTests {
	private val recorder = BattleReplayRecorder()

	@Test
	fun `records turn actions random trace events and replays final state`() {
		val initialState = initialState(
			first = participant("attacker", speed = 100),
			second = participant("defender", speed = 50),
		)
		val turnActions = listOf(
			BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender"),
		)

		val replay = recorder.record(
			initialState = initialState,
			turns = listOf(turnActions),
			random = ScriptedBattleRandom(listOf(1, 15)),
		)

		val turn = replay.turns.single()
		assertEquals(listOf(BattleEvent.BattleStarted(0, "standard-single", listOf("side-a", "side-b"))), replay.initialEvents)
		assertEquals(1, turn.turnNumber)
		assertEquals(turnActions, turn.submittedActions)
		assertEquals(
			listOf(
				BattleRandomTraceEntry(1, bound = 24, reason = "critical hit for 1", value = 1),
				BattleRandomTraceEntry(2, bound = 16, reason = "damage random for 1", value = 15),
			),
			turn.randomTrace,
		)
		assertEquals(
			listOf(
				BattleEvent.TurnStarted(1),
				BattleEvent.SkillUsed(1, "attacker", "defender", 1, "撞击"),
				BattleEvent.DamageApplied(1, "attacker", "defender", 1, 28, 1.0, 1.0, false),
				BattleEvent.TurnEnded(1),
			),
			turn.events,
		)
		assertEquals(replay.finalState, recorder.replay(replay))
	}

	@Test
	fun `strict replay rejects mismatched random reason`() {
		val initialState = initialState(
			first = participant("attacker", speed = 100),
			second = participant("defender", speed = 50),
		)
		val replay = recorder.record(
			initialState = initialState,
			turns = listOf(listOf(BattleAction.UseSkill("attacker", skillId = 1, targetActorId = "defender"))),
			random = ScriptedBattleRandom(listOf(1, 15)),
		)
		val turn = replay.turns.single()
		val tamperedTurn = turn.copy(
			randomTrace = listOf(
				turn.randomTrace.first().copy(reason = "damage random for 1"),
			) + turn.randomTrace.drop(1),
		)
		val tamperedReplay = replay.copy(turns = listOf(tamperedTurn))

		assertFailsWith<IllegalStateException> {
			recorder.replay(tamperedReplay)
		}
	}
}
