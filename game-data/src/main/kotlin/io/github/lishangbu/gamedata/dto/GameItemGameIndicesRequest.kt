package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 道具索引写入请求。
 */
@Schema(name = "GameItemGameIndicesRequest", description = "道具索引写入请求。")
data class GameItemGameIndicesRequest(
	@param:JsonProperty("item_id")
	@get:JsonProperty("item_id")
	@field:Schema(description = "道具 ID")
	val itemId: Long? = null,
	@param:JsonProperty("game_index")
	@get:JsonProperty("game_index")
	@field:Schema(description = "索引")
	val gameIndex: Int? = null
)
