package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能数值变化写入请求。
 */
@Schema(name = "GameSkillStatChangesRequest", description = "技能数值变化写入请求。")
data class GameSkillStatChangesRequest(
	@param:JsonProperty("skill_id")
	@get:JsonProperty("skill_id")
	@field:Schema(description = "技能 ID")
	val skillId: Long? = null,
	@param:JsonProperty("stat_id")
	@get:JsonProperty("stat_id")
	@field:Schema(description = "数值项 ID")
	val statId: Long? = null,
	@param:JsonProperty("change_value")
	@get:JsonProperty("change_value")
	@field:Schema(description = "变化值")
	val changeValue: Int? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"skill_id" to skillId,
		"stat_id" to statId,
		"change_value" to changeValue,
		)
}
