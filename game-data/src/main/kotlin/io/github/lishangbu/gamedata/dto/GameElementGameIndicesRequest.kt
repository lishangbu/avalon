package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 属性索引写入请求。
 */
@Schema(name = "GameElementGameIndicesRequest", description = "属性索引写入请求。")
data class GameElementGameIndicesRequest(
	@param:JsonProperty("element_id")
	@get:JsonProperty("element_id")
	@field:Schema(description = "属性 ID")
	val elementId: Long? = null,
	@param:JsonProperty("game_index")
	@get:JsonProperty("game_index")
	@field:Schema(description = "索引")
	val gameIndex: Int? = null
)
