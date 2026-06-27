package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 树果口味响应。
 */
@Schema(name = "GameBerryFlavorsResponse", description = "树果口味响应。")
data class GameBerryFlavorsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("contest_type_id")
	@field:Schema(description = "评分类别 ID")
	val contestTypeId: Long?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameBerryFlavorsResponse =
			GameBerryFlavorsResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				contestTypeId = record.longField("contest_type_id"),
				enabled = record.booleanField("enabled")
			)
	}
}
