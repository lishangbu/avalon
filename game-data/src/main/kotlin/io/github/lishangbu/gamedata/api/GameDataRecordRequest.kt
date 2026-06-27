package io.github.lishangbu.gamedata.api

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 游戏资料通用写入请求。
 *
 * 每张资料表的可写字段由后端白名单定义，未知字段会被拒绝。
 */
@Schema(name = "GameDataRecordRequest", description = "游戏资料通用写入请求。fields 的键必须是该资料表允许写入的字段名。")
data class GameDataRecordRequest(
	@field:Schema(description = "字段名到字段值的映射。", example = """{"name":"妙蛙种子","enabled":true}""")
	val fields: Map<String, Any?> = emptyMap(),
)
