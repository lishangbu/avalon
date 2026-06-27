package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 评价效果响应。
 */
@Schema(name = "GameContestEffectsResponse", description = "评价效果响应。")
data class GameContestEffectsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("appeal")
	@field:Schema(description = "吸引力")
	val appeal: Int?,
	@get:JsonProperty("jam")
	@field:Schema(description = "干扰值")
	val jam: Int?,
	@get:JsonProperty("effect")
	@field:Schema(description = "效果")
	val effect: String?,
	@get:JsonProperty("flavor_text")
	@field:Schema(description = "风味说明")
	val flavorText: String?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameContestEffectsResponse =
			GameContestEffectsResponse(
				id = record.id,
				appeal = record.intField("appeal"),
				jam = record.intField("jam"),
				effect = record.stringField("effect"),
				flavorText = record.stringField("flavor_text")
			)
	}
}
