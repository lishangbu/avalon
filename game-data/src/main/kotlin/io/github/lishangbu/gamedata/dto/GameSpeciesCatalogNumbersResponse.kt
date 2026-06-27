package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 种类目录编号响应。
 */
@Schema(name = "GameSpeciesCatalogNumbersResponse", description = "种类目录编号响应。")
data class GameSpeciesCatalogNumbersResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long?,
	@get:JsonProperty("catalog_id")
	@field:Schema(description = "目录 ID")
	val catalogId: Long?,
	@get:JsonProperty("entry_number")
	@field:Schema(description = "目录编号")
	val entryNumber: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameSpeciesCatalogNumbersResponse =
			GameSpeciesCatalogNumbersResponse(
				id = record.id,
				speciesId = record.longField("species_id"),
				catalogId = record.longField("catalog_id"),
				entryNumber = record.intField("entry_number")
			)
	}
}
