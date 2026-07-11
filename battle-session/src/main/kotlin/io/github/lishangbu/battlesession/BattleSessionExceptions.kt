package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.BattleActionViolation
import java.time.Duration

class BattleSessionNotFoundException(
	val sessionId: String,
) : NoSuchElementException("battle session not found: $sessionId")

class IncompleteTurnCommandException(
	val requirements: TurnRequirements,
) : IllegalArgumentException("turn command must contain every required selection and no other actions")

class InvalidTurnActionsException(
	val violations: List<BattleActionViolation>,
) : IllegalArgumentException("turn command contains invalid action combination: ${violations.joinToString { it.code }}")

class CommandPayloadConflictException(
	val commandId: String,
) : IllegalStateException("commandId was already used with a different payload: $commandId")

class SessionRevisionConflictException(
	val expectedRevision: Long,
	val actualRevision: Long,
) : IllegalStateException("expected session revision $expectedRevision but was $actualRevision")

class BattleSessionNotActiveException(
	val sessionId: String,
	val status: BattleSessionStatus,
) : IllegalStateException("battle session is not active: $sessionId ($status)")

class SessionCapacityExhaustedException(
	val retryAfter: Duration,
) : IllegalStateException("Battle Session Runtime active capacity is exhausted") {
	val code: String = "battle-session.capacity-exhausted"
}
