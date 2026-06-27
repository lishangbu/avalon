package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 成长等级经验响应。
 */
@Schema(name = "GameGrowthRateLevelsResponse", description = "成长等级经验响应。")
data class GameGrowthRateLevelsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("growth_rate_id")
	@field:Schema(description = "成长速率 ID")
	val growthRateId: Long?,
	@get:JsonProperty("level")
	@field:Schema(description = "等级")
	val level: Int?,
	@get:JsonProperty("experience")
	@field:Schema(description = "经验")
	val experience: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameGrowthRateLevelsResponse =
			GameGrowthRateLevelsResponse(
				id = record.id,
				growthRateId = record.longField("growth_rate_id"),
				level = record.intField("level"),
				experience = record.intField("experience")
			)
	}
}
