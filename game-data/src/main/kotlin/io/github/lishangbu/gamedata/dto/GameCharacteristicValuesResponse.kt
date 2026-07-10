package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 个体特征取值响应。
 */
@Schema(name = "GameCharacteristicValuesResponse", description = "个体特征取值响应。")
@Immutable
interface GameCharacteristicValuesResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("characteristic_id")
	@get:Schema(description = "特征 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val characteristicId: Long?
	@get:JsonProperty("possible_value")
	@get:Schema(description = "可能取值")
	val possibleValue: Int?
}
