package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 树果口味强度响应。
 */
@Schema(name = "GameBerryFlavorPotenciesResponse", description = "树果口味强度响应。")
@Immutable
interface GameBerryFlavorPotenciesResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("berry_id")
	@get:Schema(description = "树果 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val berryId: Long?
	@get:JsonProperty("flavor_id")
	@get:Schema(description = "口味 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val flavorId: Long?
	@get:JsonProperty("potency")
	@get:Schema(description = "强度")
	val potency: Int?
}
