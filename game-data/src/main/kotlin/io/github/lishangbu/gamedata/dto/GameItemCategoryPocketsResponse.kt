package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 道具分类口袋响应。
 */
@Schema(name = "GameItemCategoryPocketsResponse", description = "道具分类口袋响应。")
data class GameItemCategoryPocketsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("category_id")
	@field:Schema(description = "分类 ID")
	val categoryId: Long?,
	@get:JsonProperty("pocket_id")
	@field:Schema(description = "口袋 ID")
	val pocketId: Long?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameItemCategoryPocketsResponse =
			GameItemCategoryPocketsResponse(
				id = record.id,
				categoryId = record.longField("category_id"),
				pocketId = record.longField("pocket_id")
			)
	}
}
