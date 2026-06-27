package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 评价效果写入请求。
 */
@Schema(name = "GameContestEffectsRequest", description = "评价效果写入请求。")
data class GameContestEffectsRequest(
	@param:JsonProperty("appeal")
	@get:JsonProperty("appeal")
	@field:Schema(description = "吸引力")
	val appeal: Int? = null,
	@param:JsonProperty("jam")
	@get:JsonProperty("jam")
	@field:Schema(description = "干扰值")
	val jam: Int? = null,
	@param:JsonProperty("effect")
	@get:JsonProperty("effect")
	@field:Schema(description = "效果")
	val effect: String? = null,
	@param:JsonProperty("flavor_text")
	@get:JsonProperty("flavor_text")
	@field:Schema(description = "风味说明")
	val flavorText: String? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"appeal" to appeal,
		"jam" to jam,
		"effect" to effect,
		"flavor_text" to flavorText,
		)
}
