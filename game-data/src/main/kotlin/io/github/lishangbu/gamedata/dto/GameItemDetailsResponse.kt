package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 道具详情响应。
 */
@Schema(name = "GameItemDetailsResponse", description = "道具详情响应。")
data class GameItemDetailsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("item_id")
	@field:Schema(description = "道具 ID")
	val itemId: Long?,
	@get:JsonProperty("fling_effect_id")
	@field:Schema(description = "投掷效果 ID")
	val flingEffectId: Long?,
	@get:JsonProperty("effect")
	@field:Schema(description = "效果")
	val effect: String?,
	@get:JsonProperty("short_effect")
	@field:Schema(description = "短效果")
	val shortEffect: String?,
	@get:JsonProperty("flavor_text")
	@field:Schema(description = "风味说明")
	val flavorText: String?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameItemDetailsResponse =
			GameItemDetailsResponse(
				id = record.id,
				itemId = record.longField("item_id"),
				flingEffectId = record.longField("fling_effect_id"),
				effect = record.stringField("effect"),
				shortEffect = record.stringField("short_effect"),
				flavorText = record.stringField("flavor_text")
			)
	}
}
