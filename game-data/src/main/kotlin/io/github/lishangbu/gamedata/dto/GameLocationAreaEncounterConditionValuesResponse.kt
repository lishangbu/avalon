package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 区域遭遇条件绑定响应。
 */
@Schema(name = "GameLocationAreaEncounterConditionValuesResponse", description = "区域遭遇条件绑定响应。")
data class GameLocationAreaEncounterConditionValuesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("encounter_id")
	@field:Schema(description = "遭遇 ID")
	val encounterId: Long?,
	@get:JsonProperty("condition_value_id")
	@field:Schema(description = "遭遇条件值 ID")
	val conditionValueId: Long?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameLocationAreaEncounterConditionValuesResponse =
			GameLocationAreaEncounterConditionValuesResponse(
				id = record.id,
				encounterId = record.longField("encounter_id"),
				conditionValueId = record.longField("condition_value_id")
			)
	}
}
