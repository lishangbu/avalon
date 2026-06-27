package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 区域遭遇条件绑定写入请求。
 */
@Schema(name = "GameLocationAreaEncounterConditionValuesRequest", description = "区域遭遇条件绑定写入请求。")
data class GameLocationAreaEncounterConditionValuesRequest(
	@param:JsonProperty("encounter_id")
	@get:JsonProperty("encounter_id")
	@field:Schema(description = "遭遇 ID")
	val encounterId: Long? = null,
	@param:JsonProperty("condition_value_id")
	@get:JsonProperty("condition_value_id")
	@field:Schema(description = "遭遇条件值 ID")
	val conditionValueId: Long? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"encounter_id" to encounterId,
		"condition_value_id" to conditionValueId,
		)
}
