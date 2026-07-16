package io.github.lishangbu.match.challenge

import io.github.lishangbu.match.trainer.*
import io.github.lishangbu.match.event.PlayerEventPublisher
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 私人 Challenge 聚合服务。
 *
 * 发起事务锁定双方 Trainer，冻结发起方当前 Team，并让 commandId 幂等与双向 Pending 唯一约束共享同一并发边界。
 */
open class ChallengeService(
	private val challenges: MatchChallengeRepository,
	private val snapshots: MatchTeamSnapshotRepository,
	private val trainers: TrainerService,
	private val teams: TrainerTeamService,
	private val presence: TrainerSessionRegistry,
	private val sqlClient: KSqlClient,
	private val events: PlayerEventPublisher,
	private val clock: Clock = Clock.systemUTC(),
) {
	@Transactional
	open fun create(accountId: Long, trainerId: Long, request: CreateChallengeRequest): ChallengeResponse {
		val now = Instant.now(clock)
		val commandId = try {
			UUID.fromString(request.commandId).toString()
		} catch (_: IllegalArgumentException) {
			throw invalid("challenge.command-id.invalid")
		}
		val targetName = try {
			TrainerDisplayName.of(request.challengedDisplayName)
		} catch (_: InvalidTrainerDisplayNameException) {
			throw unavailable()
		}
		val teamId = request.teamId.toLongOrNull()?.takeIf { it > 0 } ?: throw invalid("challenge.team.invalid")
		findExisting(accountId, trainerId, commandId, targetName.key, teamId, now)?.let { return it }
		val target = trainers.findPublicByDisplayNameKey(targetName.key) ?: throw unavailable()
		if (target.accountId == accountId || target.id == trainerId) throw unavailable()
		lockTrainers(trainerId, target.id)
		findExisting(accountId, trainerId, commandId, targetName.key, teamId, now)?.let { return it }

		expirePendingBetween(trainerId, target.id, now)
		if (!presence.isOnline(target.id, now) || trainers.hasActiveMatch(accountId) || trainers.hasActiveMatch(target.accountId)) {
			throw conflict("challenge.target-unavailable")
		}
		if (findPendingBetween(trainerId, target.id) != null) throw conflict("challenge.pending-exists")
		val team = try {
			teams.get(trainerId, teamId)
		} catch (_: TrainerTeamRequestException) {
			throw invalid("challenge.team.invalid")
		}

		val snapshot = snapshots.save(MatchTeamSnapshot {
			this.trainerId = trainerId
			sourceTeamId = team.id
			schemaVersion = SNAPSHOT_SCHEMA_VERSION
			roster = team.toSnapshot(0)
		}, SaveMode.INSERT_ONLY)
		val challenge = challenges.save(MatchChallenge {
			this.commandId = commandId
			challengerTrainerId = trainerId
			challengedTrainerId = target.id
			challengerDisplayName = findTrainer(trainerId)?.displayName ?: error("Current Trainer is missing")
			challengedDisplayName = target.displayName
			challengerAccountId = accountId
			challengedAccountId = target.accountId
			challengerSnapshotId = snapshot.id
			challengedSnapshotId = null
			ruleCode = STANDARD_SINGLE
			status = ChallengeStatus.PENDING
			cancellationReason = null
			revision = 0
			expiresAt = now.plus(CHALLENGE_TTL)
			resolvedAt = null
		}, SaveMode.INSERT_ONLY)
		return toResponse(findChallenge(challenge.id) ?: error("Created Challenge is missing"), trainerId)
	}

	@Transactional
	open fun list(trainerId: Long): List<ChallengeResponse> {
		expireVisible(trainerId, Instant.now(clock))
		return toResponses(findVisible(trainerId), trainerId)
	}

	@Transactional
	open fun find(trainerId: Long, challengeId: Long): ChallengeResponse {
		expireOne(challengeId, Instant.now(clock))
		return toResponse(requireVisible(trainerId, challengeId), trainerId)
	}

	@Transactional
	open fun reject(trainerId: Long, challengeId: Long, expectedRevision: Long): ChallengeResponse =
		resolve(trainerId, challengeId, expectedRevision, ChallengeStatus.REJECTED, challenged = true)

	@Transactional
	open fun withdraw(trainerId: Long, challengeId: Long, expectedRevision: Long): ChallengeResponse =
		resolve(trainerId, challengeId, expectedRevision, ChallengeStatus.CANCELLED, challenged = false)

	private fun resolve(
		trainerId: Long,
		challengeId: Long,
		expectedRevision: Long,
		status: ChallengeStatus,
		challenged: Boolean,
	): ChallengeResponse {
		val now = Instant.now(clock)
		expireOne(challengeId, now)
		val current = requireVisible(trainerId, challengeId)
		if (current.status != ChallengeStatus.PENDING) throw conflict("challenge.already-resolved")
		val ownsRole = if (challenged) current.challengedTrainerId == trainerId else current.challengerTrainerId == trainerId
		if (!ownsRole) throw ChallengeRequestException(HttpStatus.FORBIDDEN, "challenge.action-forbidden")
		val changed = sqlClient.createUpdate(MatchChallenge::class) {
			where(table.id eq challengeId, table.status eq ChallengeStatus.PENDING, table.revision eq expectedRevision)
			set(table.status, status)
			set(table.cancellationReason, if (status == ChallengeStatus.CANCELLED) ChallengeCancellationReason.WITHDRAWN else null)
			set(table.resolvedAt, now)
			set(table.revision, table.revision + 1)
			set(table.updatedAt, now)
		}.execute()
		if (changed != 1) throw conflict("challenge.revision-conflict")
		return toResponse(requireVisible(trainerId, challengeId), trainerId)
	}

	private fun lockTrainers(firstId: Long, secondId: Long) {
		val ids = sqlClient.createQuery(MatchTrainer::class) {
			where(table.id valueIn listOf(firstId, secondId), table.archivedAt.isNull())
			orderBy(table.id)
			select(table.id)
		}.forUpdate().execute()
		if (ids.size != 2) throw unavailable()
	}

	private fun findByCommand(accountId: Long, commandId: String): MatchChallenge? =
		sqlClient.createQuery(MatchChallenge::class) {
			where(table.challengerAccountId eq accountId, table.commandId eq commandId)
			select(table)
		}.execute().singleOrNull()

	/** 幂等重放只比较原始命令载荷，不重新应用在线、归档或 Team 当前状态。 */
	private fun findExisting(
		accountId: Long,
		trainerId: Long,
		commandId: String,
		targetDisplayNameKey: String,
		sourceTeamId: Long,
		now: Instant,
	): ChallengeResponse? {
		val existing = findByCommand(accountId, commandId) ?: return null
		val snapshot = findSnapshot(existing.challengerSnapshotId)
		val frozenTargetKey = TrainerDisplayName.of(existing.challengedDisplayName).key
		if (existing.challengerTrainerId != trainerId || frozenTargetKey != targetDisplayNameKey ||
			snapshot?.sourceTeamId != sourceTeamId) {
			throw conflict("challenge.command-payload-conflict")
		}
		if (existing.status == ChallengeStatus.PENDING && !now.isBefore(existing.expiresAt)) expire(existing, now)
		return toResponse(findChallenge(existing.id) ?: error("Idempotent Challenge is missing"), trainerId)
	}

	private fun findPendingBetween(firstId: Long, secondId: Long): MatchChallenge? =
		sqlClient.createQuery(MatchChallenge::class) {
			where(table.status eq ChallengeStatus.PENDING)
			where(or(
				and(table.challengerTrainerId eq firstId, table.challengedTrainerId eq secondId),
				and(table.challengerTrainerId eq secondId, table.challengedTrainerId eq firstId),
			))
			select(table)
		}.execute().singleOrNull()

	private fun expirePendingBetween(firstId: Long, secondId: Long, now: Instant) {
		findPendingBetween(firstId, secondId)?.takeIf { !now.isBefore(it.expiresAt) }?.let { expire(it, now) }
	}

	private fun expireVisible(trainerId: Long, now: Instant) {
		findVisible(trainerId).filter { it.status == ChallengeStatus.PENDING && !now.isBefore(it.expiresAt) }
			.forEach { expire(it, now) }
	}

	private fun expireOne(challengeId: Long, now: Instant) {
		findChallenge(challengeId)?.takeIf { it.status == ChallengeStatus.PENDING && !now.isBefore(it.expiresAt) }
			?.let { expire(it, now) }
	}

	private fun expire(challenge: MatchChallenge, now: Instant) {
		val changed = sqlClient.createUpdate(MatchChallenge::class) {
			where(table.id eq challenge.id, table.status eq ChallengeStatus.PENDING, table.revision eq challenge.revision)
			set(table.status, ChallengeStatus.EXPIRED)
			set(table.resolvedAt, now)
			set(table.revision, table.revision + 1)
			set(table.updatedAt, now)
		}.execute()
		if (changed == 1) events.challengeChanged(challenge.id, challenge.revision + 1)
	}

	private fun findVisible(trainerId: Long): List<MatchChallenge> = sqlClient.createQuery(MatchChallenge::class) {
		where(or(table.challengerTrainerId eq trainerId, table.challengedTrainerId eq trainerId))
		orderBy(table.createdAt.desc(), table.id.desc())
		select(table)
	}.execute()

	private fun requireVisible(trainerId: Long, challengeId: Long): MatchChallenge =
		findChallenge(challengeId)?.takeIf { it.challengerTrainerId == trainerId || it.challengedTrainerId == trainerId }
			?: throw ChallengeRequestException(HttpStatus.NOT_FOUND, "challenge.not-found")

	private fun findChallenge(id: Long): MatchChallenge? = sqlClient.createQuery(MatchChallenge::class) {
		where(table.id eq id)
		select(table)
	}.execute().singleOrNull()

	private fun findSnapshot(id: Long): MatchTeamSnapshot? = sqlClient.createQuery(MatchTeamSnapshot::class) {
		where(table.id eq id)
		select(table)
	}.execute().singleOrNull()

	private fun findTrainer(id: Long): MatchTrainer? = sqlClient.createQuery(MatchTrainer::class) {
		where(table.id eq id)
		select(table)
	}.execute().singleOrNull()

	private fun toResponse(challenge: MatchChallenge, viewerTrainerId: Long): ChallengeResponse =
		toResponses(listOf(challenge), viewerTrainerId).single()

	/** 列表映射批量读取关联名称和 Snapshot，避免随 Challenge 数量增长的 N+1 查询。 */
	private fun toResponses(challenges: List<MatchChallenge>, viewerTrainerId: Long): List<ChallengeResponse> {
		if (challenges.isEmpty()) return emptyList()
		val snapshotsById = sqlClient.createQuery(MatchTeamSnapshot::class) {
			where(table.id valueIn challenges.map(MatchChallenge::challengerSnapshotId).toSet())
			select(table)
		}.execute().associateBy(MatchTeamSnapshot::id)
		return challenges.map { challenge ->
			val teamSize = snapshotsById[challenge.challengerSnapshotId]?.roster?.members?.size
				?: throw IllegalStateException("Challenge snapshot is missing")
			ChallengeResponse {
				id = challenge.id
				direction = if (challenge.challengerTrainerId == viewerTrainerId) ChallengeDirection.OUTGOING else ChallengeDirection.INCOMING
				challengerDisplayName = challenge.challengerDisplayName
				challengedDisplayName = challenge.challengedDisplayName
				ruleCode = challenge.ruleCode
				this.teamSize = teamSize
				status = challenge.status
				cancellationReason = challenge.cancellationReason
				revision = challenge.revision
				expiresAt = challenge.expiresAt
				resolvedAt = challenge.resolvedAt
				createdAt = challenge.createdAt
			}
		}
	}

	private fun TrainerTeamRecord.toSnapshot(leadPosition: Int) = TrainerTeamSnapshotRoster(
		leadPosition,
		members.map { member ->
			TrainerTeamSnapshotMember(
				creatureId = member.creatureId,
				skinId = member.skinId,
				skillIds = member.skillIds,
				abilityId = member.abilityId,
				itemId = member.itemId,
				natureId = member.natureId,
				teraElementId = member.teraElementId,
				level = MATCH_LEVEL,
				individualValues = member.individualValues,
				effortValues = member.effortValues,
			)
		},
	)

	private fun unavailable() = ChallengeRequestException(HttpStatus.NOT_FOUND, "challenge.target-unavailable")
	private fun invalid(code: String) = ChallengeRequestException(HttpStatus.UNPROCESSABLE_ENTITY, code)
	private fun conflict(code: String) = ChallengeRequestException(HttpStatus.CONFLICT, code)

	private companion object {
		const val SNAPSHOT_SCHEMA_VERSION = 1
		const val MATCH_LEVEL = 50
		const val STANDARD_SINGLE = "standard-single"
		val CHALLENGE_TTL: Duration = Duration.ofMinutes(5)
	}
}
