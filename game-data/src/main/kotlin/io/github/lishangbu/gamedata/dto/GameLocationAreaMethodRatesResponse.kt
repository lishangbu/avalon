package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 区域遭遇方式概率响应。
 */
@Schema(name = "GameLocationAreaMethodRatesResponse", description = "区域遭遇方式概率响应。")
@Immutable
interface GameLocationAreaMethodRatesResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("area_id")
	@get:Schema(description = "区域 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val areaId: Long?
	@get:JsonProperty("method_id")
	@get:Schema(description = "遭遇方式 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val methodId: Long?
	@get:JsonProperty("rate")
	@get:Schema(description = "概率")
	val rate: Int?
}
