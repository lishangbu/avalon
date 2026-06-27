package io.github.lishangbu.gamedata.model

/**
 * 游戏资料通用写入请求。
 *
 * 每张资料表的可写字段由后端白名单定义，未知字段会被拒绝。
 */
data class GameDataRecordRequest(
	val fields: Map<String, Any?> = emptyMap(),
)
