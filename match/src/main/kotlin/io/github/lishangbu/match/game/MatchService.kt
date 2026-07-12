package io.github.lishangbu.match.game

import io.github.lishangbu.gamedata.entity.GameNatures
import io.github.lishangbu.gamedata.entity.GameStat
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battlesession.model.TurnCommand
import io.github.lishangbu.battlesession.model.TurnCommandResult
import io.github.lishangbu.battlesession.model.TurnRecord
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
import java.security.MessageDigest
import java.nio.ByteBuffer
import java.util.UUID
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** Challenge 接受、Match 持久化与 Battle Runtime 启动的单一应用边界。 */
open class MatchService(
	private val games: MatchGameRepository,
	private val participants: MatchParticipantRepository,
	private val reservations: MatchActiveAccountReservationRepository,
	private val turns: MatchTurnSubmissionRepository,
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
			val runtime = host.inspect(startedSessionId)
			transaction.execute { activate(plan.matchId, startedSessionId, runtime, plan.viewerTrainerId) }
			advanceAutomaticTurns(plan.matchId, startedSessionId, plan.viewerTrainerId)
			response(plan.matchId, plan.viewerTrainerId)
		} catch (error: RuntimeException) {
			sessionId?.let { startedId -> runCatching { host.terminate(startedId) } }
			transaction.execute { interruptStart(plan.matchId) }
			throw MatchStartException(plan.matchId)
		}
	}

	/** 查询当前 Trainer 占用容量的 Match，并按该 Trainer 的隐藏信息视角投影。 */
	open fun current(accountId: Long, trainerId: Long): MatchViewResponse {
		val matchId = sqlClient.createQuery(MatchActiveAccountReservation::class) {
			where(table.accountId eq accountId)
			select(table.matchId)
		}.execute().singleOrNull() ?: throw ChallengeRequestException(HttpStatus.NOT_FOUND, "match.current.not-found")
		return view(accountId, trainerId, matchId)
	}

	/**
	 * 终态 Match 才进入历史；结果按查看方转换为 WIN/LOSS/DRAW/NO_CONTEST。
	 * 归档入口额外要求 Trainer 已归档，避免绕过有效 Trainer 的 Session 边界。
	 */
	open fun history(
		accountId: Long,
		trainerId: Long,
		archivedOnly: Boolean = false,
		beforeMatchId: Long? = null,
		limit: Int = 20,
	): List<MatchHistoryResponse> {
		if (limit !in 1..100) throw ChallengeRequestException(HttpStatus.UNPROCESSABLE_ENTITY, "match.history.invalid-limit")
		requireTrainerOwnership(accountId, trainerId, archivedOnly)
		val viewerRows = sqlClient.createQuery(MatchParticipant::class) {
			where(table.id.trainerId eq trainerId, beforeMatchId?.let { table.id.matchId lt it })
			orderBy(table.id.matchId.desc())
			select(table)
		// 每个 Trainer 同时最多只有一个非终态 Match，多取一条即可在过滤后仍返回完整一页终态历史。
		}.limit(limit + 1).execute()
		if (viewerRows.isEmpty()) return emptyList()
		val matches = sqlClient.createQuery(MatchGame::class) {
			where(
				table.id valueIn viewerRows.map { it.id.matchId },
				table.status valueIn listOf(MatchStatus.COMPLETED, MatchStatus.INTERRUPTED),
			)
			// CosId 随时间递增，ID 游标可稳定翻页且不会因新终态插入造成 offset 漂移。
			orderBy(table.id.desc())
			select(table)
		}.limit(limit).execute()
		val participantsByMatch = findParticipants(matches.map(MatchGame::id)).groupBy { it.id.matchId }
		return matches.map { game ->
			val rows = participantsByMatch.getValue(game.id)
			val opponent = rows.single { it.id.trainerId != trainerId }
			MatchHistoryResponse {
				id = game.id
				opponentDisplayName = opponent.displayName
				status = game.status
				result = game.resultFor(trainerId)
				interruptionReason = game.interruptionReason
				startedAt = game.startedAt
				endedAt = game.endedAt
				turnNumber = game.turnNumber
			}
		}
	}

	/** History 详情拒绝 ACTIVE/STARTING Match，避免账户级归档入口成为当前对局旁路。 */
	open fun historyDetail(
		accountId: Long,
		trainerId: Long,
		matchId: Long,
		archivedOnly: Boolean = false,
	): MatchViewResponse {
		requireTrainerOwnership(accountId, trainerId, archivedOnly)
		val game = findGame(matchId) ?: throw ChallengeRequestException(HttpStatus.NOT_FOUND, "match.not-found")
		if (game.status !in listOf(MatchStatus.COMPLETED, MatchStatus.INTERRUPTED)) {
			throw ChallengeRequestException(HttpStatus.NOT_FOUND, "match.not-found")
		}
		return view(accountId, trainerId, matchId)
	}

	/** 只有实际参与方可以读取 Match；账户与 Trainer 必须同时匹配，避免跨 Trainer 侧信道。 */
	open fun view(accountId: Long, trainerId: Long, matchId: Long): MatchViewResponse {
		val game = findGame(matchId) ?: throw ChallengeRequestException(HttpStatus.NOT_FOUND, "match.not-found")
		val rows = findParticipants(matchId)
		val viewer = rows.firstOrNull { it.accountId == accountId && it.id.trainerId == trainerId }
			?: throw ChallengeRequestException(HttpStatus.NOT_FOUND, "match.not-found")
		val rowsBySide = rows.associateBy(MatchParticipant::side)
		val snapshotsById = findSnapshots(rows.map(MatchParticipant::snapshotId)).associateBy(MatchTeamSnapshot::id)
		val state = if (game.status == MatchStatus.ACTIVE) {
			val sessionId = game.battleSessionId ?: throw conflict("match.runtime-unavailable")
			runCatching { host.inspect(sessionId).toViewState() }
				.getOrElse { throw ChallengeRequestException(HttpStatus.SERVICE_UNAVAILABLE, "match.runtime-unavailable") }
		} else game.viewState ?: initialViewState(rows, snapshotsById)
		val disclosedByPosition = findDisclosures(matchId, trainerId).associateBy { it.id.opponentMemberPosition }
		return MatchViewResponse {
			id = game.id
			ruleCode = game.ruleCode
			status = game.status
			revision = game.revision
			turnNumber = game.turnNumber
			turnDeadline = game.turnDeadline
			result = when {
				game.status != MatchStatus.COMPLETED -> null
				game.outcome == MatchOutcome.DRAW -> "DRAW"
				game.outcome == MatchOutcome.NO_CONTEST -> "NO_CONTEST"
				game.winnerTrainerId == trainerId -> "WIN"
				else -> "LOSS"
			}
			completionReason = game.completionReason
			interruptionReason = game.interruptionReason
			sides = state.sides.mapIndexed { index, stateSide ->
				val participant = rowsBySide.getValue(index + 1)
				val own = participant.id.trainerId == viewer.id.trainerId
				val roster = snapshotsById.getValue(participant.snapshotId).roster
				MatchViewSideResponse {
					displayName = participant.displayName
					you = own
					participants = stateSide.participants.mapIndexed { memberIndex, stateMember ->
						val frozen = roster.members[memberIndex]
						MatchViewParticipantResponse {
							position = memberIndex + 1
							creatureId = stateMember.creatureId
							active = stateMember.active
							currentHp = stateMember.currentHp
							maxHp = stateMember.maxHp
							level = if (own) frozen.level else null
							val disclosed = disclosedByPosition[memberIndex + 1]?.disclosures
							skillIds = if (own) frozen.skillIds else disclosed?.skillIds?.sorted()?.takeIf { it.isNotEmpty() }
							abilityId = if (own) frozen.abilityId else disclosed?.abilityId
							itemId = if (own) frozen.itemId else disclosed?.itemId
							natureId = if (own) frozen.natureId else null
							individualValues = if (own) frozen.individualValues else null
							effortValues = if (own) frozen.effortValues else null
						}
					}
				}
			}
			// 终态快照可能来自 Forfeit/Timeout 之前，绝不能继续向客户端呈现可提交行动。
			requirements = state.requirements.takeIf { game.status == MatchStatus.ACTIVE }.orEmpty()
				.filter { selection -> selection.actorSide == viewer.side }
				.map { selection ->
					MatchTurnRequirementResponse {
						actorPosition = selection.actorPosition
						options = selection.options.map { option -> option.toViewOption(viewer.side) }
					}
				}
		}
	}

	/** 先持久化不可逆赛果，再 best-effort 终止 Runtime；Runtime 不能反向决定胜负。 */
	open fun forfeit(accountId: Long, trainerId: Long, matchId: Long, expectedRevision: Long): MatchViewResponse {
		val sessionId = transaction.execute {
			val game = findGame(matchId, forUpdate = true)
				?: throw ChallengeRequestException(HttpStatus.NOT_FOUND, "match.not-found")
			val rows = findParticipants(matchId)
			val viewer = rows.firstOrNull { it.accountId == accountId && it.id.trainerId == trainerId }
				?: throw ChallengeRequestException(HttpStatus.NOT_FOUND, "match.not-found")
			if (game.status == MatchStatus.COMPLETED && game.completionReason == MatchCompletionReason.FORFEIT) {
				return@execute null
			}
			if (game.status != MatchStatus.ACTIVE) throw conflict("match.not-active")
			if (game.revision != expectedRevision) throw conflict("match.revision-conflict")
			val now = Instant.now(clock)
			val winner = rows.single { it.id.trainerId != viewer.id.trainerId }
			sqlClient.createUpdate(MatchGame::class) {
				where(table.id eq matchId, table.status eq MatchStatus.ACTIVE, table.revision eq expectedRevision)
				set(table.status, MatchStatus.COMPLETED)
				set(table.outcome, MatchOutcome.WIN)
				set(table.completionReason, MatchCompletionReason.FORFEIT)
				set(table.winnerTrainerId, winner.id.trainerId)
				set(table.turnDeadline, null)
				set(table.endedAt, now)
				set(table.revision, table.revision + 1)
				set(table.updatedAt, now)
			}.execute().also { if (it != 1) throw conflict("match.revision-conflict") }
			sqlClient.createDelete(MatchActiveAccountReservation::class) { where(table.matchId eq matchId) }.execute()
			game.battleSessionId
		}
		sessionId?.let { runCatching { host.terminate(it) } }
		return view(accountId, trainerId, matchId)
	}

	/** 扫描绝对 deadline；查询、断线和失败命令都不会延长该时间。 */
	open fun adjudicateExpiredTurns() {
		val now = Instant.now(clock)
		val expiredIds = sqlClient.createQuery(MatchGame::class) {
			where(table.status eq MatchStatus.ACTIVE, table.turnDeadline.isNotNull(), table.turnDeadline le now)
			orderBy(table.id)
			select(table.id)
		}.limit(TIMEOUT_BATCH_SIZE).execute()
		expiredIds.forEach { matchId ->
			val plan = transaction.execute { adjudicateExpiredTurn(matchId, now) }
			if (plan is TimeoutPlan.Terminate) runCatching { host.terminate(plan.sessionId) }
		}
	}

	private fun adjudicateExpiredTurn(matchId: Long, now: Instant): TimeoutPlan {
		val game = findGame(matchId, forUpdate = true) ?: return TimeoutPlan.None()
		if (game.status != MatchStatus.ACTIVE || game.turnDeadline?.isAfter(now) != false) return TimeoutPlan.None()
		val rows = findParticipants(matchId)
		val locked = findTurns(matchId, game.turnNumber + 1)
		// 双方齐备时应由正在进行的 Runtime handoff 收敛，超时器不能抢先伪造赛果。
		if (locked.size >= rows.size) return TimeoutPlan.None()
		val winnerTrainerId = locked.singleOrNull()?.id?.trainerId
		sqlClient.createUpdate(MatchGame::class) {
			where(table.id eq matchId, table.status eq MatchStatus.ACTIVE)
			set(table.status, MatchStatus.COMPLETED)
			set(table.outcome, if (winnerTrainerId == null) MatchOutcome.NO_CONTEST else MatchOutcome.WIN)
			set(table.completionReason, MatchCompletionReason.TIMEOUT)
			set(table.winnerTrainerId, winnerTrainerId)
			set(table.turnDeadline, null)
			set(table.endedAt, now)
			set(table.revision, table.revision + 1)
			set(table.updatedAt, now)
		}.execute()
		sqlClient.createDelete(MatchActiveAccountReservation::class) { where(table.matchId eq matchId) }.execute()
		return game.battleSessionId?.let(TimeoutPlan::Terminate) ?: TimeoutPlan.None()
	}

	/** 单方提交只锁定己方选择；双方齐备后才向 Runtime 发送完整命令。 */
	open fun submitTurn(accountId: Long, trainerId: Long, matchId: Long, request: SubmitMatchTurnRequest): MatchTurnResponse {
		val plan = transaction.execute { persistTurn(accountId, trainerId, matchId, request) }
		if (plan is TurnPlan.Waiting) return MatchTurnResponse(locked = true)
		if (plan is TurnPlan.Applied) return MatchTurnResponse(locked = true, match = view(accountId, trainerId, matchId))
		plan as TurnPlan.Ready
		return try {
			val result = host.execute(plan.sessionId, plan.command)
			val applied = transaction.execute { applyTurnResult(matchId, result) }
			if (!applied) {
				runCatching { host.terminate(plan.sessionId) }
				return MatchTurnResponse(locked = true, match = view(accountId, trainerId, matchId))
			}
			MatchTurnResponse(locked = true, match = advanceAutomaticTurns(matchId, plan.sessionId, plan.viewerTrainerId))
		} catch (error: RuntimeException) {
			runCatching { host.terminate(plan.sessionId) }
			transaction.execute { interruptRuntime(matchId) }
			throw ChallengeRequestException(HttpStatus.SERVICE_UNAVAILABLE, "match.runtime-failed")
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
			viewState = null
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

	private fun persistTurn(
		accountId: Long,
		trainerId: Long,
		matchId: Long,
		request: SubmitMatchTurnRequest,
	): TurnPlan {
		val game = findGame(matchId, forUpdate = true)
			?: throw ChallengeRequestException(HttpStatus.NOT_FOUND, "match.not-found")
		val participantRows = findParticipants(matchId)
		val viewer = participantRows.firstOrNull { it.accountId == accountId && it.id.trainerId == trainerId }
			?: throw ChallengeRequestException(HttpStatus.NOT_FOUND, "match.not-found")
		val normalizedSubmissionId = request.submissionId.toUuidV4OrNull()?.toString()
			?: throw invalid("match.submission-id.invalid")
		val reused = findTurnBySubmission(trainerId, normalizedSubmissionId)
		if (reused != null) {
			if (reused.id.matchId != matchId || reused.actions != request.actions) throw conflict("match.submission-conflict")
			if (game.turnNumber >= reused.id.turnNumber) return TurnPlan.Applied()
		}
		if (game.status != MatchStatus.ACTIVE) throw conflict("match.not-active")
		if (game.revision != request.expectedRevision) throw conflict("match.revision-conflict")
		val turnNumber = game.turnNumber + 1
		if (reused != null && reused.id.turnNumber != turnNumber) {
			throw conflict("match.submission-conflict")
		}
		val existing = findTurn(matchId, turnNumber, trainerId)
		if (existing != null) {
			if (existing.submissionId != normalizedSubmissionId || existing.actions != request.actions) {
				throw conflict("match.submission-conflict")
			}
		}
		val sessionId = game.battleSessionId ?: throw conflict("match.runtime-unavailable")
		val runtime = runCatching { host.inspect(sessionId) }
			.getOrElse { throw ChallengeRequestException(HttpStatus.SERVICE_UNAVAILABLE, "match.runtime-unavailable") }
		val ownActions = request.actions.map { it.toBattleAction(viewer.side) }
		val required = runtime.requirements.selections
			.filter { runtime.state.sideOf(it.actorId)?.sideId == "side-${viewer.side}" }
		if (required.isEmpty()) throw conflict("match.turn.not-required")
		if (ownActions.size != required.size || required.any { requirement ->
			val selected = ownActions.singleOrNull { it.actorId == requirement.actorId }
			selected == null || selected !in requirement.options
		}) throw invalid("match.turn.invalid")
		if (existing == null) {
			turns.save(MatchTurnSubmission {
				id = MatchTurnSubmissionId {
					this.matchId = matchId
					this.turnNumber = turnNumber
					this.trainerId = trainerId
				}
				submissionId = normalizedSubmissionId
				actions = request.actions
			}, SaveMode.INSERT_ONLY)
		}
		val locked = findTurns(matchId, turnNumber)
		val manualSides = runtime.requirements.selections.map { actorSide(it.actorId) }.toSet()
		val requiredTrainerIds = participantRows.filter { it.side in manualSides }.map { it.id.trainerId }.toSet()
		if (!locked.map { it.id.trainerId }.containsAll(requiredTrainerIds)) return TurnPlan.Waiting()
		val sides = participantRows.associate { it.id.trainerId to it.side }
		val actions = locked.sortedBy { sides.getValue(it.id.trainerId) }
			.flatMap { row -> row.actions.map { it.toBattleAction(sides.getValue(row.id.trainerId)) } }
		return TurnPlan.Ready(
			sessionId,
			TurnCommand(stableTurnCommandId(matchId, turnNumber), runtime.revision, actions),
			trainerId,
		)
	}

	private fun advanceAutomaticTurns(matchId: Long, sessionId: String, viewerTrainerId: Long): MatchViewResponse {
		repeat(MAX_AUTOMATIC_TURNS) {
			val snapshot = host.inspect(sessionId)
			if (snapshot.state.result != null || snapshot.requirements.selections.isNotEmpty()) {
				return view(findParticipants(matchId).single { it.id.trainerId == viewerTrainerId }.accountId, viewerTrainerId, matchId)
			}
			val nextTurn = snapshot.state.turnNumber + 1
			val result = host.execute(sessionId, TurnCommand(stableTurnCommandId(matchId, nextTurn), snapshot.revision, emptyList()))
			val applied = transaction.execute { applyTurnResult(matchId, result) }
			if (!applied) return view(findParticipants(matchId).single { it.id.trainerId == viewerTrainerId }.accountId, viewerTrainerId, matchId)
		}
		transaction.execute { interruptRuntime(matchId) }
		runCatching { host.terminate(sessionId) }
		throw ChallengeRequestException(HttpStatus.SERVICE_UNAVAILABLE, "match.runtime-failed")
	}

	private fun applyTurnResult(matchId: Long, resultCommand: TurnCommandResult): Boolean {
		val runtime = resultCommand.session
		val now = Instant.now(clock)
		val result = runtime.state.result
		val changed = sqlClient.createUpdate(MatchGame::class) {
			where(table.id eq matchId, table.status eq MatchStatus.ACTIVE)
			set(table.turnNumber, runtime.state.turnNumber)
			set(table.viewState, runtime.toViewState())
			set(table.revision, table.revision + 1)
			set(table.updatedAt, now)
			if (result == null) {
				set(table.turnDeadline, now.plus(TURN_TIMEOUT))
			} else {
				set(table.status, MatchStatus.COMPLETED)
				set(table.outcome, if (result.winningSideId == null) MatchOutcome.DRAW else MatchOutcome.WIN)
				set(table.completionReason, MatchCompletionReason.BATTLE)
				set(table.winnerTrainerId, result.winningSideId?.let { winning ->
					findParticipants(matchId).single { "side-${it.side}" == winning }.id.trainerId
				})
				set(table.battleReason, result.reason)
				set(table.turnDeadline, null)
				set(table.endedAt, now)
			}
		}.execute()
		if (changed != 1) {
			val current = findGame(matchId) ?: throw conflict("match.revision-conflict")
			if (current.status == MatchStatus.COMPLETED || current.status == MatchStatus.INTERRUPTED) return false
			throw conflict("match.revision-conflict")
		}
		// Match 行与公开账本处于同一事务：任何账本写入失败都会撤销本次 revision，避免视图少揭示事实。
		persistDisclosures(matchId, resultCommand.turnRecord, now)
		if (result != null) {
			sqlClient.createDelete(MatchActiveAccountReservation::class) { where(table.matchId eq matchId) }.execute()
		}
		return true
	}

	private fun interruptRuntime(matchId: Long) {
		val now = Instant.now(clock)
		sqlClient.createUpdate(MatchGame::class) {
			where(table.id eq matchId, table.status eq MatchStatus.ACTIVE)
			set(table.status, MatchStatus.INTERRUPTED)
			set(table.interruptionReason, MatchInterruptionReason.RUNTIME_FAILED)
			set(table.turnDeadline, null)
			set(table.endedAt, now)
			set(table.revision, table.revision + 1)
			set(table.updatedAt, now)
		}.execute()
		sqlClient.createDelete(MatchActiveAccountReservation::class) { where(table.matchId eq matchId) }.execute()
	}

	private fun activate(
		matchId: Long,
		sessionId: String,
		runtime: io.github.lishangbu.battlesession.model.BattleSessionSnapshot,
		viewerTrainerId: Long,
	): MatchResponse {
		val now = Instant.now(clock)
		val changed = sqlClient.createUpdate(MatchGame::class) {
			where(table.id eq matchId, table.status eq MatchStatus.STARTING, table.revision eq 0L)
			set(table.status, MatchStatus.ACTIVE)
			set(table.battleSessionId, sessionId)
			set(table.startedAt, now)
			set(table.turnDeadline, now.plus(TURN_TIMEOUT))
			set(table.viewState, runtime.toViewState())
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

	private fun findSnapshots(ids: Collection<Long>) = sqlClient.createQuery(MatchTeamSnapshot::class) {
		where(table.id valueIn ids)
		select(table)
	}.execute()

	private fun findGame(id: Long, forUpdate: Boolean = false): MatchGame? {
		val query = sqlClient.createQuery(MatchGame::class) {
		where(table.id eq id); select(table)
		}
		return (if (forUpdate) query.forUpdate() else query).execute().singleOrNull()
	}

	private fun findParticipants(matchId: Long) = sqlClient.createQuery(MatchParticipant::class) {
		where(table.id.matchId eq matchId); select(table)
	}.execute()

	/**
	 * START_FAILED 发生在 Runtime 产生战斗快照之前，历史详情仍须从永久 Team Snapshot 重建 Team Preview。
	 * 此时从未发生战斗，HP 没有权威结算值，因此使用 0 表示“不适用”，且绝不产生可提交 requirements。
	 */
	private fun initialViewState(
		rows: List<MatchParticipant>,
		snapshotsById: Map<Long, MatchTeamSnapshot>,
	) = MatchBattleViewState(
		sides = rows.sortedBy(MatchParticipant::side).map { participant ->
			val roster = snapshotsById.getValue(participant.snapshotId).roster
			MatchBattleViewSide(roster.members.mapIndexed { index, member ->
				MatchBattleViewParticipant(
					creatureId = member.creatureId,
					active = index + 1 == roster.leadPosition,
					currentHp = 0,
					maxHp = 0,
				)
			})
		},
		requirements = emptyList(),
	)

	private fun findParticipants(matchIds: Collection<Long>) = sqlClient.createQuery(MatchParticipant::class) {
		where(table.id.matchId valueIn matchIds); select(table)
	}.execute()

	private fun requireTrainerOwnership(accountId: Long, trainerId: Long, archivedOnly: Boolean) {
		val owned = sqlClient.createQuery(MatchTrainer::class) {
			where(
				table.id eq trainerId,
				table.accountId eq accountId,
				if (archivedOnly) table.archivedAt.isNotNull() else table.archivedAt.isNull(),
			)
			select(table.id)
		}.execute().singleOrNull()
		if (owned == null) throw ChallengeRequestException(HttpStatus.NOT_FOUND, "match.history.not-found")
	}

	private fun MatchGame.resultFor(trainerId: Long): String? = when {
		status != MatchStatus.COMPLETED -> null
		outcome == MatchOutcome.DRAW -> "DRAW"
		outcome == MatchOutcome.NO_CONTEST -> "NO_CONTEST"
		winnerTrainerId == trainerId -> "WIN"
		else -> "LOSS"
	}

	private fun findTurn(matchId: Long, turnNumber: Int, trainerId: Long) =
		sqlClient.createQuery(MatchTurnSubmission::class) {
			where(table.id.matchId eq matchId, table.id.turnNumber eq turnNumber, table.id.trainerId eq trainerId)
			select(table)
		}.execute().singleOrNull()

	private fun findTurns(matchId: Long, turnNumber: Int) = sqlClient.createQuery(MatchTurnSubmission::class) {
		where(table.id.matchId eq matchId, table.id.turnNumber eq turnNumber)
		select(table)
	}.execute()

	private fun findTurnBySubmission(trainerId: Long, submissionId: String) =
		sqlClient.createQuery(MatchTurnSubmission::class) {
			where(table.id.trainerId eq trainerId, table.submissionId eq submissionId)
			select(table)
		}.execute().singleOrNull()

	private fun findDisclosures(matchId: Long, viewerTrainerId: Long) =
		sqlClient.createQuery(MatchDisclosureLedger::class) {
			where(table.id.matchId eq matchId, table.id.viewerTrainerId eq viewerTrainerId)
			select(table)
		}.execute()

	private fun findDisclosures(matchId: Long, viewerTrainerIds: Collection<Long>) =
		sqlClient.createQuery(MatchDisclosureLedger::class) {
			where(table.id.matchId eq matchId, table.id.viewerTrainerId valueIn viewerTrainerIds)
			select(table)
		}.execute()

	/**
	 * 只从已提交技能和明确公开的特性/道具事件提取事实，不复制完整事件流或随机轨迹。
	 * Match 行已持有事务级写锁，因此批量读取、内存合并并按新增/更新分组写入不会丢失既有集合。
	 */
	private fun persistDisclosures(matchId: Long, turn: TurnRecord, now: Instant) {
		val viewerByOpponentSide = findParticipants(matchId).associate { participant ->
			(3 - participant.side) to participant.id.trainerId
		}
		data class Delta(val skills: MutableSet<Long> = linkedSetOf(), var abilityId: Long? = null, var itemId: Long? = null)
		val deltas = linkedMapOf<Pair<Long, Int>, Delta>()
		fun reveal(actorId: String, skillId: Long? = null, abilityId: Long? = null, itemId: Long? = null) {
			val viewerTrainerId = viewerByOpponentSide[actorSide(actorId)] ?: return
			val delta = deltas.getOrPut(viewerTrainerId to actorPosition(actorId)) { Delta() }
			skillId?.let(delta.skills::add)
			if (abilityId != null) delta.abilityId = abilityId
			if (itemId != null) delta.itemId = itemId
		}
		turn.events.forEach { event ->
			when (event) {
				// 只有实际进入公开事件流的技能才可揭示；锁定但未执行的提交仍是隐藏信息。
				is BattleEvent.SkillUsed -> reveal(event.actorId, skillId = event.skillId)
				is BattleEvent.SkillBlockedByAbility -> reveal(event.abilityHolderActorId, abilityId = event.abilityId)
				is BattleEvent.SkillAbsorbedByAbility -> reveal(event.abilityHolderActorId, abilityId = event.abilityId)
				is BattleEvent.HeldItemDamageApplied -> reveal(event.actorId, itemId = event.itemId)
				is BattleEvent.HeldItemTransferred -> {
					reveal(event.fromActorId, itemId = event.itemId)
					reveal(event.toActorId, itemId = event.itemId)
				}
				is BattleEvent.DamageReducedByItem -> reveal(event.targetActorId, itemId = event.itemId)
				is BattleEvent.SkillChargeSkippedByItem -> reveal(event.actorId, itemId = event.itemId)
				else -> Unit
			}
		}
		val existingById = findDisclosures(matchId, viewerByOpponentSide.values).associateBy(MatchDisclosureLedger::id)
		val rows = deltas.map { (key, delta) ->
			val id = MatchDisclosureLedgerId {
				this.matchId = matchId
				viewerTrainerId = key.first
				opponentMemberPosition = key.second
			}
			val previous = existingById[id]?.disclosures ?: MatchDisclosure()
			MatchDisclosureLedger {
				this.id = id
				schemaVersion = 1
				this.disclosures = MatchDisclosure(
					skillIds = previous.skillIds + delta.skills,
					abilityId = delta.abilityId ?: previous.abilityId,
					itemId = delta.itemId ?: previous.itemId,
				)
				updatedAt = now
			}
		}
		val (updates, inserts) = rows.partition { it.id in existingById }
		if (updates.isNotEmpty()) sqlClient.saveEntities(updates) { setMode(SaveMode.UPDATE_ONLY) }
		if (inserts.isNotEmpty()) sqlClient.saveEntities(inserts) { setMode(SaveMode.INSERT_ONLY) }
	}

	private fun TrainerTeamRecord.toSnapshot(lead: Int) = TrainerTeamSnapshotRoster(lead, members.map {
		TrainerTeamSnapshotMember(it.creatureId, it.skillIds, it.abilityId, it.itemId, it.natureId, 50,
			it.individualValues, it.effortValues)
	})

	private fun TrainerTeamSnapshotMember.toTeamMember() = TrainerTeamMemberRecord(
		creatureId, skillIds, abilityId, itemId, natureId, individualValues, effortValues,
	)

	private fun MatchBattleViewOption.toViewOption(viewerSide: Int) =
		MatchTurnOptionResponse {
			type = this@toViewOption.type
			skillId = this@toViewOption.skillId
			targetPosition = this@toViewOption.targetPosition
			targetYou = targetSide == viewerSide
		}

	private fun io.github.lishangbu.battlesession.model.BattleSessionSnapshot.toViewState() = MatchBattleViewState(
		sides = state.sides.map { side ->
			MatchBattleViewSide(side.participants.map { member ->
				MatchBattleViewParticipant(member.creatureId, member.actorId in side.activeActorIds, member.currentHp, member.maxHp)
			})
		},
		requirements = requirements.selections.map { requirement ->
			MatchBattleViewRequirement(
				actorSide = actorSide(requirement.actorId),
				actorPosition = actorPosition(requirement.actorId),
				options = requirement.options.map { option ->
					val targetActorId = when (option) {
						is BattleAction.UseSkill -> option.targetActorId
						is BattleAction.SwitchParticipant -> option.targetActorId
					}
					MatchBattleViewOption(
						type = if (option is BattleAction.UseSkill) "USE_SKILL" else "SWITCH_PARTICIPANT",
						skillId = (option as? BattleAction.UseSkill)?.skillId,
						targetSide = actorSide(targetActorId),
						targetPosition = actorPosition(targetActorId),
					)
				},
			)
		},
	)

	/** Runtime actorId 只在服务端用于定位；公开 View 仅暴露一侧内的稳定 position。 */
	private fun actorPosition(actorId: String): Int = actorId.substringAfterLast('-').toInt()
	private fun actorSide(actorId: String): Int = actorId.substringAfter("side-").substringBefore('-').toInt()

	private fun MatchTurnAction.toBattleAction(side: Int): BattleAction {
		val actorId = "side-$side-actor-$actorPosition"
		val targetSide = if (targetYou) side else 3 - side
		val targetActorId = "side-$targetSide-actor-$targetPosition"
		return when (type) {
			"USE_SKILL" -> BattleAction.UseSkill(actorId, skillId ?: throw invalid("match.turn.invalid"), targetActorId)
			"SWITCH_PARTICIPANT" -> BattleAction.SwitchParticipant(actorId, targetActorId)
			else -> throw invalid("match.turn.invalid")
		}
	}

	private fun String.toUuidV4OrNull() = runCatching { UUID.fromString(this) }.getOrNull()?.takeIf { it.version() == 4 }

	/** 以 Match/turn 派生稳定 UUID v4，使双方齐备后的 Runtime 命令可安全重试。 */
	private fun stableTurnCommandId(matchId: Long, turnNumber: Int): String {
		val digest = MessageDigest.getInstance("SHA-256").digest("$matchId:$turnNumber".toByteArray())
		digest[6] = ((digest[6].toInt() and 0x0f) or 0x40).toByte()
		digest[8] = ((digest[8].toInt() and 0x3f) or 0x80).toByte()
		val bytes = ByteBuffer.wrap(digest)
		return UUID(bytes.long, bytes.long).toString()
	}

	private fun invalid(code: String) = ChallengeRequestException(HttpStatus.UNPROCESSABLE_ENTITY, code)
	private fun conflict(code: String) = ChallengeRequestException(HttpStatus.CONFLICT, code)

	private sealed interface AcceptanceOutcome {
		data class Ready(val plan: AcceptancePlan) : AcceptanceOutcome
		data class Failed(val error: ChallengeRequestException) : AcceptanceOutcome
	}
	private data class AcceptancePlan(val matchId: Long, val viewerTrainerId: Long, val roster: HostedBattleRoster)
	private sealed interface TurnPlan {
		class Waiting : TurnPlan
		class Applied : TurnPlan
		data class Ready(val sessionId: String, val command: TurnCommand, val viewerTrainerId: Long) : TurnPlan
	}
	private sealed interface TimeoutPlan {
		class None : TimeoutPlan
		data class Terminate(val sessionId: String) : TimeoutPlan
	}

	private companion object {
		val TURN_TIMEOUT: Duration = Duration.ofSeconds(90)
		const val MAX_AUTOMATIC_TURNS = 100
		const val TIMEOUT_BATCH_SIZE = 100
	}
}
