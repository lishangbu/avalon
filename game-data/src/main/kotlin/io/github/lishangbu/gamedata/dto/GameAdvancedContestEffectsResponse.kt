package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 高级评价效果响应。
 */
@Schema(name = "GameAdvancedContestEffectsResponse", description = "高级评价效果响应。")
@Immutable
interface GameAdvancedContestEffectsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("appeal")
	@get:Schema(description = "吸引力")
	val appeal: Int?
	@get:JsonProperty("flavor_text")
	@get:Schema(description = "风味说明")
	val flavorText: String?
}
