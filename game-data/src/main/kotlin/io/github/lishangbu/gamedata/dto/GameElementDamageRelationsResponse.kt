package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 属性克制关系响应。
 */
@Schema(name = "GameElementDamageRelationsResponse", description = "属性克制关系响应。")
@Immutable
interface GameElementDamageRelationsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("source_element_id")
	@get:Schema(description = "来源属性 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val sourceElementId: Long?
	@get:JsonProperty("target_element_id")
	@get:Schema(description = "目标属性 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val targetElementId: Long?
	@get:JsonProperty("relation_type")
	@get:Schema(description = "关系类型")
	val relationType: String?
}
