package io.github.lishangbu.match.trainer

import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/** 单节点内存中的账户级 Trainer Session 注册表。 */
class TrainerSessionRegistry(
	private val idleTimeout: Duration = Duration.ofMinutes(30),
	private val presenceTimeout: Duration = Duration.ofSeconds(45),
	private val credentialGenerator: (TrainerSelection) -> String = { randomCredential() },
) {
	private val byCredential = ConcurrentHashMap<String, TrainerSession>()
	private val credentialByAccount = ConcurrentHashMap<Long, String>()
	private val archiveReservations = mutableSetOf<Long>()
	private val lastSeenByTrainer = ConcurrentHashMap<Long, Instant>()

	@Synchronized
	fun enter(selection: TrainerSelection, now: Instant): TrainerSession {
		if (selection.accountId in archiveReservations) throw TrainerSessionEntryBlockedException()
		if (selection.activeMatchTrainerId != null && selection.activeMatchTrainerId != selection.trainerId) {
			throw TrainerSwitchBlockedException()
		}
		removeAccountSession(selection.accountId)
		val session = TrainerSession(
			selection.accountId,
			selection.trainerId,
			credentialGenerator(selection),
			now.plus(idleTimeout),
		)
		byCredential[session.credential] = session
		credentialByAccount[session.accountId] = session.credential
		lastSeenByTrainer[session.trainerId] = now
		return session
	}

	@Synchronized
	fun authenticate(credential: String, now: Instant): TrainerSession? {
		val current = byCredential[credential] ?: return null
		if (!now.isBefore(current.expiresAt)) {
			removeSession(current)
			return null
		}
		val refreshed = current.copy(expiresAt = now.plus(idleTimeout))
		byCredential[credential] = refreshed
		lastSeenByTrainer[refreshed.trainerId] = now
		return refreshed
	}

	@Synchronized
	fun authenticate(accountId: Long, credential: String, now: Instant): TrainerSession? {
		val current = byCredential[credential] ?: return null
		if (current.accountId != accountId) return null
		return authenticate(credential, now)
	}

	/** 认证心跳只刷新 Presence，不滑动 Session 的空闲到期时间。 */
	@Synchronized
	fun heartbeat(accountId: Long, credential: String, now: Instant): TrainerSession? {
		val current = byCredential[credential] ?: return null
		if (current.accountId != accountId) return null
		if (!now.isBefore(current.expiresAt)) {
			removeSession(current)
			return null
		}
		lastSeenByTrainer[current.trainerId] = now
		return current
	}

	@Synchronized
	fun leave(accountId: Long) {
		removeAccountSession(accountId)
	}

	@Synchronized
	fun leave(accountId: Long, credential: String) {
		val current = byCredential[credential] ?: return
		if (current.accountId == accountId) removeSession(current)
	}

	@Synchronized
	fun isCurrent(accountId: Long, trainerId: Long): Boolean =
		credentialByAccount[accountId]?.let(byCredential::get)?.trainerId == trainerId

	/** Presence 是短时在线信号，不参与持久化；独立心跳不会延长 Trainer Session。 */
	@Synchronized
	fun isOnline(trainerId: Long, now: Instant): Boolean =
		lastSeenByTrainer[trainerId]?.let { now.isBefore(it.plus(presenceTimeout)) } == true

	@Synchronized
	fun reserveArchive(accountId: Long, trainerId: Long) {
		if (isCurrent(accountId, trainerId) || !archiveReservations.add(accountId)) {
			throw TrainerArchiveBlockedException()
		}
	}

	@Synchronized
	fun releaseArchive(accountId: Long) {
		archiveReservations.remove(accountId)
	}

	private fun removeAccountSession(accountId: Long) {
		credentialByAccount[accountId]?.let(byCredential::get)?.let(::removeSession)
	}

	private fun removeSession(session: TrainerSession) {
		byCredential.remove(session.credential, session)
		credentialByAccount.remove(session.accountId, session.credential)
		lastSeenByTrainer.remove(session.trainerId)
	}

	private companion object {
		val random = SecureRandom()
		fun randomCredential(): String = ByteArray(32).also(random::nextBytes).let {
			Base64.getUrlEncoder().withoutPadding().encodeToString(it)
		}
	}
}
