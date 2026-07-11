package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.canBattle
import io.github.lishangbu.battleengine.BattleActionValidator
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleReplay
import io.github.lishangbu.battleengine.model.BattleReplayTurn
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.RecordingBattleRandom
import io.github.lishangbu.battleengine.random.TracedBattleRandom
import java.time.Clock

class BattleSessionRuntime(
	private val engine: BattleSessionEngine = DefaultBattleSessionEngine(),
	private val identifierGenerator: SessionIdentifierGenerator = UuidSessionIdentifierGenerator(),
	private val requirementsDeriver: TurnRequirementsDeriver = TurnRequirementsDeriver(),
	private val actionValidator: BattleActionValidator = BattleActionValidator(),
	private val randomFactory: BattleRandomFactory = SecureBattleRandomFactory(),
	private val clock: Clock = Clock.systemUTC(),
	private val capacity: SessionRuntimeCapacity = SessionRuntimeCapacity(),
) {
	private val registryLock = Any()
	private val activeSessions = linkedMapOf<String, SessionEntry>()
	private val recentSessions = linkedMapOf<String, SessionEntry>()

	fun create(initialState: BattleInitialState): BattleSessionSnapshot {
		synchronized(registryLock) {
			pruneExpiredRecentSessions(clock.instant())
			ensureActiveCapacity()
		}
		val state = engine.start(initialState)
		val now = clock.instant()
		val snapshot = BattleSessionSnapshot(
			sessionId = identifierGenerator.generate(),
			formatCode = state.format.code,
			status = BattleSessionStatus.ACTIVE,
			revision = 0,
			state = state,
			requirements = requirementsDeriver.derive(state),
			createdAt = now,
			updatedAt = now,
		)
		synchronized(registryLock) {
			pruneExpiredRecentSessions(now)
			ensureActiveCapacity()
			check(snapshot.sessionId !in activeSessions && snapshot.sessionId !in recentSessions) {
				"generated sessionId already exists"
			}
			activeSessions[snapshot.sessionId] = SessionEntry(snapshot, initialState)
		}
		return snapshot
	}

	fun get(sessionId: String): BattleSessionSnapshot {
		val entry = findEntry(sessionId)
		return synchronized(entry.lock) { entry.snapshot }
	}

	fun list(query: SessionQuery = SessionQuery()): SessionPage {
		val entries = synchronized(registryLock) {
			pruneExpiredRecentSessions(clock.instant())
			(activeSessions.values + recentSessions.values).toList()
		}
		val matching = entries
			.map { entry -> synchronized(entry.lock) { entry.snapshot } }
			.filter { session -> query.status == null || session.status == query.status }
			.filter { session -> query.formatCode == null || session.formatCode == query.formatCode }
			.sortedWith(
				compareByDescending<BattleSessionSnapshot> { it.updatedAt }
					.thenByDescending { it.sessionId },
			)
		val fromIndex = (query.page.toLong() * query.size).coerceAtMost(matching.size.toLong()).toInt()
		val toIndex = (fromIndex + query.size).coerceAtMost(matching.size)
		return SessionPage(
			items = matching.subList(fromIndex, toIndex).map(BattleSessionSummary::from),
			totalElements = matching.size.toLong(),
			page = query.page,
			size = query.size,
		)
	}

	fun submitTurn(sessionId: String, command: TurnCommand): TurnCommandResult {
		val entry = findEntry(sessionId)
		return synchronized(entry.lock) {
			submitTurn(entry, command)
		}
	}

	private fun submitTurn(entry: SessionEntry, command: TurnCommand): TurnCommandResult {
		entry.commandCache[command.commandId]?.let { cached ->
			if (cached !is CachedTurnCommand || cached.command != command) {
				throw CommandPayloadConflictException(command.commandId)
			}
			return reconstructTurnCommandResult(entry, cached)
		}
		val session = entry.snapshot
		if (session.status != BattleSessionStatus.ACTIVE) {
			throw BattleSessionNotActiveException(session.sessionId, session.status)
		}
		if (command.expectedRevision != session.revision) {
			throw SessionRevisionConflictException(command.expectedRevision, session.revision)
		}
		val submittedByActor = command.actions.groupBy { it.actorId }
		val complete = submittedByActor.keys == session.requirements.selections.map { it.actorId }.toSet() &&
			session.requirements.selections.all { requirement ->
				val submitted = submittedByActor[requirement.actorId]
				submitted?.size == 1 && submitted.single() in requirement.options
			}
		if (!complete) {
			throw IncompleteTurnCommandException(session.requirements)
		}
		val actionViolations = actionValidator.validate(session.state, command.actions)
		if (actionViolations.isNotEmpty()) {
			throw InvalidTurnActionsException(actionViolations)
		}
		val manualActionsByActor = session.requirements.selections.associate { requirement ->
			requirement.actorId to submittedByActor.getValue(requirement.actorId).single()
		}
		val orderedActions = session.state.sides
			.flatMap { side -> side.activeParticipants() }
			.mapNotNull { actor ->
				manualActionsByActor[actor.actorId] ?: actor
					.takeIf { it.canBattle() }
					?.skillSlots
					?.firstOrNull()
					?.let { skill -> BattleAction.UseSkill(actor.actorId, skill.skillId, actor.actorId) }
			}
		val recordingRandom = RecordingBattleRandom(randomFactory.create())
		val beforeEventCount = session.state.events.size
		val stateAfter = engine.resolveTurn(session.state, orderedActions, recordingRandom)
		val revisionAfter = session.revision + 1
		val record = TurnRecord(
			commandId = command.commandId,
			revisionBefore = session.revision,
			revisionAfter = revisionAfter,
			turnNumber = stateAfter.turnNumber,
			submittedActions = orderedActions,
			randomTrace = recordingRandom.trace(),
			events = stateAfter.events.drop(beforeEventCount),
			resolvedAt = clock.instant(),
		)
		val status = if (stateAfter.result == null) BattleSessionStatus.ACTIVE else BattleSessionStatus.COMPLETED
		val endedAt = record.resolvedAt.takeIf { status == BattleSessionStatus.COMPLETED }
		val expiresAt = endedAt?.plus(capacity.recentSessionTtl)
		val nextRecords = session.turnRecords + record
		val updated = session.copy(
			status = status,
			revision = revisionAfter,
			state = stateAfter,
			requirements = requirementsDeriver.derive(stateAfter),
			turnRecords = nextRecords,
			updatedAt = record.resolvedAt,
			endedAt = endedAt,
			expiresAt = expiresAt,
			battleRecord = endedAt?.let {
				buildBattleRecord(entry, status, stateAfter, nextRecords, termination = null, endedAt = it)
			},
		)
		val result = TurnCommandResult(updated, record)
		entry.snapshot = updated
		entry.commandCache[command.commandId] = CachedTurnCommand(command, record)
		if (status != BattleSessionStatus.ACTIVE) {
			markTerminal(entry)
		}
		return result
	}

	private fun reconstructTurnCommandResult(
		entry: SessionEntry,
		cached: CachedTurnCommand,
	): TurnCommandResult {
		val current = entry.snapshot
		val record = cached.turnRecord
		if (current.revision == record.revisionAfter) {
			return TurnCommandResult(current, record)
		}
		val recordIndex = current.turnRecords.indexOfFirst { it.commandId == record.commandId }
		check(recordIndex >= 0) { "cached turn command is missing from session history" }
		val turnRecords = current.turnRecords.take(recordIndex + 1)
		check(turnRecords.last() == record) { "cached turn command differs from session history" }
		val stateAfter = replayTurns(entry, turnRecords)
		val status = if (stateAfter.result == null) BattleSessionStatus.ACTIVE else BattleSessionStatus.COMPLETED
		val endedAt = record.resolvedAt.takeIf { status == BattleSessionStatus.COMPLETED }
		val reconstructed = current.copy(
			status = status,
			revision = record.revisionAfter,
			state = stateAfter,
			requirements = requirementsDeriver.derive(stateAfter),
			turnRecords = turnRecords,
			updatedAt = record.resolvedAt,
			endedAt = endedAt,
			expiresAt = endedAt?.plus(capacity.recentSessionTtl),
			battleRecord = endedAt?.let {
				buildBattleRecord(entry, status, stateAfter, turnRecords, termination = null, endedAt = it)
			},
		)
		return TurnCommandResult(reconstructed, record)
	}

	private fun replayTurns(
		entry: SessionEntry,
		turnRecords: List<TurnRecord>,
	): BattleState {
		var current = engine.start(entry.initialState)
		check(current.events == entry.initialEvents) { "replay initial events differ from recorded events" }
		turnRecords.forEach { turn ->
			check(current.result == null) { "battle already ended before replay turn ${turn.turnNumber}" }
			val random = TracedBattleRandom(turn.randomTrace)
			val beforeEventCount = current.events.size
			val resolved = engine.resolveTurn(current, turn.submittedActions, random)
			check(random.isFullyConsumed()) {
				"replay random trace for turn ${turn.turnNumber} was not fully consumed"
			}
			check(resolved.events.drop(beforeEventCount) == turn.events) {
				"replay events differ at turn ${turn.turnNumber}"
			}
			current = resolved
		}
		return current
	}

	fun terminate(sessionId: String, command: TerminationCommand): TerminationResult {
		val entry = findEntry(sessionId)
		return synchronized(entry.lock) {
			entry.commandCache[command.commandId]?.let { cached ->
				if (cached !is CachedTerminationCommand || cached.command != command) {
					throw CommandPayloadConflictException(command.commandId)
				}
				return@synchronized cached.result
			}
			val session = entry.snapshot
			if (command.expectedRevision != session.revision) {
				throw SessionRevisionConflictException(command.expectedRevision, session.revision)
			}
			if (session.status != BattleSessionStatus.ACTIVE) {
				throw BattleSessionNotActiveException(sessionId, session.status)
			}
			val terminatedAt = clock.instant()
			val termination = SessionTermination(
				commandId = command.commandId,
				reason = command.reason,
				revisionBefore = session.revision,
				revisionAfter = session.revision + 1,
				terminatedAt = terminatedAt,
			)
			val record = buildBattleRecord(
				entry = entry,
				status = BattleSessionStatus.TERMINATED,
				finalState = session.state,
				turnRecords = session.turnRecords,
				termination = termination,
				endedAt = terminatedAt,
			)
			val updated = session.copy(
				status = BattleSessionStatus.TERMINATED,
				revision = termination.revisionAfter,
				requirements = TurnRequirements(emptyList()),
				updatedAt = terminatedAt,
				endedAt = terminatedAt,
				expiresAt = terminatedAt.plus(capacity.recentSessionTtl),
				battleRecord = record,
			)
			val result = TerminationResult(updated, termination)
			entry.snapshot = updated
			entry.commandCache[command.commandId] = CachedTerminationCommand(command, result)
			markTerminal(entry)
			result
		}
	}

	private fun buildBattleRecord(
		entry: SessionEntry,
		status: BattleSessionStatus,
		finalState: io.github.lishangbu.battleengine.model.BattleState,
		turnRecords: List<TurnRecord>,
		termination: SessionTermination?,
		endedAt: java.time.Instant,
	): BattleRecord =
		BattleRecord(
			sessionId = entry.snapshot.sessionId,
			status = status,
			replay = BattleReplay(
				initialState = entry.initialState,
				initialEvents = entry.initialEvents,
				turns = turnRecords.map { turn ->
					BattleReplayTurn(
						turnNumber = turn.turnNumber,
						submittedActions = turn.submittedActions,
						randomTrace = turn.randomTrace,
						events = turn.events,
					)
				},
				finalState = finalState,
			),
			termination = termination,
			startedAt = entry.snapshot.createdAt,
			endedAt = endedAt,
		)

	private fun findEntry(sessionId: String): SessionEntry =
		synchronized(registryLock) {
			pruneExpiredRecentSessions(clock.instant())
			activeSessions[sessionId] ?: recentSessions[sessionId] ?: throw BattleSessionNotFoundException(sessionId)
		}

	private fun markTerminal(entry: SessionEntry) {
		synchronized(registryLock) {
			val sessionId = entry.snapshot.sessionId
			activeSessions.remove(sessionId)
			if (capacity.maxRecentSessions > 0) {
				recentSessions[sessionId] = entry
			}
			while (recentSessions.size > capacity.maxRecentSessions) {
				val oldestSessionId = recentSessions.values
					.minWith(
						compareBy<SessionEntry> { requireNotNull(it.snapshot.endedAt) }
							.thenBy { it.snapshot.sessionId },
					)
					.snapshot.sessionId
				recentSessions.remove(oldestSessionId)
			}
		}
	}

	private fun pruneExpiredRecentSessions(now: java.time.Instant) {
		val expiredSessionIds = recentSessions.values
			.filter { entry -> entry.snapshot.expiresAt?.let { !it.isAfter(now) } == true }
			.map { entry -> entry.snapshot.sessionId }
		expiredSessionIds.forEach(recentSessions::remove)
	}

	private fun ensureActiveCapacity() {
		if (activeSessions.size >= capacity.maxActiveSessions) {
			throw SessionCapacityExhaustedException(capacity.retryAfter)
		}
	}
}
