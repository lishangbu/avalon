package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 种类资料响应。
 */
@Schema(name = "GameSpeciesResponse", description = "种类资料响应。")
@Immutable
interface GameSpeciesResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("code")
	@get:Schema(description = "编码")
	val code: String?
	@get:JsonProperty("name")
	@get:Schema(description = "名称")
	val name: String?
	@get:JsonProperty("color_id")
	@get:Schema(description = "颜色 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val colorId: Long?
	@get:JsonProperty("shape_id")
	@get:Schema(description = "形态 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val shapeId: Long?
	@get:JsonProperty("habitat_id")
	@get:Schema(description = "栖息地 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val habitatId: Long?
	@get:JsonProperty("gender_rate")
	@get:Schema(description = "性别比例")
	val genderRate: Int?
	@get:JsonProperty("capture_rate")
	@get:Schema(description = "捕获率")
	val captureRate: Int?
	@get:JsonProperty("base_happiness")
	@get:Schema(description = "初始亲和度")
	val baseHappiness: Int?
	@get:JsonProperty("hatch_counter")
	@get:Schema(description = "孵化计数")
	val hatchCounter: Int?
	@get:JsonProperty("baby")
	@get:Schema(description = "幼体")
	val baby: Boolean?
	@get:JsonProperty("legendary")
	@get:Schema(description = "传说级")
	val legendary: Boolean?
	@get:JsonProperty("mythical")
	@get:Schema(description = "幻级")
	val mythical: Boolean?
	@get:JsonProperty("enabled")
	@get:Schema(description = "启用")
	val enabled: Boolean?
}
