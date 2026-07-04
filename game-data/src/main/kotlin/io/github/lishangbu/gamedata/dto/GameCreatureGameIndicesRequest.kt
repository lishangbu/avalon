package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 精灵索引写入请求。
 */
@Schema(name = "GameCreatureGameIndicesRequest", description = "精灵索引写入请求。")
data class GameCreatureGameIndicesRequest(
	@param:JsonProperty("creature_id")
	@get:JsonProperty("creature_id")
	@field:Schema(description = "精灵 ID")
	val creatureId: Long? = null,
	@param:JsonProperty("game_index")
	@get:JsonProperty("game_index")
	@field:Schema(description = "索引")
	val gameIndex: Int? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"creature_id" to creatureId,
		"game_index" to gameIndex,
		)
}
