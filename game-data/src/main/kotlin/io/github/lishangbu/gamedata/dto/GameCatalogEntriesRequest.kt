package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 图鉴目录条目写入请求。
 */
@Schema(name = "GameCatalogEntriesRequest", description = "图鉴目录条目写入请求。")
data class GameCatalogEntriesRequest(
	@param:JsonProperty("catalog_id")
	@get:JsonProperty("catalog_id")
	@field:Schema(description = "目录 ID")
	val catalogId: Long? = null,
	@param:JsonProperty("species_id")
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long? = null,
	@param:JsonProperty("entry_number")
	@get:JsonProperty("entry_number")
	@field:Schema(description = "目录编号")
	val entryNumber: Int? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"catalog_id" to catalogId,
		"species_id" to speciesId,
		"entry_number" to entryNumber,
		)
}
