package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 道具资料响应。
 */
@Schema(name = "GameItemResponse", description = "道具资料响应。")
data class GameItemResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("category_id")
	@field:Schema(description = "分类 ID")
	val categoryId: Long?,
	@get:JsonProperty("cost")
	@field:Schema(description = "价格")
	val cost: Int?,
	@get:JsonProperty("fling_power")
	@field:Schema(description = "投掷威力")
	val flingPower: Int?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameItemResponse =
			GameItemResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				categoryId = record.longField("category_id"),
				cost = record.intField("cost"),
				flingPower = record.intField("fling_power"),
				enabled = record.booleanField("enabled")
			)
	}
}
