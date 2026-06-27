package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 数值项性格影响写入请求。
 */
@Schema(name = "GameStatNatureEffectsRequest", description = "数值项性格影响写入请求。")
data class GameStatNatureEffectsRequest(
	@param:JsonProperty("stat_id")
	@get:JsonProperty("stat_id")
	@field:Schema(description = "数值项 ID")
	val statId: Long? = null,
	@param:JsonProperty("nature_id")
	@get:JsonProperty("nature_id")
	@field:Schema(description = "性格 ID")
	val natureId: Long? = null,
	@param:JsonProperty("effect_type")
	@get:JsonProperty("effect_type")
	@field:Schema(description = "影响类型")
	val effectType: String? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"stat_id" to statId,
		"nature_id" to natureId,
		"effect_type" to effectType,
		)
}
