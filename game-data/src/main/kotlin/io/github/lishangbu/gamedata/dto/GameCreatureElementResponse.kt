package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 精灵属性绑定响应。
 */
@Schema(name = "GameCreatureElementResponse", description = "精灵属性绑定响应。")
@Immutable
interface GameCreatureElementResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("creature_id")
	@get:Schema(description = "精灵 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val creatureId: Long?
	@get:JsonProperty("element_id")
	@get:Schema(description = "属性 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val elementId: Long?
	@get:JsonProperty("slot_order")
	@get:Schema(description = "槽位")
	val slotOrder: Int?
}
