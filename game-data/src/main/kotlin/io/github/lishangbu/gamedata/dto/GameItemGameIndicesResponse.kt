package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 道具索引响应。
 */
@Schema(name = "GameItemGameIndicesResponse", description = "道具索引响应。")
data class GameItemGameIndicesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("item_id")
	@field:Schema(description = "道具 ID")
	val itemId: Long?,
	@get:JsonProperty("game_index")
	@field:Schema(description = "索引")
	val gameIndex: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameItemGameIndicesResponse =
			GameItemGameIndicesResponse(
				id = record.id,
				itemId = record.longField("item_id"),
				gameIndex = record.intField("game_index")
			)
	}
}
