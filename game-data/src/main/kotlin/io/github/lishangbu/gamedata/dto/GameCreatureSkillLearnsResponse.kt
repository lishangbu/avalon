package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 精灵技能学习响应。
 */
@Schema(name = "GameCreatureSkillLearnsResponse", description = "精灵技能学习响应。")
data class GameCreatureSkillLearnsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("creature_id")
	@field:Schema(description = "精灵 ID")
	val creatureId: Long?,
	@get:JsonProperty("skill_id")
	@field:Schema(description = "技能 ID")
	val skillId: Long?,
	@get:JsonProperty("learn_method_id")
	@field:Schema(description = "学习方式 ID")
	val learnMethodId: Long?,
	@get:JsonProperty("level_learned_at")
	@field:Schema(description = "习得等级")
	val levelLearnedAt: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameCreatureSkillLearnsResponse =
			GameCreatureSkillLearnsResponse(
				id = record.id,
				creatureId = record.longField("creature_id"),
				skillId = record.longField("skill_id"),
				learnMethodId = record.longField("learn_method_id"),
				levelLearnedAt = record.intField("level_learned_at")
			)
	}
}
