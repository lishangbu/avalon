package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 图鉴目录条目响应。
 */
@Schema(name = "GameCatalogEntriesResponse", description = "图鉴目录条目响应。")
@Immutable
interface GameCatalogEntriesResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("catalog_id")
	@get:Schema(description = "目录 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val catalogId: Long?
	@get:JsonProperty("species_id")
	@get:Schema(description = "种类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val speciesId: Long?
	@get:JsonProperty("entry_number")
	@get:Schema(description = "目录编号")
	val entryNumber: Int?
}
