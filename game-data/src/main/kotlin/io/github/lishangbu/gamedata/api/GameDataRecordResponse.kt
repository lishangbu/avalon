package io.github.lishangbu.gamedata.api

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 游戏资料通用记录响应。
 */
@Schema(name = "GameDataRecordResponse", description = "游戏资料通用记录响应。")
data class GameDataRecordResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@field:Schema(description = "字段名到字段值的映射。")
	val fields: Map<String, Any?>,
)
