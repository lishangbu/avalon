package io.github.lishangbu.match.trainer

import java.time.Clock
import java.time.Instant

/** 组合公开资料、短时 Presence 与 Match 占用状态，不向调用方泄露具体不可挑战原因。 */
open class PublicTrainerService(
	private val trainers: TrainerService,
	private val sessions: TrainerSessionService,
	private val presence: TrainerSessionRegistry,
	private val clock: Clock = Clock.systemUTC(),
) {
	open fun find(accountId: Long, credential: String, displayName: String): PublicTrainerProfile {
		val current = sessions.current(accountId, credential)
		val name = try {
			TrainerDisplayName.of(displayName)
		} catch (_: InvalidTrainerDisplayNameException) {
			throw PublicTrainerNotFoundException()
		}
		val target = trainers.findPublicByDisplayNameKey(name.key) ?: throw PublicTrainerNotFoundException()
		val online = presence.isOnline(target.id, Instant.now(clock))
		val challengeable = online &&
			target.accountId != current.session.accountId &&
			!trainers.hasActiveMatch(current.session.accountId) &&
			!trainers.hasActiveMatch(target.accountId)
		return PublicTrainerProfile(target.displayName, online, challengeable)
	}
}
