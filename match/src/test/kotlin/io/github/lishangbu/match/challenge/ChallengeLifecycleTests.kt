package io.github.lishangbu.match.challenge

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChallengeLifecycleTests {
	@Test
	fun `pending challenge transitions once`() {
		val challenge = ChallengeLifecycle.pending(Instant.parse("2026-07-11T12:00:00Z"))

		val accepted = challenge.resolve(ChallengeStatus.ACCEPTED, Instant.parse("2026-07-11T12:01:00Z"))

		assertEquals(ChallengeStatus.ACCEPTED, accepted.status)
		assertEquals(1, accepted.revision)
		assertFailsWith<ChallengeAlreadyResolvedException> {
			accepted.resolve(ChallengeStatus.REJECTED, Instant.parse("2026-07-11T12:02:00Z"))
		}
	}

	@Test
	fun `cancelled challenge requires an allowed reason`() {
		val challenge = ChallengeLifecycle.pending(Instant.parse("2026-07-11T12:00:00Z"))
		assertFailsWith<InvalidChallengeTransitionException> {
			challenge.resolve(ChallengeStatus.CANCELLED, Instant.parse("2026-07-11T12:01:00Z"))
		}
	}
}
