package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 种类详情写入请求。
 */
@Schema(name = "GameSpeciesDetailsRequest", description = "种类详情写入请求。")
data class GameSpeciesDetailsRequest(
	@param:JsonProperty("species_id")
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long? = null,
	@param:JsonProperty("growth_rate_id")
	@get:JsonProperty("growth_rate_id")
	@field:Schema(description = "成长速率 ID")
	val growthRateId: Long? = null,
	@param:JsonProperty("sort_order")
	@get:JsonProperty("sort_order")
	@field:Schema(description = "排序")
	val sortOrder: Int? = null,
	@param:JsonProperty("gender_differences")
	@get:JsonProperty("gender_differences")
	@field:Schema(description = "性别差异")
	val genderDifferences: Boolean? = null,
	@param:JsonProperty("forms_switchable")
	@get:JsonProperty("forms_switchable")
	@field:Schema(description = "形态可切换")
	val formsSwitchable: Boolean? = null,
	@param:JsonProperty("genus")
	@get:JsonProperty("genus")
	@field:Schema(description = "分类")
	val genus: String? = null,
	@param:JsonProperty("flavor_text")
	@get:JsonProperty("flavor_text")
	@field:Schema(description = "风味说明")
	val flavorText: String? = null
)
