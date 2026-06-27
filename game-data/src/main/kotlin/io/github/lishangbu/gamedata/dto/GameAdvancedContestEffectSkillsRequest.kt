package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 高级评价效果技能写入请求。
 */
@Schema(name = "GameAdvancedContestEffectSkillsRequest", description = "高级评价效果技能写入请求。")
data class GameAdvancedContestEffectSkillsRequest(
	@param:JsonProperty("advanced_contest_effect_id")
	@get:JsonProperty("advanced_contest_effect_id")
	@field:Schema(description = "高级评价效果 ID")
	val advancedContestEffectId: Long? = null,
	@param:JsonProperty("skill_id")
	@get:JsonProperty("skill_id")
	@field:Schema(description = "技能 ID")
	val skillId: Long? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"advanced_contest_effect_id" to advancedContestEffectId,
		"skill_id" to skillId,
		)
}
