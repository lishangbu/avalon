package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 地点索引写入请求。
 */
@Schema(name = "GameLocationGameIndicesRequest", description = "地点索引写入请求。")
data class GameLocationGameIndicesRequest(
	@param:JsonProperty("location_id")
	@get:JsonProperty("location_id")
	@field:Schema(description = "地点 ID")
	val locationId: Long? = null,
	@param:JsonProperty("game_index")
	@get:JsonProperty("game_index")
	@field:Schema(description = "索引")
	val gameIndex: Int? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"location_id" to locationId,
		"game_index" to gameIndex,
		)
}
