package io.github.lishangbu.battlesession.model

/** 返回成功提交后的权威 Snapshot 与本次追加的 Turn Record。 */
data class TurnCommandResult(
	val session: BattleSessionSnapshot,
	val turnRecord: TurnRecord,
)
