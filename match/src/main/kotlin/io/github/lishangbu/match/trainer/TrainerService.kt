package io.github.lishangbu.match.trainer

import java.time.Clock
import java.time.Instant
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.entity.id as securityUserId
import io.github.lishangbu.match.challenge.ChallengeCancellationReason
import io.github.lishangbu.match.challenge.ChallengeStatus
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.isNotNull
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.babyfish.jimmer.sql.kt.ast.expression.plus
import org.babyfish.jimmer.sql.kt.ast.expression.sql

/**
 * Trainer 生命周期应用服务。
 *
 * 查询与写入遵循项目统一的持久化边界；账户级事务锁用于串行化三 Trainer 上限、归档及未来 Match 账户保留。
 */
open class TrainerService(
	private val repository: MatchTrainerRepository,
	private val sqlClient: KSqlClient,
	private val clock: Clock = Clock.systemUTC(),
	private val sessions: TrainerSessionRegistry = TrainerSessionRegistry(),
) {
	@Transactional
	open fun create(accountId: Long, command: CreateTrainerCommand): TrainerRecord {
		lockAccount(accountId)
		val name = TrainerDisplayName.of(command.displayName)
		findByCommand(accountId, command.commandId.toString())?.let { existing ->
			if (existing.displayNameKey != name.key) throw TrainerCommandPayloadConflictException()
			return existing
		}
		if (countActive(accountId) >= 3) throw TrainerLimitExceededException()
		if (enabledSensitiveNameRules().any { it.matches(name.moderationKey) }) {
			throw SensitiveTrainerDisplayNameException()
		}
		return repository.save(MatchTrainer {
			this.accountId = accountId
			displayName = name.value
			displayNameKey = name.key
			commandId = command.commandId.toString()
			revision = 0
			archivedAt = null
		}, SaveMode.INSERT_ONLY).toRecord()
	}

	open fun list(accountId: Long): List<TrainerRecord> = list(accountId, archived = false)
	open fun listArchived(accountId: Long): List<TrainerRecord> = list(accountId, archived = true)

	@Transactional
	open fun archive(accountId: Long, trainerId: Long, expectedRevision: Long): TrainerRecord {
		lockAccount(accountId)
		sessions.reserveArchive(accountId, trainerId)
		val transactionOwnsReservation = releaseArchiveAfterTransaction(accountId)
		try {
			if (hasBlockingActivity(accountId, trainerId)) throw TrainerArchiveBlockedException()
			cancelPendingChallenges(trainerId)
			return updateArchive(accountId, trainerId, expectedRevision, Instant.now(clock))
				?: throw TrainerRevisionConflictException()
		} finally {
			if (!transactionOwnsReservation) sessions.releaseArchive(accountId)
		}
	}

	@Transactional
	open fun restore(accountId: Long, trainerId: Long, expectedRevision: Long): TrainerRecord {
		lockAccount(accountId)
		if (countActive(accountId) >= 3) throw TrainerLimitExceededException()
		return updateArchive(accountId, trainerId, expectedRevision, null) ?: throw TrainerRevisionConflictException()
	}

	private fun lockAccount(accountId: Long) {
		sqlClient.createQuery(SecurityUser::class) {
			where(table.securityUserId eq accountId)
			select(sql<String>("cast(pg_advisory_xact_lock(%v) as text)") { value(accountId) })
		}.execute()
	}

	private fun findByCommand(accountId: Long, commandId: String): TrainerRecord? =
		sqlClient.createQuery(MatchTrainer::class) {
			where(table.accountId eq accountId, table.commandId eq commandId)
			select(table)
		}.execute().singleOrNull()?.toRecord()

	internal open fun findById(accountId: Long, trainerId: Long): TrainerRecord? =
		sqlClient.createQuery(MatchTrainer::class) {
			where(table.accountId eq accountId, table.id eq trainerId)
			select(table)
		}.execute().singleOrNull()?.toRecord()

	private fun list(accountId: Long, archived: Boolean): List<TrainerRecord> =
		sqlClient.createQuery(MatchTrainer::class) {
			where(table.accountId eq accountId)
			where(if (archived) table.archivedAt.isNotNull() else table.archivedAt.isNull())
			orderBy(table.createdAt, table.id)
			select(table)
		}.execute().map(MatchTrainer::toRecord)

	private fun countActive(accountId: Long): Int = sqlClient.createQuery(MatchTrainer::class) {
		where(table.accountId eq accountId, table.archivedAt.isNull())
		orderBy(table.id)
		select(table.id)
	}.limit(3).execute().size

	private fun enabledSensitiveNameRules(): List<SensitiveNameRule> =
		sqlClient.createQuery(MatchSensitiveNameRule::class) {
			where(table.enabled eq true)
			orderBy(table.id)
			select(table)
		}.execute().map { SensitiveNameRule(it.normalizedTerm, it.matchType) }

	internal open fun findActiveMatchTrainerId(accountId: Long): Long? {
		val matchId = sqlClient.createQuery(MatchActiveAccountReservation::class) {
			where(table.accountId eq accountId)
			select(table.matchId)
		}.execute().singleOrNull() ?: return null
		return sqlClient.createQuery(MatchParticipant::class) {
			where(table.accountId eq accountId, table.id.matchId eq matchId)
			select(table.id.trainerId)
		}.execute().singleOrNull()
	}

	private fun hasBlockingActivity(accountId: Long, trainerId: Long): Boolean =
		findActiveMatchTrainerId(accountId) == trainerId

	private fun cancelPendingChallenges(trainerId: Long) {
		val now = Instant.now(clock)
		sqlClient.createUpdate(MatchChallenge::class) {
			where(table.status eq ChallengeStatus.PENDING, or(table.challengerTrainerId eq trainerId, table.challengedTrainerId eq trainerId))
			set(table.status, ChallengeStatus.CANCELLED)
			set(table.cancellationReason, ChallengeCancellationReason.TRAINER_ARCHIVED)
			set(table.resolvedAt, now)
			set(table.revision, table.revision + 1)
			set(table.updatedAt, now)
		}.execute()
	}

	private fun updateArchive(accountId: Long, trainerId: Long, expectedRevision: Long, archivedAt: Instant?): TrainerRecord? {
		val changed = sqlClient.createUpdate(MatchTrainer::class) {
			where(table.accountId eq accountId, table.id eq trainerId, table.revision eq expectedRevision)
			where(if (archivedAt == null) table.archivedAt.isNotNull() else table.archivedAt.isNull())
			set(table.archivedAt, archivedAt)
			set(table.revision, table.revision + 1)
		}.execute()
		return if (changed == 1) findById(accountId, trainerId) else null
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

internal fun MatchTrainer.toRecord() = TrainerRecord(id, accountId, displayName, displayNameKey, commandId, revision, archivedAt)
