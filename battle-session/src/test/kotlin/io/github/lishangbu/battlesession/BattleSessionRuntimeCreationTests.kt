package io.github.lishangbu.battlesession

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleSessionRuntimeCreationTests {
	@Test
	fun `创建成功后返回服务端 UUID 会话并从 ACTIVE revision 0 开始`() {
		val runtime = BattleSessionRuntime()

		val session = runtime.create(sessionInitialState())

		assertEquals(4, UUID.fromString(session.sessionId).version())
		assertEquals(BattleSessionStatus.ACTIVE, session.status)
		assertEquals(0, session.revision)
		assertEquals(0, session.state.turnNumber)
	}
}
