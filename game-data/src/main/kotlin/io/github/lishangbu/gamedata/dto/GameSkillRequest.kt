package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能资料写入请求。
 */
@Schema(name = "GameSkillRequest", description = "技能资料写入请求。")
data class GameSkillRequest(
	@param:JsonProperty("code")
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String? = null,
	@param:JsonProperty("name")
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String? = null,
	@param:JsonProperty("element_id")
	@get:JsonProperty("element_id")
	@field:Schema(description = "属性 ID")
	val elementId: Long? = null,
	@param:JsonProperty("damage_class_id")
	@get:JsonProperty("damage_class_id")
	@field:Schema(description = "分类 ID")
	val damageClassId: Long? = null,
	@param:JsonProperty("accuracy")
	@get:JsonProperty("accuracy")
	@field:Schema(description = "命中")
	val accuracy: Int? = null,
	@param:JsonProperty("power")
	@get:JsonProperty("power")
	@field:Schema(description = "威力")
	val power: Int? = null,
	@param:JsonProperty("pp")
	@get:JsonProperty("pp")
	@field:Schema(description = "PP")
	val pp: Int? = null,
	@param:JsonProperty("priority")
	@get:JsonProperty("priority")
	@field:Schema(description = "优先级")
	val priority: Int? = null,
	@param:JsonProperty("effect_chance")
	@get:JsonProperty("effect_chance")
	@field:Schema(description = "效果概率")
	val effectChance: Int? = null,
	@param:JsonProperty("enabled")
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"code" to code,
		"name" to name,
		"element_id" to elementId,
		"damage_class_id" to damageClassId,
		"accuracy" to accuracy,
		"power" to power,
		"pp" to pp,
		"priority" to priority,
		"effect_chance" to effectChance,
		"enabled" to enabled,
		)
}
