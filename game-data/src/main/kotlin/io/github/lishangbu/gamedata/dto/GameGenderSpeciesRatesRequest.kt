package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 性别种类比例写入请求。
 */
@Schema(name = "GameGenderSpeciesRatesRequest", description = "性别种类比例写入请求。")
data class GameGenderSpeciesRatesRequest(
	@param:JsonProperty("gender_id")
	@get:JsonProperty("gender_id")
	@field:Schema(description = "性别 ID")
	val genderId: Long? = null,
	@param:JsonProperty("species_id")
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long? = null,
	@param:JsonProperty("rate")
	@get:JsonProperty("rate")
	@field:Schema(description = "概率")
	val rate: Int? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"gender_id" to genderId,
		"species_id" to speciesId,
		"rate" to rate,
		)
}
