package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 性别进化要求响应。
 */
@Schema(name = "GameGenderEvolutionRequirementsResponse", description = "性别进化要求响应。")
@Immutable
interface GameGenderEvolutionRequirementsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("gender_id")
	@get:Schema(description = "性别 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val genderId: Long?
	@get:JsonProperty("species_id")
	@get:Schema(description = "种类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val speciesId: Long?
}
