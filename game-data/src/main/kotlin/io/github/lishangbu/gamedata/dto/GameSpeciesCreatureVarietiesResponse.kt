package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 种类精灵变种响应。
 */
@Schema(name = "GameSpeciesCreatureVarietiesResponse", description = "种类精灵变种响应。")
data class GameSpeciesCreatureVarietiesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long?,
	@get:JsonProperty("creature_id")
	@field:Schema(description = "精灵 ID")
	val creatureId: Long?,
	@get:JsonProperty("default_variety")
	@field:Schema(description = "默认变种")
	val defaultVariety: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameSpeciesCreatureVarietiesResponse =
			GameSpeciesCreatureVarietiesResponse(
				id = record.id,
				speciesId = record.longField("species_id"),
				creatureId = record.longField("creature_id"),
				defaultVariety = record.booleanField("default_variety")
			)
	}
}
