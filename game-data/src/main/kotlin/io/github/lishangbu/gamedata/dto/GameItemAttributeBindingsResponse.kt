package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 道具属性绑定响应。
 */
@Schema(name = "GameItemAttributeBindingsResponse", description = "道具属性绑定响应。")
@Immutable
interface GameItemAttributeBindingsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("item_id")
	@get:Schema(description = "道具 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val itemId: Long?
	@get:JsonProperty("attribute_id")
	@get:Schema(description = "属性 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val attributeId: Long?
}
