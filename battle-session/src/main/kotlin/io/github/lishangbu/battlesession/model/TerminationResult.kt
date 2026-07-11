package io.github.lishangbu.battlesession.model

/** 返回终止后的权威 Snapshot 与对应终止事实。 */
data class TerminationResult(
	val session: BattleSessionSnapshot,
	val termination: SessionTermination,
)
