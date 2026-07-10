package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 种类分组绑定响应。
 */
@Schema(name = "GameSpeciesEggGroupResponse", description = "种类分组绑定响应。")
@Immutable
interface GameSpeciesEggGroupResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("species_id")
	@get:Schema(description = "种类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val speciesId: Long?
	@get:JsonProperty("egg_group_id")
	@get:Schema(description = "分组 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val eggGroupId: Long?
	@get:JsonProperty("slot_order")
	@get:Schema(description = "槽位")
	val slotOrder: Int?
}
