package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 遭遇条件值响应。
 */
@Schema(name = "GameEncounterConditionValuesResponse", description = "遭遇条件值响应。")
data class GameEncounterConditionValuesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("condition_id")
	@field:Schema(description = "遭遇条件 ID")
	val conditionId: Long?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameEncounterConditionValuesResponse =
			GameEncounterConditionValuesResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				conditionId = record.longField("condition_id"),
				enabled = record.booleanField("enabled")
			)
	}
}
