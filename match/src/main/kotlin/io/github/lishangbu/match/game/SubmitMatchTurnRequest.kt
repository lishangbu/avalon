package io.github.lishangbu.match.game

/** 锁定当前 Trainer 的完整回合选择。 */
data class SubmitMatchTurnRequest(
	var submissionId: String = "",
	var expectedRevision: Long = -1,
	var actions: List<MatchTurnAction> = emptyList(),
)
