package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 种类精灵变种响应。
 */
@Schema(name = "GameSpeciesCreatureVarietiesResponse", description = "种类精灵变种响应。")
@Immutable
interface GameSpeciesCreatureVarietiesResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("species_id")
	@get:Schema(description = "种类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val speciesId: Long?
	@get:JsonProperty("creature_id")
	@get:Schema(description = "精灵 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val creatureId: Long?
	@get:JsonProperty("default_variety")
	@get:Schema(description = "默认变种")
	val defaultVariety: Boolean?
}
