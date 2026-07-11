package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/** 成功执行 Turn Command 后的 Session 与新增 Turn Record。 */
@Schema(description = "成功执行 Turn Command 后的 Session 与新增 Turn Record。")
data class BattleSessionTurnResponse(
	@field:Schema(description = "命令执行后的权威 Session 快照。")
	val session: BattleSessionResponse,
	@field:Schema(description = "本次命令追加的 Turn Record。")
	val turnRecord: BattleSessionTurnRecordResponse,
)
