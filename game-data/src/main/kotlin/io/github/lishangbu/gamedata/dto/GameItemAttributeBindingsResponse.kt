package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 道具属性绑定响应。
 */
@Schema(name = "GameItemAttributeBindingsResponse", description = "道具属性绑定响应。")
data class GameItemAttributeBindingsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("item_id")
	@field:Schema(description = "道具 ID")
	val itemId: Long?,
	@get:JsonProperty("attribute_id")
	@field:Schema(description = "属性 ID")
	val attributeId: Long?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameItemAttributeBindingsResponse =
			GameItemAttributeBindingsResponse(
				id = record.id,
				itemId = record.longField("item_id"),
				attributeId = record.longField("attribute_id")
			)
	}
}
