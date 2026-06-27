package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 高级评价效果响应。
 */
@Schema(name = "GameAdvancedContestEffectsResponse", description = "高级评价效果响应。")
data class GameAdvancedContestEffectsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("appeal")
	@field:Schema(description = "吸引力")
	val appeal: Int?,
	@get:JsonProperty("flavor_text")
	@field:Schema(description = "风味说明")
	val flavorText: String?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameAdvancedContestEffectsResponse =
			GameAdvancedContestEffectsResponse(
				id = record.id,
				appeal = record.intField("appeal"),
				flavorText = record.stringField("flavor_text")
			)
	}
}
