package io.github.lishangbu.match.trainer

import java.time.Clock
import java.time.Instant
import org.springframework.transaction.annotation.Transactional

data class TrainerSessionView(val session: TrainerSession, val trainer: TrainerRecord)

open class TrainerSessionService(
	private val store: TrainerStore,
	private val sessions: TrainerSessionRegistry,
	private val clock: Clock = Clock.systemUTC(),
) {
	@Transactional
	open fun enter(accountId: Long, trainerId: Long): TrainerSessionView {
		store.lockAccount(accountId)
		val trainer = store.findById(accountId, trainerId)
			?.takeIf { it.archivedAt == null }
			?: throw TrainerUnavailableException()
		val selection = TrainerSelection(accountId, trainerId, store.findActiveMatchTrainerId(accountId))
		return TrainerSessionView(sessions.enter(selection, Instant.now(clock)), trainer)
	}

	open fun current(accountId: Long, credential: String): TrainerSessionView {
		val session = sessions.authenticate(accountId, credential, Instant.now(clock))
			?: throw InvalidTrainerSessionException()
		val trainer = store.findById(accountId, session.trainerId)
			?.takeIf { it.archivedAt == null }
			?: throw InvalidTrainerSessionException()
		return TrainerSessionView(session, trainer)
	}

	open fun leave(accountId: Long, credential: String) = sessions.leave(accountId, credential)
}

class TrainerUnavailableException : RuntimeException()
class InvalidTrainerSessionException : RuntimeException()
