package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 精灵属性绑定响应。
 */
@Schema(name = "GameCreatureElementResponse", description = "精灵属性绑定响应。")
data class GameCreatureElementResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("creature_id")
	@field:Schema(description = "精灵 ID")
	val creatureId: Long?,
	@get:JsonProperty("element_id")
	@field:Schema(description = "属性 ID")
	val elementId: Long?,
	@get:JsonProperty("slot_order")
	@field:Schema(description = "槽位")
	val slotOrder: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameCreatureElementResponse =
			GameCreatureElementResponse(
				id = record.id,
				creatureId = record.longField("creature_id"),
				elementId = record.longField("element_id"),
				slotOrder = record.intField("slot_order")
			)
	}
}
