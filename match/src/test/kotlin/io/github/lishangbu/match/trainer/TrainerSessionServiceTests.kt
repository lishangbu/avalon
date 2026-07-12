package io.github.lishangbu.match.trainer

import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** 验证持久 Trainer 与单账户内存 Session 组合后的身份切换边界。 */
class TrainerSessionServiceTests {
	@Test
	fun `enters active owned trainer and replaces previous session`() {
		val trainers = mock(TrainerService::class.java)
		`when`(trainers.findById(1, 11)).thenReturn(trainer(11, 1, "One"))
		`when`(trainers.findById(1, 12)).thenReturn(trainer(12, 1, "Two"))
		`when`(trainers.findActiveMatchTrainerId(1)).thenReturn(null)
		val registry = TrainerSessionRegistry(credentialGenerator = { "credential-${it.trainerId}" })
		val service = TrainerSessionService(trainers, registry, fixedClock())

		val first = service.enter(1, 11)
		val second = service.enter(1, 12)

		assertEquals("credential-11", first.session.credential)
		assertFailsWith<InvalidTrainerSessionException> { service.current(1, first.session.credential) }
		assertEquals(12L, service.current(1, second.session.credential).trainer.id)
	}

	@Test
	fun `rejects archived foreign and active match trainer switches`() {
		val trainers = mock(TrainerService::class.java)
		`when`(trainers.findById(1, 11)).thenReturn(trainer(11, 1, "One", archivedAt = Instant.EPOCH))
		`when`(trainers.findById(1, 12)).thenReturn(null)
		`when`(trainers.findById(1, 13)).thenReturn(trainer(13, 1, "Current"))
		`when`(trainers.findById(1, 14)).thenReturn(trainer(14, 1, "Other"))
		`when`(trainers.findActiveMatchTrainerId(1)).thenReturn(13)
		val service = TrainerSessionService(trainers, TrainerSessionRegistry(), fixedClock())

		assertFailsWith<TrainerUnavailableException> { service.enter(1, 11) }
		assertFailsWith<TrainerUnavailableException> { service.enter(1, 12) }
		assertFailsWith<TrainerSwitchBlockedException> { service.enter(1, 14) }
		assertEquals(13L, service.enter(1, 13).trainer.id)
	}

	private fun trainer(id: Long, accountId: Long, name: String, archivedAt: Instant? = null) =
		TrainerRecord(id, accountId, name, name.lowercase(), "command-$id", 0, archivedAt)

	private fun fixedClock() = Clock.fixed(Instant.parse("2026-07-12T00:00:00Z"), ZoneOffset.UTC)
}
