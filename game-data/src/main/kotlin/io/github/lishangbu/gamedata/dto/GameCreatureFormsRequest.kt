package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 生物形态写入请求。
 */
@Schema(name = "GameCreatureFormsRequest", description = "生物形态写入请求。")
data class GameCreatureFormsRequest(
	@param:JsonProperty("code")
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String? = null,
	@param:JsonProperty("name")
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String? = null,
	@param:JsonProperty("creature_id")
	@get:JsonProperty("creature_id")
	@field:Schema(description = "生物 ID")
	val creatureId: Long? = null,
	@param:JsonProperty("form_name")
	@get:JsonProperty("form_name")
	@field:Schema(description = "形态名")
	val formName: String? = null,
	@param:JsonProperty("sort_order")
	@get:JsonProperty("sort_order")
	@field:Schema(description = "排序")
	val sortOrder: Int? = null,
	@param:JsonProperty("form_order")
	@get:JsonProperty("form_order")
	@field:Schema(description = "形态排序")
	val formOrder: Int? = null,
	@param:JsonProperty("battle_only")
	@get:JsonProperty("battle_only")
	@field:Schema(description = "仅战斗")
	val battleOnly: Boolean? = null,
	@param:JsonProperty("default_form")
	@get:JsonProperty("default_form")
	@field:Schema(description = "默认形态")
	val defaultForm: Boolean? = null,
	@param:JsonProperty("enhanced_form")
	@get:JsonProperty("enhanced_form")
	@field:Schema(description = "强化形态")
	val enhancedForm: Boolean? = null,
	@param:JsonProperty("enabled")
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"code" to code,
		"name" to name,
		"creature_id" to creatureId,
		"form_name" to formName,
		"sort_order" to sortOrder,
		"form_order" to formOrder,
		"battle_only" to battleOnly,
		"default_form" to defaultForm,
		"enhanced_form" to enhancedForm,
		"enabled" to enabled,
		)
}
