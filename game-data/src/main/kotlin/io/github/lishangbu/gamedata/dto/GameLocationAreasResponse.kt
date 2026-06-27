package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 地点区域响应。
 */
@Schema(name = "GameLocationAreasResponse", description = "地点区域响应。")
data class GameLocationAreasResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("location_id")
	@field:Schema(description = "地点 ID")
	val locationId: Long?,
	@get:JsonProperty("game_index")
	@field:Schema(description = "索引")
	val gameIndex: Int?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameLocationAreasResponse =
			GameLocationAreasResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				locationId = record.longField("location_id"),
				gameIndex = record.intField("game_index"),
				enabled = record.booleanField("enabled")
			)
	}
}
