package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 活动能力性格影响写入请求。
 */
@Schema(name = "GameEventStatNatureEffectsRequest", description = "活动能力性格影响写入请求。")
data class GameEventStatNatureEffectsRequest(
	@param:JsonProperty("event_stat_id")
	@get:JsonProperty("event_stat_id")
	@field:Schema(description = "活动能力项 ID")
	val eventStatId: Long? = null,
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
		"event_stat_id" to eventStatId,
		"nature_id" to natureId,
		"effect_type" to effectType,
		)
}
