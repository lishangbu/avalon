package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 性别进化要求写入请求。
 */
@Schema(name = "GameGenderEvolutionRequirementsRequest", description = "性别进化要求写入请求。")
data class GameGenderEvolutionRequirementsRequest(
	@param:JsonProperty("gender_id")
	@get:JsonProperty("gender_id")
	@field:Schema(description = "性别 ID")
	val genderId: Long? = null,
	@param:JsonProperty("species_id")
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long? = null
)
