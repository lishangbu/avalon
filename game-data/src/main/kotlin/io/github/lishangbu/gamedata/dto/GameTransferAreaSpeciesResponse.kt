package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 迁移区域种类响应。
 */
@Schema(name = "GameTransferAreaSpeciesResponse", description = "迁移区域种类响应。")
data class GameTransferAreaSpeciesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("area_id")
	@field:Schema(description = "区域 ID")
	val areaId: Long?,
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long?,
	@get:JsonProperty("base_score")
	@field:Schema(description = "基础分")
	val baseScore: Int?,
	@get:JsonProperty("rate")
	@field:Schema(description = "概率")
	val rate: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameTransferAreaSpeciesResponse =
			GameTransferAreaSpeciesResponse(
				id = record.id,
				areaId = record.longField("area_id"),
				speciesId = record.longField("species_id"),
				baseScore = record.intField("base_score"),
				rate = record.intField("rate")
			)
	}
}
