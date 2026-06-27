package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 属性索引响应。
 */
@Schema(name = "GameElementGameIndicesResponse", description = "属性索引响应。")
data class GameElementGameIndicesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("element_id")
	@field:Schema(description = "属性 ID")
	val elementId: Long?,
	@get:JsonProperty("game_index")
	@field:Schema(description = "索引")
	val gameIndex: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameElementGameIndicesResponse =
			GameElementGameIndicesResponse(
				id = record.id,
				elementId = record.longField("element_id"),
				gameIndex = record.intField("game_index")
			)
	}
}
