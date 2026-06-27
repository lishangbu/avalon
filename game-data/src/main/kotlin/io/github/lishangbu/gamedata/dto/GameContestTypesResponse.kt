package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 评分类别响应。
 */
@Schema(name = "GameContestTypesResponse", description = "评分类别响应。")
data class GameContestTypesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("color")
	@field:Schema(description = "颜色")
	val color: String?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameContestTypesResponse =
			GameContestTypesResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				color = record.stringField("color"),
				enabled = record.booleanField("enabled")
			)
	}
}
