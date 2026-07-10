package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 精灵资料响应。
 */
@Schema(name = "GameCreatureResponse", description = "精灵资料响应。")
@Immutable
interface GameCreatureResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("code")
	@get:Schema(description = "编码")
	val code: String?
	@get:JsonProperty("name")
	@get:Schema(description = "名称")
	val name: String?
	@get:JsonProperty("species_id")
	@get:Schema(description = "种类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val speciesId: Long?
	@get:JsonProperty("height")
	@get:Schema(description = "高度")
	val height: Int?
	@get:JsonProperty("weight")
	@get:Schema(description = "重量")
	val weight: Int?
	@get:JsonProperty("base_experience")
	@get:Schema(description = "基础经验")
	val baseExperience: Int?
	@get:JsonProperty("sort_order")
	@get:Schema(description = "排序")
	val sortOrder: Int?
	@get:JsonProperty("default_form")
	@get:Schema(description = "默认形态")
	val defaultForm: Boolean?
	@get:JsonProperty("enabled")
	@get:Schema(description = "启用")
	val enabled: Boolean?
}
