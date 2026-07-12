package io.github.lishangbu.match.trainer

import java.time.Clock
import java.time.Instant
import org.springframework.transaction.annotation.Transactional

/** 将持久 Trainer 与内存 Session 组合成玩家身份边界的应用服务。 */
open class TrainerSessionService(
	private val trainers: TrainerService,
	private val sessions: TrainerSessionRegistry,
	private val clock: Clock = Clock.systemUTC(),
) {
	@Transactional
	open fun enter(accountId: Long, trainerId: Long): TrainerSessionView {
		val trainer = trainers.findById(accountId, trainerId)
			?.takeIf { it.archivedAt == null }
			?: throw TrainerUnavailableException()
		val selection = TrainerSelection(accountId, trainerId, trainers.findActiveMatchTrainerId(accountId))
		return TrainerSessionView(sessions.enter(selection, Instant.now(clock)), trainer)
	}

	open fun current(accountId: Long, credential: String): TrainerSessionView {
		val session = sessions.authenticate(accountId, credential, Instant.now(clock))
			?: throw InvalidTrainerSessionException()
		val trainer = trainers.findById(accountId, session.trainerId)
			?.takeIf { it.archivedAt == null }
			?: throw InvalidTrainerSessionException()
		return TrainerSessionView(session, trainer)
	}

	open fun leave(accountId: Long, credential: String) = sessions.leave(accountId, credential)
}
