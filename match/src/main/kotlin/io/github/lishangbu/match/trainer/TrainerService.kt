package io.github.lishangbu.match.trainer

import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

open class TrainerService(
	private val store: TrainerStore,
	private val clock: Clock = Clock.systemUTC(),
	private val idGenerator: () -> Long = { SecureRandom().nextLong().ushr(1) },
	private val sessions: TrainerSessionRegistry = TrainerSessionRegistry(),
) {
	@Transactional
	open fun create(accountId: Long, command: CreateTrainerCommand): TrainerRecord {
		store.lockAccount(accountId)
		val name = TrainerDisplayName.of(command.displayName)
		store.findByCommand(accountId, command.commandId.toString())?.let { existing ->
			if (existing.displayNameKey != name.key) throw TrainerCommandPayloadConflictException()
			return existing
		}
		if (store.countActive(accountId) >= 3) throw TrainerLimitExceededException()
		if (store.enabledSensitiveNameRules().any { it.matches(name.moderationKey) }) {
			throw SensitiveTrainerDisplayNameException()
		}
		return store.insert(
			TrainerRecord(idGenerator(), accountId, name.value, name.key, command.commandId.toString(), 0, null),
		)
	}

	open fun list(accountId: Long): List<TrainerRecord> = store.list(accountId).filter { it.archivedAt == null }
	open fun listArchived(accountId: Long): List<TrainerRecord> = store.list(accountId).filter { it.archivedAt != null }

	@Transactional
	open fun archive(accountId: Long, trainerId: Long, expectedRevision: Long): TrainerRecord {
		store.lockAccount(accountId)
		sessions.reserveArchive(accountId, trainerId)
		val transactionOwnsReservation = releaseArchiveAfterTransaction(accountId)
		try {
			if (store.hasBlockingActivity(accountId, trainerId)) throw TrainerArchiveBlockedException()
			return store.archive(accountId, trainerId, expectedRevision, Instant.now(clock))
				?: throw TrainerRevisionConflictException()
		} finally {
			if (!transactionOwnsReservation) sessions.releaseArchive(accountId)
		}
	}

	@Transactional
	open fun restore(accountId: Long, trainerId: Long, expectedRevision: Long): TrainerRecord {
		store.lockAccount(accountId)
		if (store.countActive(accountId) >= 3) throw TrainerLimitExceededException()
		return store.restore(accountId, trainerId, expectedRevision) ?: throw TrainerRevisionConflictException()
	}

	private fun SensitiveNameRule.matches(key: String): Boolean = when (matchType) {
		SensitiveNameMatchType.EXACT -> key == normalizedTerm
		SensitiveNameMatchType.CONTAINS -> key.contains(normalizedTerm)
	}

	private fun releaseArchiveAfterTransaction(accountId: Long): Boolean {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) return false
		TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
			override fun afterCompletion(status: Int) = sessions.releaseArchive(accountId)
		})
		return true
	}
}

class TrainerCommandPayloadConflictException : RuntimeException()
class TrainerLimitExceededException : RuntimeException()
class SensitiveTrainerDisplayNameException : RuntimeException()
class TrainerRevisionConflictException : RuntimeException()
class TrainerArchiveBlockedException : RuntimeException()
