package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 性格活动能力变化响应。
 */
@Schema(name = "GameNatureEventStatChangesResponse", description = "性格活动能力变化响应。")
data class GameNatureEventStatChangesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("nature_id")
	@field:Schema(description = "性格 ID")
	val natureId: Long?,
	@get:JsonProperty("event_stat_id")
	@field:Schema(description = "活动能力项 ID")
	val eventStatId: Long?,
	@get:JsonProperty("max_change")
	@field:Schema(description = "最大变化")
	val maxChange: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameNatureEventStatChangesResponse =
			GameNatureEventStatChangesResponse(
				id = record.id,
				natureId = record.longField("nature_id"),
				eventStatId = record.longField("event_stat_id"),
				maxChange = record.intField("max_change")
			)
	}
}
