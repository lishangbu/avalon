package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 特性详情写入请求。
 */
@Schema(name = "GameAbilityDetailsRequest", description = "特性详情写入请求。")
data class GameAbilityDetailsRequest(
	@param:JsonProperty("ability_id")
	@get:JsonProperty("ability_id")
	@field:Schema(description = "特性 ID")
	val abilityId: Long? = null,
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
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"ability_id" to abilityId,
		"effect" to effect,
		"short_effect" to shortEffect,
		"flavor_text" to flavorText,
		)
}
