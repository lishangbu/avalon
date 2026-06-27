package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 种类资料写入请求。
 */
@Schema(name = "GameSpeciesRequest", description = "种类资料写入请求。")
data class GameSpeciesRequest(
	@param:JsonProperty("code")
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String? = null,
	@param:JsonProperty("name")
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String? = null,
	@param:JsonProperty("color_id")
	@get:JsonProperty("color_id")
	@field:Schema(description = "颜色 ID")
	val colorId: Long? = null,
	@param:JsonProperty("shape_id")
	@get:JsonProperty("shape_id")
	@field:Schema(description = "形态 ID")
	val shapeId: Long? = null,
	@param:JsonProperty("habitat_id")
	@get:JsonProperty("habitat_id")
	@field:Schema(description = "栖息地 ID")
	val habitatId: Long? = null,
	@param:JsonProperty("gender_rate")
	@get:JsonProperty("gender_rate")
	@field:Schema(description = "性别比例")
	val genderRate: Int? = null,
	@param:JsonProperty("capture_rate")
	@get:JsonProperty("capture_rate")
	@field:Schema(description = "捕获率")
	val captureRate: Int? = null,
	@param:JsonProperty("base_happiness")
	@get:JsonProperty("base_happiness")
	@field:Schema(description = "初始亲和度")
	val baseHappiness: Int? = null,
	@param:JsonProperty("hatch_counter")
	@get:JsonProperty("hatch_counter")
	@field:Schema(description = "孵化计数")
	val hatchCounter: Int? = null,
	@param:JsonProperty("baby")
	@get:JsonProperty("baby")
	@field:Schema(description = "幼体")
	val baby: Boolean? = null,
	@param:JsonProperty("legendary")
	@get:JsonProperty("legendary")
	@field:Schema(description = "传说级")
	val legendary: Boolean? = null,
	@param:JsonProperty("mythical")
	@get:JsonProperty("mythical")
	@field:Schema(description = "幻级")
	val mythical: Boolean? = null,
	@param:JsonProperty("enabled")
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"code" to code,
		"name" to name,
		"color_id" to colorId,
		"shape_id" to shapeId,
		"habitat_id" to habitatId,
		"gender_rate" to genderRate,
		"capture_rate" to captureRate,
		"base_happiness" to baseHappiness,
		"hatch_counter" to hatchCounter,
		"baby" to baby,
		"legendary" to legendary,
		"mythical" to mythical,
		"enabled" to enabled,
		)
}
