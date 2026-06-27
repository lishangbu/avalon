package io.github.lishangbu.gamedata.api

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 游戏资料通用分页响应。
 */
@Schema(name = "GameDataPageResponse", description = "游戏资料通用分页响应。")
data class GameDataPageResponse(
	@field:Schema(description = "当前页记录。")
	val rows: List<GameDataRecordResponse>,
	@field:Schema(description = "总记录数。", example = "1351")
	val totalRowCount: Long,
	@field:Schema(description = "总页数。", example = "28")
	val totalPageCount: Int,
	@field:Schema(description = "当前页码，从 0 开始。", example = "0")
	val page: Int,
	@field:Schema(description = "每页大小。", example = "50")
	val size: Int,
)
