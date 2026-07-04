package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 精灵资料写入请求。
 */
@Schema(name = "GameCreatureRequest", description = "精灵资料写入请求。")
data class GameCreatureRequest(
	@param:JsonProperty("code")
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String? = null,
	@param:JsonProperty("name")
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String? = null,
	@param:JsonProperty("species_id")
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long? = null,
	@param:JsonProperty("height")
	@get:JsonProperty("height")
	@field:Schema(description = "高度")
	val height: Int? = null,
	@param:JsonProperty("weight")
	@get:JsonProperty("weight")
	@field:Schema(description = "重量")
	val weight: Int? = null,
	@param:JsonProperty("base_experience")
	@get:JsonProperty("base_experience")
	@field:Schema(description = "基础经验")
	val baseExperience: Int? = null,
	@param:JsonProperty("sort_order")
	@get:JsonProperty("sort_order")
	@field:Schema(description = "排序")
	val sortOrder: Int? = null,
	@param:JsonProperty("default_form")
	@get:JsonProperty("default_form")
	@field:Schema(description = "默认形态")
	val defaultForm: Boolean? = null,
	@param:JsonProperty("enabled")
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"code" to code,
		"name" to name,
		"species_id" to speciesId,
		"height" to height,
		"weight" to weight,
		"base_experience" to baseExperience,
		"sort_order" to sortOrder,
		"default_form" to defaultForm,
		"enabled" to enabled,
		)
}
