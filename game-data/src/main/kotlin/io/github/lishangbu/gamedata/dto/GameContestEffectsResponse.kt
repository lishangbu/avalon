package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 评价效果响应。
 */
@Schema(name = "GameContestEffectsResponse", description = "评价效果响应。")
@Immutable
interface GameContestEffectsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("appeal")
	@get:Schema(description = "吸引力")
	val appeal: Int?
	@get:JsonProperty("jam")
	@get:Schema(description = "干扰值")
	val jam: Int?
	@get:JsonProperty("effect")
	@get:Schema(description = "效果")
	val effect: String?
	@get:JsonProperty("flavor_text")
	@get:Schema(description = "风味说明")
	val flavorText: String?
}
