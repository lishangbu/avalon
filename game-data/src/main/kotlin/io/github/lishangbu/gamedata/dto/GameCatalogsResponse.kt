package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 图鉴目录响应。
 */
@Schema(name = "GameCatalogsResponse", description = "图鉴目录响应。")
data class GameCatalogsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("region_id")
	@field:Schema(description = "地区 ID")
	val regionId: Long?,
	@get:JsonProperty("main_series")
	@field:Schema(description = "主体资料")
	val mainSeries: Boolean?,
	@get:JsonProperty("description")
	@field:Schema(description = "说明")
	val description: String?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameCatalogsResponse =
			GameCatalogsResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				regionId = record.longField("region_id"),
				mainSeries = record.booleanField("main_series"),
				description = record.stringField("description"),
				enabled = record.booleanField("enabled")
			)
	}
}
