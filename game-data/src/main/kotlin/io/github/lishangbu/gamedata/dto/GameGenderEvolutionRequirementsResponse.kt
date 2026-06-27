package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 性别进化要求响应。
 */
@Schema(name = "GameGenderEvolutionRequirementsResponse", description = "性别进化要求响应。")
data class GameGenderEvolutionRequirementsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("gender_id")
	@field:Schema(description = "性别 ID")
	val genderId: Long?,
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameGenderEvolutionRequirementsResponse =
			GameGenderEvolutionRequirementsResponse(
				id = record.id,
				genderId = record.longField("gender_id"),
				speciesId = record.longField("species_id")
			)
	}
}
