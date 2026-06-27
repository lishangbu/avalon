package io.github.lishangbu.gamedata.model

/**
 * 游戏资料通用记录响应。
 */
data class GameDataRecordResponse(
	val id: Long,
	val fields: Map<String, Any?>,
)
