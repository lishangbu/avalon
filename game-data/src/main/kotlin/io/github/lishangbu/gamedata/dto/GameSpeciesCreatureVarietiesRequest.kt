package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 种类精灵变种写入请求。
 */
@Schema(name = "GameSpeciesCreatureVarietiesRequest", description = "种类精灵变种写入请求。")
data class GameSpeciesCreatureVarietiesRequest(
	@param:JsonProperty("species_id")
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long? = null,
	@param:JsonProperty("creature_id")
	@get:JsonProperty("creature_id")
	@field:Schema(description = "精灵 ID")
	val creatureId: Long? = null,
	@param:JsonProperty("default_variety")
	@get:JsonProperty("default_variety")
	@field:Schema(description = "默认变种")
	val defaultVariety: Boolean? = null
)
