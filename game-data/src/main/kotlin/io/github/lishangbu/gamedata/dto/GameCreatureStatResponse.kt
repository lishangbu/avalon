package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 生物数值绑定响应。
 */
@Schema(name = "GameCreatureStatResponse", description = "生物数值绑定响应。")
data class GameCreatureStatResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("creature_id")
	@field:Schema(description = "生物 ID")
	val creatureId: Long?,
	@get:JsonProperty("stat_id")
	@field:Schema(description = "数值项 ID")
	val statId: Long?,
	@get:JsonProperty("base_value")
	@field:Schema(description = "基础值")
	val baseValue: Int?,
	@get:JsonProperty("effort")
	@field:Schema(description = "努力收益")
	val effort: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameCreatureStatResponse =
			GameCreatureStatResponse(
				id = record.id,
				creatureId = record.longField("creature_id"),
				statId = record.longField("stat_id"),
				baseValue = record.intField("base_value"),
				effort = record.intField("effort")
			)
	}
}
