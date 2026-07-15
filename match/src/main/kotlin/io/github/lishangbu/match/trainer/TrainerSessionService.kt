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
	open fun enter(accountId: Long, trainerId: Long, loginToken: String? = null): TrainerSessionView {
		val trainer = trainers.findById(accountId, trainerId)
			?.takeIf { it.archivedAt == null }
			?: throw TrainerUnavailableException()
		val selection = TrainerSelection(accountId, trainerId, trainers.findActiveMatchTrainerId(accountId))
		return TrainerSessionView(sessions.enter(selection, Instant.now(clock), loginToken), trainer)
	}

	open fun current(accountId: Long, credential: String): TrainerSessionView {
		val session = sessions.authenticate(accountId, credential, Instant.now(clock))
			?: throw InvalidTrainerSessionException()
		val trainer = trainers.findById(accountId, session.trainerId)
			?.takeIf { it.archivedAt == null }
			?: throw InvalidTrainerSessionException()
		return TrainerSessionView(session, trainer)
	}

	/** 心跳认证只维持在线状态，不把后台定时器伪装成用户活动来延长 Session。 */
	open fun heartbeat(accountId: Long, credential: String) {
		val session = sessions.heartbeat(accountId, credential, Instant.now(clock))
			?: throw InvalidTrainerSessionException()
		trainers.findById(accountId, session.trainerId)
			?.takeIf { it.archivedAt == null }
			?: throw InvalidTrainerSessionException()
	}

	open fun leave(accountId: Long, credential: String) = sessions.leave(accountId, credential)
}
