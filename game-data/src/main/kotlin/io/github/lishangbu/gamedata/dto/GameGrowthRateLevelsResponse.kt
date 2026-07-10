package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 成长等级经验响应。
 */
@Schema(name = "GameGrowthRateLevelsResponse", description = "成长等级经验响应。")
@Immutable
interface GameGrowthRateLevelsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("growth_rate_id")
	@get:Schema(description = "成长速率 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val growthRateId: Long?
	@get:JsonProperty("level")
	@get:Schema(description = "等级")
	val level: Int?
	@get:JsonProperty("experience")
	@get:Schema(description = "经验")
	val experience: Int?
}
