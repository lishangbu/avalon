package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 精灵技能学习写入请求。
 */
@Schema(name = "GameCreatureSkillLearnsRequest", description = "精灵技能学习写入请求。")
data class GameCreatureSkillLearnsRequest(
	@param:JsonProperty("creature_id")
	@get:JsonProperty("creature_id")
	@field:Schema(description = "精灵 ID")
	val creatureId: Long? = null,
	@param:JsonProperty("skill_id")
	@get:JsonProperty("skill_id")
	@field:Schema(description = "技能 ID")
	val skillId: Long? = null,
	@param:JsonProperty("learn_method_id")
	@get:JsonProperty("learn_method_id")
	@field:Schema(description = "学习方式 ID")
	val learnMethodId: Long? = null,
	@param:JsonProperty("level_learned_at")
	@get:JsonProperty("level_learned_at")
	@field:Schema(description = "习得等级")
	val levelLearnedAt: Int? = null
)
