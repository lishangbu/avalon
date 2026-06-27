package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 生物持有道具响应。
 */
@Schema(name = "GameCreatureHeldItemsResponse", description = "生物持有道具响应。")
data class GameCreatureHeldItemsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("creature_id")
	@field:Schema(description = "生物 ID")
	val creatureId: Long?,
	@get:JsonProperty("item_id")
	@field:Schema(description = "道具 ID")
	val itemId: Long?,
	@get:JsonProperty("rarity")
	@field:Schema(description = "稀有度")
	val rarity: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameCreatureHeldItemsResponse =
			GameCreatureHeldItemsResponse(
				id = record.id,
				creatureId = record.longField("creature_id"),
				itemId = record.longField("item_id"),
				rarity = record.intField("rarity")
			)
	}
}
