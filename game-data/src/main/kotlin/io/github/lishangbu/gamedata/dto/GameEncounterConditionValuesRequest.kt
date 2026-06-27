package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 遭遇条件值写入请求。
 */
@Schema(name = "GameEncounterConditionValuesRequest", description = "遭遇条件值写入请求。")
data class GameEncounterConditionValuesRequest(
	@param:JsonProperty("code")
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String? = null,
	@param:JsonProperty("name")
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String? = null,
	@param:JsonProperty("condition_id")
	@get:JsonProperty("condition_id")
	@field:Schema(description = "遭遇条件 ID")
	val conditionId: Long? = null,
	@param:JsonProperty("enabled")
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"code" to code,
		"name" to name,
		"condition_id" to conditionId,
		"enabled" to enabled,
		)
}
