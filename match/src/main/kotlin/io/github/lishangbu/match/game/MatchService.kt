package io.github.lishangbu.match.game

import io.github.lishangbu.gamedata.entity.GameNatures
import io.github.lishangbu.gamedata.entity.GameStat
import io.github.lishangbu.gamedata.entity.id as gameDataId
import io.github.lishangbu.gamedata.entity.code as gameDataCode
import io.github.lishangbu.match.challenge.*
import io.github.lishangbu.match.runtime.*
import io.github.lishangbu.match.trainer.*
import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.entity.id as securityUserId
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.springframework.http.HttpStatus
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** Challenge 接受、Match 持久化与 Battle Runtime 启动的单一应用边界。 */
open class MatchService(
	private val games: MatchGameRepository,
	private val participants: MatchParticipantRepository,
	private val reservations: MatchActiveAccountReservationRepository,
	private val snapshots: MatchTeamSnapshotRepository,
	private val teams: TrainerTeamService,
	private val presence: TrainerSessionRegistry,
	private val host: BattleSessionHost,
	private val sqlClient: KSqlClient,
	transactionManager: PlatformTransactionManager,
	private val clock: Clock = Clock.systemUTC(),
	private val chooseFirstSide: () -> Boolean = { SecureRandom().nextBoolean() },
) {
	private val transaction = TransactionTemplate(transactionManager)

	/**
	 * 数据库接受事务先完整提交，再同步启动内存 Runtime。
	 * Runtime 启动失败不会抹掉已接受事实，而是另一个事务收敛为 INTERRUPTED / START_FAILED。
	 */
	open fun accept(accountId: Long, trainerId: Long, challengeId: Long, request: AcceptChallengeRequest): MatchResponse {
		val outcome = transaction.execute { persistAcceptance(accountId, trainerId, challengeId, request) }
		if (outcome is AcceptanceOutcome.Failed) throw outcome.error
		val plan = (outcome as AcceptanceOutcome.Ready).plan
		var sessionId: String? = null
		return try {
			val startedSessionId = host.start(plan.roster)
			sessionId = startedSessionId
			transaction.execute { activate(plan.matchId, startedSessionId, plan.viewerTrainerId) }
		} catch (error: RuntimeException) {
			sessionId?.let { startedId -> runCatching { host.terminate(startedId) } }
			transaction.execute { interruptStart(plan.matchId) }
			throw MatchStartException(plan.matchId)
		}
	}

	private fun persistAcceptance(
		accountId: Long,
		trainerId: Long,
		challengeId: Long,
		request: AcceptChallengeRequest,
	): AcceptanceOutcome {
		val now = Instant.now(clock)
		// 先无锁观察不可变参与方，再按账户、Trainer、Challenge 的全局顺序加锁，避免并发接受形成锁环。
		val observed = findChallenge(challengeId, forUpdate = false)
			?: throw ChallengeRequestException(HttpStatus.NOT_FOUND, "challenge.not-found")
		if (observed.challengedTrainerId != trainerId || observed.challengedAccountId != accountId) {
			throw ChallengeRequestException(HttpStatus.FORBIDDEN, "challenge.action-forbidden")
		}
		lockAccounts(observed.challengerAccountId, observed.challengedAccountId)
		lockTrainers(observed.challengerTrainerId, observed.challengedTrainerId)
		val challenge = findChallenge(challengeId, forUpdate = true)
			?: throw ChallengeRequestException(HttpStatus.NOT_FOUND, "challenge.not-found")
		if (challenge.challengedTrainerId != trainerId || challenge.challengedAccountId != accountId) {
			throw ChallengeRequestException(HttpStatus.FORBIDDEN, "challenge.action-forbidden")
		}
		if (challenge.status != ChallengeStatus.PENDING) throw conflict("challenge.already-resolved")
		if (request.expectedRevision != challenge.revision) throw conflict("challenge.revision-conflict")
		if (!now.isBefore(challenge.expiresAt)) {
			resolveChallenge(challenge, ChallengeStatus.EXPIRED, now)
			return AcceptanceOutcome.Failed(conflict("challenge.already-resolved"))
		}

		if (!presence.isOnline(challenge.challengerTrainerId, now) || !presence.isOnline(challenge.challengedTrainerId, now)) {
			throw conflict("challenge.trainer-offline")
		}
		if (hasReservation(challenge.challengerAccountId) || hasReservation(challenge.challengedAccountId)) {
			throw conflict("challenge.active-match-exists")
		}

		val challengerSnapshot = findSnapshot(challenge.challengerSnapshotId)
			?: error("Challenger snapshot is missing")
		val challengerMembers = challengerSnapshot.roster.members.map { it.toTeamMember() }
		try {
			teams.validateForMatch(challengerMembers)
		} catch (_: TrainerTeamRequestException) {
			resolveChallenge(challenge, ChallengeStatus.CANCELLED, now, ChallengeCancellationReason.ROSTER_INVALIDATED)
			return AcceptanceOutcome.Failed(conflict("challenge.roster-invalidated"))
		}

		val challengedTeam = teams.find(trainerId)
			?: throw invalid("challenge.team-required")
		if (challengedTeam.members.size != challengerMembers.size) throw invalid("challenge.team-size-mismatch")
		if (request.leadPosition !in 1..challengedTeam.members.size) throw invalid("challenge.lead.invalid")
		try {
			teams.validateForMatch(challengedTeam.members)
		} catch (_: TrainerTeamRequestException) {
			throw invalid("challenge.team-invalid")
		}

		val challengedSnapshot = snapshots.save(MatchTeamSnapshot {
			this.trainerId = trainerId
			schemaVersion = challengerSnapshot.schemaVersion
			roster = challengedTeam.toSnapshot(request.leadPosition)
		}, SaveMode.INSERT_ONLY)
		val game = games.save(MatchGame {
			this.challengeId = challenge.id
			ruleCode = challenge.ruleCode
			status = MatchStatus.STARTING
			battleSessionId = null
			revision = 0
			turnNumber = 0
			turnDeadline = null
			interruptionReason = null
			outcome = null
			completionReason = null
			winnerTrainerId = null
			battleReason = null
			startedAt = null
			endedAt = null
		}, SaveMode.INSERT_ONLY)

		val challengerSide = if (chooseFirstSide()) 1 else 2
		val challengedSide = 3 - challengerSide
		saveParticipant(game.id, challenge.challengerTrainerId, challenge.challengerAccountId,
			challengerSnapshot.id, challengerSide, challenge.challengerDisplayName)
		saveParticipant(game.id, challenge.challengedTrainerId, challenge.challengedAccountId,
			challengedSnapshot.id, challengedSide, challenge.challengedDisplayName)
		reservations.save(MatchActiveAccountReservation {
			this.accountId = challenge.challengerAccountId
			matchId = game.id
		}, SaveMode.INSERT_ONLY)
		reservations.save(MatchActiveAccountReservation {
			this.accountId = challenge.challengedAccountId
			matchId = game.id
		}, SaveMode.INSERT_ONLY)
		resolveChallenge(challenge, ChallengeStatus.ACCEPTED, now, challengedSnapshotId = challengedSnapshot.id)
		supersedeOtherChallenges(challenge, now)

		val sides = listOf(
			challengerSide to challengerSnapshot.roster,
			challengedSide to challengedSnapshot.roster,
		).sortedBy { it.first }.map { (_, roster) -> toHostedSide(roster) }
		return AcceptanceOutcome.Ready(AcceptancePlan(game.id, trainerId, HostedBattleRoster(challenge.ruleCode, sides)))
	}

	private fun activate(matchId: Long, sessionId: String, viewerTrainerId: Long): MatchResponse {
		val now = Instant.now(clock)
		val changed = sqlClient.createUpdate(MatchGame::class) {
			where(table.id eq matchId, table.status eq MatchStatus.STARTING, table.revision eq 0L)
			set(table.status, MatchStatus.ACTIVE)
			set(table.battleSessionId, sessionId)
			set(table.startedAt, now)
			set(table.turnDeadline, now.plus(TURN_TIMEOUT))
			set(table.revision, table.revision + 1)
			set(table.updatedAt, now)
		}.execute()
		if (changed != 1) throw IllegalStateException("Starting Match cannot be activated")
		return response(matchId, viewerTrainerId)
	}

	private fun interruptStart(matchId: Long) {
		val now = Instant.now(clock)
		sqlClient.createUpdate(MatchGame::class) {
			where(table.id eq matchId, table.status eq MatchStatus.STARTING)
			set(table.status, MatchStatus.INTERRUPTED)
			set(table.interruptionReason, MatchInterruptionReason.START_FAILED)
			set(table.endedAt, now)
			set(table.revision, table.revision + 1)
			set(table.updatedAt, now)
		}.execute()
		sqlClient.createDelete(MatchActiveAccountReservation::class) { where(table.matchId eq matchId) }.execute()
	}

	private fun saveParticipant(matchId: Long, trainerId: Long, accountId: Long, snapshotId: Long, side: Int, displayName: String) {
		participants.save(MatchParticipant {
			id = MatchParticipantId { this.matchId = matchId; this.trainerId = trainerId }
			this.accountId = accountId
			this.snapshotId = snapshotId
			this.side = side
			this.displayName = displayName
		}, SaveMode.INSERT_ONLY)
	}

	private fun response(matchId: Long, viewerTrainerId: Long): MatchResponse {
		val game = findGame(matchId) ?: error("Match is missing")
		val rows = findParticipants(matchId)
		return MatchResponse {
			id = game.id
			ruleCode = game.ruleCode
			status = game.status
			revision = game.revision
			turnNumber = game.turnNumber
			turnDeadline = game.turnDeadline
			interruptionReason = game.interruptionReason
			participants = rows.sortedBy(MatchParticipant::side).map {
				MatchParticipantResponse(it.displayName, it.id.trainerId == viewerTrainerId)
			}
			startedAt = game.startedAt
			endedAt = game.endedAt
		}
	}

	private fun toHostedSide(roster: TrainerTeamSnapshotRoster): HostedBattleSide {
		val natureIds = roster.members.map(TrainerTeamSnapshotMember::natureId).toSet()
		val natures = sqlClient.createQuery(GameNatures::class) {
			where(table.gameDataId valueIn natureIds)
			select(table)
		}.execute().associateBy(GameNatures::id)
		val statIds = natures.values.flatMap { listOfNotNull(it.increasedStatId, it.decreasedStatId) }.toSet()
		val statCodes = if (statIds.isEmpty()) emptyMap() else sqlClient.createQuery(GameStat::class) {
			where(table.gameDataId valueIn statIds)
			select(table.gameDataId, table.gameDataCode)
		}.execute().associate { it._1 to it._2 }
		return HostedBattleSide(
			activeParticipantIndexes = listOf(roster.leadPosition - 1),
			participants = roster.members.map { member ->
				val nature = natures.getValue(member.natureId)
				HostedBattleParticipant(
					member.creatureId, member.level, member.skillIds, member.abilityId, member.itemId,
					member.individualValues, member.effortValues,
					nature.increasedStatId?.let(statCodes::get), nature.decreasedStatId?.let(statCodes::get),
				)
			},
		)
	}

	private fun lockAccounts(first: Long, second: Long) {
		val ids = sqlClient.createQuery(SecurityUser::class) {
			where(table.securityUserId valueIn listOf(first, second))
			orderBy(table.securityUserId)
			select(table.securityUserId)
		}.forUpdate().execute()
		if (ids.size != 2) throw conflict("challenge.account-unavailable")
	}

	private fun lockTrainers(first: Long, second: Long) {
		val ids = sqlClient.createQuery(MatchTrainer::class) {
			where(table.id valueIn listOf(first, second), table.archivedAt.isNull())
			orderBy(table.id)
			select(table.id)
		}.forUpdate().execute()
		if (ids.size != 2) throw conflict("challenge.trainer-unavailable")
	}

	private fun hasReservation(accountId: Long) = sqlClient.createQuery(MatchActiveAccountReservation::class) {
		where(table.accountId eq accountId)
		select(table.accountId)
	}.execute().isNotEmpty()

	private fun resolveChallenge(
		challenge: MatchChallenge,
		status: ChallengeStatus,
		now: Instant,
		cancellationReason: ChallengeCancellationReason? = null,
		challengedSnapshotId: Long? = challenge.challengedSnapshotId,
	) {
		sqlClient.createUpdate(MatchChallenge::class) {
			where(table.id eq challenge.id, table.status eq ChallengeStatus.PENDING, table.revision eq challenge.revision)
			set(table.status, status)
			set(table.cancellationReason, cancellationReason)
			set(table.challengedSnapshotId, challengedSnapshotId)
			set(table.resolvedAt, now)
			set(table.revision, table.revision + 1)
			set(table.updatedAt, now)
		}.execute()
	}

	private fun supersedeOtherChallenges(accepted: MatchChallenge, now: Instant) {
		sqlClient.createUpdate(MatchChallenge::class) {
			where(table.id ne accepted.id, table.status eq ChallengeStatus.PENDING)
			where(or(
				table.challengerAccountId valueIn listOf(accepted.challengerAccountId, accepted.challengedAccountId),
				table.challengedAccountId valueIn listOf(accepted.challengerAccountId, accepted.challengedAccountId),
			))
			set(table.status, ChallengeStatus.SUPERSEDED)
			set(table.resolvedAt, now)
			set(table.revision, table.revision + 1)
			set(table.updatedAt, now)
		}.execute()
	}

	private fun findChallenge(id: Long, forUpdate: Boolean): MatchChallenge? {
		val query = sqlClient.createQuery(MatchChallenge::class) { where(table.id eq id); select(table) }
		return (if (forUpdate) query.forUpdate() else query).execute().singleOrNull()
	}

	private fun findSnapshot(id: Long) = sqlClient.createQuery(MatchTeamSnapshot::class) {
		where(table.id eq id); select(table)
	}.execute().singleOrNull()

	private fun findGame(id: Long) = sqlClient.createQuery(MatchGame::class) {
		where(table.id eq id); select(table)
	}.execute().singleOrNull()

	private fun findParticipants(matchId: Long) = sqlClient.createQuery(MatchParticipant::class) {
		where(table.id.matchId eq matchId); select(table)
	}.execute()

	private fun TrainerTeamRecord.toSnapshot(lead: Int) = TrainerTeamSnapshotRoster(lead, members.map {
		TrainerTeamSnapshotMember(it.creatureId, it.skillIds, it.abilityId, it.itemId, it.natureId, 50,
			it.individualValues, it.effortValues)
	})

	private fun TrainerTeamSnapshotMember.toTeamMember() = TrainerTeamMemberRecord(
		creatureId, skillIds, abilityId, itemId, natureId, individualValues, effortValues,
	)

	private fun invalid(code: String) = ChallengeRequestException(HttpStatus.UNPROCESSABLE_ENTITY, code)
	private fun conflict(code: String) = ChallengeRequestException(HttpStatus.CONFLICT, code)

	private sealed interface AcceptanceOutcome {
		data class Ready(val plan: AcceptancePlan) : AcceptanceOutcome
		data class Failed(val error: ChallengeRequestException) : AcceptanceOutcome
	}
	private data class AcceptancePlan(val matchId: Long, val viewerTrainerId: Long, val roster: HostedBattleRoster)

	private companion object {
		val TURN_TIMEOUT: Duration = Duration.ofSeconds(90)
	}
}
