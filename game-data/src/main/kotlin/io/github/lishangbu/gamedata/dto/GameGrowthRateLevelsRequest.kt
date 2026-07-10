package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 成长等级经验写入请求。
 */
@Schema(name = "GameGrowthRateLevelsRequest", description = "成长等级经验写入请求。")
data class GameGrowthRateLevelsRequest(
	@param:JsonProperty("growth_rate_id")
	@get:JsonProperty("growth_rate_id")
	@field:Schema(description = "成长速率 ID")
	val growthRateId: Long? = null,
	@param:JsonProperty("level")
	@get:JsonProperty("level")
	@field:Schema(description = "等级")
	val level: Int? = null,
	@param:JsonProperty("experience")
	@get:JsonProperty("experience")
	@field:Schema(description = "经验")
	val experience: Int? = null
)
