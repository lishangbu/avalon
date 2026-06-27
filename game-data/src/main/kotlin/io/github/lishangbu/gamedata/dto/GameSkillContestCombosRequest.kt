package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能评价组合写入请求。
 */
@Schema(name = "GameSkillContestCombosRequest", description = "技能评价组合写入请求。")
data class GameSkillContestCombosRequest(
	@param:JsonProperty("skill_id")
	@get:JsonProperty("skill_id")
	@field:Schema(description = "技能 ID")
	val skillId: Long? = null,
	@param:JsonProperty("combo_type")
	@get:JsonProperty("combo_type")
	@field:Schema(description = "组合类型")
	val comboType: String? = null,
	@param:JsonProperty("relation_type")
	@get:JsonProperty("relation_type")
	@field:Schema(description = "关系类型")
	val relationType: String? = null,
	@param:JsonProperty("related_skill_id")
	@get:JsonProperty("related_skill_id")
	@field:Schema(description = "关联技能 ID")
	val relatedSkillId: Long? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"skill_id" to skillId,
		"combo_type" to comboType,
		"relation_type" to relationType,
		"related_skill_id" to relatedSkillId,
		)
}
