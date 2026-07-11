package io.github.lishangbu.match.game

enum class MatchStatus { STARTING, ACTIVE, COMPLETED, INTERRUPTED }
enum class MatchInterruptionReason { START_FAILED, RUNTIME_LOST, RUNTIME_FAILED }
enum class MatchOutcome { WIN, DRAW, NO_CONTEST }
enum class MatchCompletionReason { BATTLE, FORFEIT, TIMEOUT }

@ConsistentCopyVisibility
data class MatchResult private constructor(
	val outcome: MatchOutcome,
	val reason: MatchCompletionReason,
	val winnerTrainerId: Long?,
	val battleReason: String?,
) {
	companion object {
		fun win(winnerTrainerId: Long, reason: MatchCompletionReason, battleReason: String? = null): MatchResult {
			require(reason != MatchCompletionReason.BATTLE || !battleReason.isNullOrBlank())
			require(reason == MatchCompletionReason.BATTLE || battleReason == null)
			return MatchResult(MatchOutcome.WIN, reason, winnerTrainerId, battleReason)
		}
		fun draw(battleReason: String) = MatchResult(MatchOutcome.DRAW, MatchCompletionReason.BATTLE, null, battleReason)
		fun noContest() = MatchResult(MatchOutcome.NO_CONTEST, MatchCompletionReason.TIMEOUT, null, null)
	}
}

@ConsistentCopyVisibility
data class MatchLifecycle private constructor(
	val status: MatchStatus,
	val battleSessionId: String?,
	val result: MatchResult?,
	val interruptionReason: MatchInterruptionReason?,
	val revision: Long,
) {
	companion object { fun starting() = MatchLifecycle(MatchStatus.STARTING, null, null, null, 0) }

	fun activate(sessionId: String): MatchLifecycle {
		require(status == MatchStatus.STARTING)
		return copy(status = MatchStatus.ACTIVE, battleSessionId = sessionId, revision = revision + 1)
	}

	fun complete(result: MatchResult): MatchLifecycle {
		if (status == MatchStatus.COMPLETED || status == MatchStatus.INTERRUPTED) throw MatchAlreadyTerminalException()
		require(status == MatchStatus.ACTIVE)
		return copy(status = MatchStatus.COMPLETED, result = result, revision = revision + 1)
	}

	fun interrupt(reason: MatchInterruptionReason): MatchLifecycle {
		if (status == MatchStatus.COMPLETED || status == MatchStatus.INTERRUPTED) throw MatchAlreadyTerminalException()
		return copy(status = MatchStatus.INTERRUPTED, interruptionReason = reason, revision = revision + 1)
	}
}

class MatchAlreadyTerminalException : IllegalStateException("Match is already terminal")
