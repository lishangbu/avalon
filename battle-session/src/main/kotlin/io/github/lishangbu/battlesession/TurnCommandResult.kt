package io.github.lishangbu.battlesession

data class TurnCommandResult(
	val session: BattleSessionSnapshot,
	val turnRecord: TurnRecord,
)
