package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 性格活动能力变化写入请求。
 */
@Schema(name = "GameNatureEventStatChangesRequest", description = "性格活动能力变化写入请求。")
data class GameNatureEventStatChangesRequest(
	@param:JsonProperty("nature_id")
	@get:JsonProperty("nature_id")
	@field:Schema(description = "性格 ID")
	val natureId: Long? = null,
	@param:JsonProperty("event_stat_id")
	@get:JsonProperty("event_stat_id")
	@field:Schema(description = "活动能力项 ID")
	val eventStatId: Long? = null,
	@param:JsonProperty("max_change")
	@get:JsonProperty("max_change")
	@field:Schema(description = "最大变化")
	val maxChange: Int? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"nature_id" to natureId,
		"event_stat_id" to eventStatId,
		"max_change" to maxChange,
		)
}
