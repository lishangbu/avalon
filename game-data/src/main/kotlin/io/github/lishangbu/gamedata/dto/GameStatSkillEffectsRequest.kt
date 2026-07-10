package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 数值项技能影响写入请求。
 */
@Schema(name = "GameStatSkillEffectsRequest", description = "数值项技能影响写入请求。")
data class GameStatSkillEffectsRequest(
	@param:JsonProperty("stat_id")
	@get:JsonProperty("stat_id")
	@field:Schema(description = "数值项 ID")
	val statId: Long? = null,
	@param:JsonProperty("skill_id")
	@get:JsonProperty("skill_id")
	@field:Schema(description = "技能 ID")
	val skillId: Long? = null,
	@param:JsonProperty("change_value")
	@get:JsonProperty("change_value")
	@field:Schema(description = "变化值")
	val changeValue: Int? = null,
	@param:JsonProperty("effect_type")
	@get:JsonProperty("effect_type")
	@field:Schema(description = "影响类型")
	val effectType: String? = null
)
