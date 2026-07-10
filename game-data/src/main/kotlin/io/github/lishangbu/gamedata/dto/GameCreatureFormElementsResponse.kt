package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 精灵形态属性响应。
 */
@Schema(name = "GameCreatureFormElementsResponse", description = "精灵形态属性响应。")
@Immutable
interface GameCreatureFormElementsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("form_id")
	@get:Schema(description = "形态 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val formId: Long?
	@get:JsonProperty("element_id")
	@get:Schema(description = "属性 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val elementId: Long?
	@get:JsonProperty("slot_order")
	@get:Schema(description = "槽位顺序")
	val slotOrder: Int?
}
