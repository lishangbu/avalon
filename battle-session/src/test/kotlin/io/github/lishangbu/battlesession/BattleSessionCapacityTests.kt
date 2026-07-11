package io.github.lishangbu.battlesession

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BattleSessionCapacityTests {
	@Test
	fun `活跃容量耗尽时拒绝创建且终态会立即释放名额`() {
		val sessionIds = ArrayDeque(
			listOf(
				"11111111-1111-4111-8111-111111111111",
				"22222222-2222-4222-8222-222222222222",
				"33333333-3333-4333-8333-333333333333",
			),
		)
		val runtime = BattleSessionRuntime(
			identifierGenerator = SessionIdentifierGenerator { sessionIds.removeFirst() },
			capacity = SessionRuntimeCapacity(
				maxActiveSessions = 1,
				maxRecentSessions = 1,
				recentSessionTtl = Duration.ofMinutes(15),
			),
		)
		val first = runtime.create(sessionInitialState())

		val exhausted = assertFailsWith<SessionCapacityExhaustedException> {
			runtime.create(sessionInitialState())
		}
		assertEquals("battle-session.capacity-exhausted", exhausted.code)

		runtime.terminate(
			first.sessionId,
			TerminationCommand("44444444-4444-4444-8444-444444444444", 0, "释放活跃名额"),
		)
		val second = runtime.create(sessionInitialState())
		assertEquals(BattleSessionStatus.ACTIVE, second.status)
	}
}
