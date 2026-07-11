package io.github.lishangbu.match.trainer

import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

data class TrainerSelection(val accountId: Long, val trainerId: Long, val activeMatchTrainerId: Long? = null)
data class TrainerSession(val accountId: Long, val trainerId: Long, val credential: String, val expiresAt: Instant)

class TrainerSessionRegistry(
	private val idleTimeout: Duration = Duration.ofMinutes(30),
	private val credentialGenerator: (TrainerSelection) -> String = { randomCredential() },
) {
	private val byCredential = ConcurrentHashMap<String, TrainerSession>()
	private val credentialByAccount = ConcurrentHashMap<Long, String>()

	@Synchronized
	fun enter(selection: TrainerSelection, now: Instant): TrainerSession {
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
		return refreshed
	}

	@Synchronized
	fun leave(accountId: Long) {
		removeAccountSession(accountId)
	}

	private fun removeAccountSession(accountId: Long) {
		credentialByAccount.remove(accountId)?.let(byCredential::remove)
	}

	private fun removeSession(session: TrainerSession) {
		byCredential.remove(session.credential, session)
		credentialByAccount.remove(session.accountId, session.credential)
	}

	private companion object {
		val random = SecureRandom()
		fun randomCredential(): String = ByteArray(32).also(random::nextBytes).let {
			Base64.getUrlEncoder().withoutPadding().encodeToString(it)
		}
	}
}

class TrainerSwitchBlockedException : IllegalStateException("Active match trainer must be restored")
