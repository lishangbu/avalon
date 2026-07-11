package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.model.BattleAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BattleSessionQueryTests {
	@Test
	fun `列表按状态与赛制筛选后返回稳定分页`() {
		val sessionIds = ArrayDeque(
			listOf(
				"11111111-1111-4111-8111-111111111111",
				"22222222-2222-4222-8222-222222222222",
				"33333333-3333-4333-8333-333333333333",
			),
		)
		val runtime = BattleSessionRuntime(
			identifierGenerator = SessionIdentifierGenerator { sessionIds.removeFirst() },
		)
		runtime.create(sessionInitialState(formatCode = "ranked-single"))
		runtime.create(sessionInitialState(formatCode = "ranked-single"))
		runtime.create(sessionInitialState(formatCode = "casual-single"))

		val page = runtime.list(
			SessionQuery(
				status = BattleSessionStatus.ACTIVE,
				formatCode = "ranked-single",
				page = 1,
				size = 1,
			),
		)

		assertEquals(2, page.totalElements)
		assertEquals(2, page.totalPages)
		assertEquals("11111111-1111-4111-8111-111111111111", page.items.single().sessionId)
	}

	@Test
	fun `列表摘要区分引擎结果与显式终止原因`() {
		val sessionIds = ArrayDeque(
			listOf(
				"44444444-4444-4444-8444-444444444444",
				"55555555-5555-4555-8555-555555555555",
			),
		)
		val runtime = BattleSessionRuntime(
			identifierGenerator = SessionIdentifierGenerator { sessionIds.removeFirst() },
			randomFactory = BattleRandomFactory(::zeroBattleRandom),
		)
		val completing = runtime.create(sessionInitialState(maxTurns = 1))
		val completed = runtime.submitTurn(
			completing.sessionId,
			TurnCommand(
				commandId = "66666666-6666-4666-8666-666666666666",
				expectedRevision = 0,
				actions = listOf(
					BattleAction.UseSkill("side-1-actor-1", 1, "side-2-actor-1"),
					BattleAction.UseSkill("side-2-actor-1", 1, "side-1-actor-1"),
				),
			),
		).session
		val terminating = runtime.create(sessionInitialState())
		val terminated = runtime.terminate(
			terminating.sessionId,
			TerminationCommand(
				commandId = "77777777-7777-4777-8777-777777777777",
				expectedRevision = 0,
				reason = "管理员结束调试",
			),
		).session

		val summaries = runtime.list(SessionQuery(size = 10)).items.associateBy { it.sessionId }

		assertEquals(completed.state.result, summaries.getValue(completed.sessionId).result)
		assertNull(summaries.getValue(completed.sessionId).terminationReason)
		assertNull(summaries.getValue(terminated.sessionId).result)
		assertEquals("管理员结束调试", summaries.getValue(terminated.sessionId).terminationReason)
	}
}
