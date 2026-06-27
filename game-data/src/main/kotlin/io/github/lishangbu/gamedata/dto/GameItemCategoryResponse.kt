package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 道具分类响应。
 */
@Schema(name = "GameItemCategoryResponse", description = "道具分类响应。")
data class GameItemCategoryResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("sort_order")
	@field:Schema(description = "排序")
	val sortOrder: Int?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameItemCategoryResponse =
			GameItemCategoryResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				sortOrder = record.intField("sort_order"),
				enabled = record.booleanField("enabled")
			)
	}
}
