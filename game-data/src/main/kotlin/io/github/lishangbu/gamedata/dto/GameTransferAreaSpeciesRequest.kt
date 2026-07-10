package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 迁移区域种类写入请求。
 */
@Schema(name = "GameTransferAreaSpeciesRequest", description = "迁移区域种类写入请求。")
data class GameTransferAreaSpeciesRequest(
	@param:JsonProperty("area_id")
	@get:JsonProperty("area_id")
	@field:Schema(description = "区域 ID")
	val areaId: Long? = null,
	@param:JsonProperty("species_id")
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long? = null,
	@param:JsonProperty("base_score")
	@get:JsonProperty("base_score")
	@field:Schema(description = "基础分")
	val baseScore: Int? = null,
	@param:JsonProperty("rate")
	@get:JsonProperty("rate")
	@field:Schema(description = "概率")
	val rate: Int? = null
)
