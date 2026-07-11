package io.github.lishangbu.battlesession

data class TerminationResult(
	val session: BattleSessionSnapshot,
	val termination: SessionTermination,
)
