package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 精灵索引响应。
 */
@Schema(name = "GameCreatureGameIndicesResponse", description = "精灵索引响应。")
data class GameCreatureGameIndicesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("creature_id")
	@field:Schema(description = "精灵 ID")
	val creatureId: Long?,
	@get:JsonProperty("game_index")
	@field:Schema(description = "索引")
	val gameIndex: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameCreatureGameIndicesResponse =
			GameCreatureGameIndicesResponse(
				id = record.id,
				creatureId = record.longField("creature_id"),
				gameIndex = record.intField("game_index")
			)
	}
}
