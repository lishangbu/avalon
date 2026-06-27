package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 地点索引响应。
 */
@Schema(name = "GameLocationGameIndicesResponse", description = "地点索引响应。")
data class GameLocationGameIndicesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("location_id")
	@field:Schema(description = "地点 ID")
	val locationId: Long?,
	@get:JsonProperty("game_index")
	@field:Schema(description = "索引")
	val gameIndex: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameLocationGameIndicesResponse =
			GameLocationGameIndicesResponse(
				id = record.id,
				locationId = record.longField("location_id"),
				gameIndex = record.intField("game_index")
			)
	}
}
