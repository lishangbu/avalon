package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 道具详情写入请求。
 */
@Schema(name = "GameItemDetailsRequest", description = "道具详情写入请求。")
data class GameItemDetailsRequest(
	@param:JsonProperty("item_id")
	@get:JsonProperty("item_id")
	@field:Schema(description = "道具 ID")
	val itemId: Long? = null,
	@param:JsonProperty("fling_effect_id")
	@get:JsonProperty("fling_effect_id")
	@field:Schema(description = "投掷效果 ID")
	val flingEffectId: Long? = null,
	@param:JsonProperty("effect")
	@get:JsonProperty("effect")
	@field:Schema(description = "效果")
	val effect: String? = null,
	@param:JsonProperty("short_effect")
	@get:JsonProperty("short_effect")
	@field:Schema(description = "短效果")
	val shortEffect: String? = null,
	@param:JsonProperty("flavor_text")
	@get:JsonProperty("flavor_text")
	@field:Schema(description = "风味说明")
	val flavorText: String? = null
)
