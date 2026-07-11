package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/** OpenAPI 中显式绑定的 Turn Record 列表分页响应。 */
@Schema(name = "PageBattleSessionTurnRecordResponse", description = "Battle Session Turn Record 分页列表。")
data class BattleSessionTurnRecordPageResponse(
	val rows: List<BattleSessionTurnRecordResponse>,
	val totalRowCount: Long,
	val totalPageCount: Long,
)
