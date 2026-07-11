package io.github.lishangbu.match.challenge

import java.time.Instant

enum class ChallengeStatus { PENDING, ACCEPTED, REJECTED, CANCELLED, EXPIRED, SUPERSEDED }
enum class ChallengeCancellationReason { WITHDRAWN, TRAINER_ARCHIVED, ROSTER_INVALIDATED }

@ConsistentCopyVisibility
data class ChallengeLifecycle private constructor(
	val status: ChallengeStatus,
	val cancellationReason: ChallengeCancellationReason?,
	val revision: Long,
	val createdAt: Instant,
	val resolvedAt: Instant?,
) {
	companion object {
		fun pending(createdAt: Instant) = ChallengeLifecycle(ChallengeStatus.PENDING, null, 0, createdAt, null)
	}

	fun resolve(
		target: ChallengeStatus,
		at: Instant,
		cancellationReason: ChallengeCancellationReason? = null,
	): ChallengeLifecycle {
		if (status != ChallengeStatus.PENDING) throw ChallengeAlreadyResolvedException()
		if (target == ChallengeStatus.PENDING) throw InvalidChallengeTransitionException()
		if ((target == ChallengeStatus.CANCELLED) != (cancellationReason != null)) {
			throw InvalidChallengeTransitionException()
		}
		return copy(status = target, cancellationReason = cancellationReason, revision = revision + 1, resolvedAt = at)
	}
}

class ChallengeAlreadyResolvedException : IllegalStateException("Challenge has already been resolved")
class InvalidChallengeTransitionException : IllegalArgumentException("Challenge transition is invalid")
