package io.github.lishangbu.match.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MatchLifecycleTests {
	@Test
	fun `accepted match previews teams before runtime starts`() {
		val preview = MatchLifecycle.preview()
		val starting = preview.start()
		val active = starting.activate("session-1")

		assertEquals(MatchStatus.PREVIEW, preview.status)
		assertEquals(MatchStatus.STARTING, starting.status)
		assertEquals(MatchStatus.ACTIVE, active.status)
		assertEquals(2, active.revision)
	}

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
