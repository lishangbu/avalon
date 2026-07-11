package io.github.lishangbu.match.trainer

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TrainerSessionRegistryTests {
	@Test
	fun `entering again replaces the account session`() {
		val registry = TrainerSessionRegistry(Duration.ofMinutes(30)) { "credential-${it.trainerId}" }
		val now = Instant.parse("2026-07-11T12:00:00Z")

		val first = registry.enter(TrainerSelection(1, 11), now)
		val second = registry.enter(TrainerSelection(1, 12), now.plusSeconds(1))

		assertNull(registry.authenticate(first.credential, now.plusSeconds(2)))
		assertEquals(12L, registry.authenticate(second.credential, now.plusSeconds(2))?.trainerId)
	}

	@Test
	fun `active match only permits restoring its trainer`() {
		val registry = TrainerSessionRegistry(Duration.ofMinutes(30)) { "credential-${it.trainerId}" }
		assertFailsWith<TrainerSwitchBlockedException> {
			registry.enter(TrainerSelection(1, 12, activeMatchTrainerId = 11), Instant.EPOCH)
		}
	}

	@Test
	fun `authentication slides idle expiry`() {
		val registry = TrainerSessionRegistry(Duration.ofMinutes(30)) { "credential" }
		val session = registry.enter(TrainerSelection(1, 11), Instant.EPOCH)
		registry.authenticate(session.credential, Instant.EPOCH.plusSeconds(20 * 60))
		assertEquals(11L, registry.authenticate(session.credential, Instant.EPOCH.plusSeconds(40 * 60))?.trainerId)
		assertNull(registry.authenticate(session.credential, Instant.EPOCH.plusSeconds(71 * 60)))
	}
}
