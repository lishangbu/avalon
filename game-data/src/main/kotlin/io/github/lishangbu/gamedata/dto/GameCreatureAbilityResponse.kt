package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 精灵特性绑定响应。
 */
@Schema(name = "GameCreatureAbilityResponse", description = "精灵特性绑定响应。")
@Immutable
interface GameCreatureAbilityResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("creature_id")
	@get:Schema(description = "精灵 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val creatureId: Long?
	@get:JsonProperty("ability_id")
	@get:Schema(description = "特性 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val abilityId: Long?
	@get:JsonProperty("slot_order")
	@get:Schema(description = "槽位")
	val slotOrder: Int?
	@get:JsonProperty("hidden")
	@get:Schema(description = "隐藏")
	val hidden: Boolean?
}
