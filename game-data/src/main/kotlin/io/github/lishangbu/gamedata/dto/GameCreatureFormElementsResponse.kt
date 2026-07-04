package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 精灵形态属性响应。
 */
@Schema(name = "GameCreatureFormElementsResponse", description = "精灵形态属性响应。")
data class GameCreatureFormElementsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("form_id")
	@field:Schema(description = "形态 ID")
	val formId: Long?,
	@get:JsonProperty("element_id")
	@field:Schema(description = "属性 ID")
	val elementId: Long?,
	@get:JsonProperty("slot_order")
	@field:Schema(description = "槽位顺序")
	val slotOrder: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameCreatureFormElementsResponse =
			GameCreatureFormElementsResponse(
				id = record.id,
				formId = record.longField("form_id"),
				elementId = record.longField("element_id"),
				slotOrder = record.intField("slot_order")
			)
	}
}
