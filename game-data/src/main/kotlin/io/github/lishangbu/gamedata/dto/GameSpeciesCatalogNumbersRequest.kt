package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 种类目录编号写入请求。
 */
@Schema(name = "GameSpeciesCatalogNumbersRequest", description = "种类目录编号写入请求。")
data class GameSpeciesCatalogNumbersRequest(
	@param:JsonProperty("species_id")
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long? = null,
	@param:JsonProperty("catalog_id")
	@get:JsonProperty("catalog_id")
	@field:Schema(description = "目录 ID")
	val catalogId: Long? = null,
	@param:JsonProperty("entry_number")
	@get:JsonProperty("entry_number")
	@field:Schema(description = "目录编号")
	val entryNumber: Int? = null
)
