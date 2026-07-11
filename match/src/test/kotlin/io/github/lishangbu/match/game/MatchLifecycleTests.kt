package io.github.lishangbu.match.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MatchLifecycleTests {
	@Test
	fun `starting match can become active then complete`() {
		val active = MatchLifecycle.starting().activate("session-1")
		val completed = active.complete(MatchResult.win(11L, MatchCompletionReason.FORFEIT))

		assertEquals(MatchStatus.COMPLETED, completed.status)
		assertEquals(11L, completed.result?.winnerTrainerId)
		assertEquals(2, completed.revision)
	}

	@Test
	fun `terminal match rejects a second decision`() {
		val interrupted = MatchLifecycle.starting().interrupt(MatchInterruptionReason.START_FAILED)
		assertFailsWith<MatchAlreadyTerminalException> {
			interrupted.interrupt(MatchInterruptionReason.RUNTIME_FAILED)
		}
	}
}
