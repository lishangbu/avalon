package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 高级评价效果写入请求。
 */
@Schema(name = "GameAdvancedContestEffectsRequest", description = "高级评价效果写入请求。")
data class GameAdvancedContestEffectsRequest(
	@param:JsonProperty("appeal")
	@get:JsonProperty("appeal")
	@field:Schema(description = "吸引力")
	val appeal: Int? = null,
	@param:JsonProperty("flavor_text")
	@get:JsonProperty("flavor_text")
	@field:Schema(description = "风味说明")
	val flavorText: String? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"appeal" to appeal,
		"flavor_text" to flavorText,
		)
}
