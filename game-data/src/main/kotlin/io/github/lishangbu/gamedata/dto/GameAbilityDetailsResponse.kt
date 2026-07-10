package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 特性详情响应。
 */
@Schema(name = "GameAbilityDetailsResponse", description = "特性详情响应。")
@Immutable
interface GameAbilityDetailsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("ability_id")
	@get:Schema(description = "特性 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val abilityId: Long?
	@get:JsonProperty("effect")
	@get:Schema(description = "效果")
	val effect: String?
	@get:JsonProperty("short_effect")
	@get:Schema(description = "短效果")
	val shortEffect: String?
	@get:JsonProperty("flavor_text")
	@get:Schema(description = "风味说明")
	val flavorText: String?
}
