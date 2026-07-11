package io.github.lishangbu.battlesession

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class BattleSessionTerminationTests {
	@Test
	fun `终止 ACTIVE 会话生成诊断记录且相同命令重试返回首次结果`() {
		val runtime = BattleSessionRuntime()
		val created = runtime.create(sessionInitialState())
		val command = TerminationCommand(
			commandId = "12345678-1234-4234-8234-123456789abc",
			expectedRevision = 0,
			reason = "管理员结束调试",
		)

		val first = runtime.terminate(created.sessionId, command)
		val retried = runtime.terminate(created.sessionId, command)

		assertEquals(first, retried)
		assertEquals(BattleSessionStatus.TERMINATED, first.session.status)
		assertEquals(1, first.session.revision)
		assertNull(first.session.state.result)
		assertEquals(emptyList(), first.session.requirements.selections)
		assertEquals("管理员结束调试", first.termination.reason)
		assertEquals(first.termination, first.session.battleRecord?.termination)
		assertEquals(first.session.state, first.session.battleRecord?.replay?.finalState)
		assertSame(first.session.battleRecord, runtime.get(created.sessionId).battleRecord)
	}
}
