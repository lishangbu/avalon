package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 种类详情响应。
 */
@Schema(name = "GameSpeciesDetailsResponse", description = "种类详情响应。")
data class GameSpeciesDetailsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long?,
	@get:JsonProperty("growth_rate_id")
	@field:Schema(description = "成长速率 ID")
	val growthRateId: Long?,
	@get:JsonProperty("evolves_from_species_id")
	@field:Schema(description = "进化来源种类 ID")
	val evolvesFromSpeciesId: Long?,
	@get:JsonProperty("evolution_chain_id")
	@field:Schema(description = "进化链 ID")
	val evolutionChainId: Long?,
	@get:JsonProperty("sort_order")
	@field:Schema(description = "排序")
	val sortOrder: Int?,
	@get:JsonProperty("gender_differences")
	@field:Schema(description = "性别差异")
	val genderDifferences: Boolean?,
	@get:JsonProperty("forms_switchable")
	@field:Schema(description = "形态可切换")
	val formsSwitchable: Boolean?,
	@get:JsonProperty("genus")
	@field:Schema(description = "分类")
	val genus: String?,
	@get:JsonProperty("flavor_text")
	@field:Schema(description = "风味说明")
	val flavorText: String?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameSpeciesDetailsResponse =
			GameSpeciesDetailsResponse(
				id = record.id,
				speciesId = record.longField("species_id"),
				growthRateId = record.longField("growth_rate_id"),
				evolvesFromSpeciesId = record.longField("evolves_from_species_id"),
				evolutionChainId = record.longField("evolution_chain_id"),
				sortOrder = record.intField("sort_order"),
				genderDifferences = record.booleanField("gender_differences"),
				formsSwitchable = record.booleanField("forms_switchable"),
				genus = record.stringField("genus"),
				flavorText = record.stringField("flavor_text")
			)
	}
}
