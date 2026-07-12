package io.github.lishangbu.match.game

/** Match 的不可变终局判定，显式区分胜负结果、完成原因和 Battle 侧原因。 */
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
