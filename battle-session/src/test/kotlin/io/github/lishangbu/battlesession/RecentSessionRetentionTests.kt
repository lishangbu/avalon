package io.github.lishangbu.battlesession

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecentSessionRetentionTests {
	@Test
	fun `近期容量淘汰最早终态且 TTL 到期后不再可见`() {
		val clock = MutableTestClock(Instant.parse("2026-07-11T00:00:00Z"))
		val sessionIds = ArrayDeque(
			listOf(
				"11111111-1111-4111-8111-111111111111",
				"22222222-2222-4222-8222-222222222222",
			),
		)
		val runtime = BattleSessionRuntime(
			identifierGenerator = SessionIdentifierGenerator { sessionIds.removeFirst() },
			clock = clock,
			capacity = SessionRuntimeCapacity(
				maxActiveSessions = 2,
				maxRecentSessions = 1,
				recentSessionTtl = Duration.ofMinutes(15),
			),
		)
		val first = runtime.create(sessionInitialState())
		val second = runtime.create(sessionInitialState())
		runtime.terminate(
			first.sessionId,
			TerminationCommand("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", 0, "第一场结束"),
		)
		clock.advance(Duration.ofMinutes(1))
		val retained = runtime.terminate(
			second.sessionId,
			TerminationCommand("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb", 0, "第二场结束"),
		).session

		assertFailsWith<BattleSessionNotFoundException> { runtime.get(first.sessionId) }
		assertEquals(retained, runtime.get(second.sessionId))

		clock.advance(Duration.ofMinutes(15))
		assertFailsWith<BattleSessionNotFoundException> { runtime.get(second.sessionId) }
	}

	@Test
	fun `recent capacity evicts the session with the earliest endedAt even when terminalization arrives out of order`() {
		val clock = MutableTestClock(Instant.parse("2026-07-11T00:00:00Z"))
		val sessionIds = ArrayDeque(
			listOf(
				"11111111-1111-4111-8111-111111111111",
				"22222222-2222-4222-8222-222222222222",
			),
		)
		val runtime = BattleSessionRuntime(
			identifierGenerator = SessionIdentifierGenerator { sessionIds.removeFirst() },
			clock = clock,
			capacity = SessionRuntimeCapacity(maxActiveSessions = 2, maxRecentSessions = 1),
		)
		val first = runtime.create(sessionInitialState())
		val second = runtime.create(sessionInitialState())
		clock.set(Instant.parse("2026-07-11T00:10:00Z"))
		val newer = runtime.terminate(
			first.sessionId,
			TerminationCommand("cccccccc-cccc-4ccc-8ccc-cccccccccccc", 0, "newer"),
		).session
		clock.set(Instant.parse("2026-07-11T00:05:00Z"))
		runtime.terminate(
			second.sessionId,
			TerminationCommand("dddddddd-dddd-4ddd-8ddd-dddddddddddd", 0, "older"),
		)

		assertEquals(newer, runtime.get(first.sessionId))
		assertFailsWith<BattleSessionNotFoundException> { runtime.get(second.sessionId) }
	}

	@Test
	fun `recent capacity uses sessionId as a stable tie break for equal endedAt`() {
		val clock = MutableTestClock(Instant.parse("2026-07-11T00:00:00Z"))
		val sessionIds = ArrayDeque(
			listOf(
				"22222222-2222-4222-8222-222222222222",
				"11111111-1111-4111-8111-111111111111",
			),
		)
		val runtime = BattleSessionRuntime(
			identifierGenerator = SessionIdentifierGenerator { sessionIds.removeFirst() },
			clock = clock,
			capacity = SessionRuntimeCapacity(maxActiveSessions = 2, maxRecentSessions = 1),
		)
		val lexicographicallyLater = runtime.create(sessionInitialState())
		val lexicographicallyEarlier = runtime.create(sessionInitialState())
		val retained = runtime.terminate(
			lexicographicallyLater.sessionId,
			TerminationCommand("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee", 0, "same time"),
		).session
		runtime.terminate(
			lexicographicallyEarlier.sessionId,
			TerminationCommand("ffffffff-ffff-4fff-8fff-ffffffffffff", 0, "same time"),
		)

		assertEquals(retained, runtime.get(lexicographicallyLater.sessionId))
		assertFailsWith<BattleSessionNotFoundException> { runtime.get(lexicographicallyEarlier.sessionId) }
	}
}
