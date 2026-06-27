package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 高级评价效果技能响应。
 */
@Schema(name = "GameAdvancedContestEffectSkillsResponse", description = "高级评价效果技能响应。")
data class GameAdvancedContestEffectSkillsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("advanced_contest_effect_id")
	@field:Schema(description = "高级评价效果 ID")
	val advancedContestEffectId: Long?,
	@get:JsonProperty("skill_id")
	@field:Schema(description = "技能 ID")
	val skillId: Long?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameAdvancedContestEffectSkillsResponse =
			GameAdvancedContestEffectSkillsResponse(
				id = record.id,
				advancedContestEffectId = record.longField("advanced_contest_effect_id"),
				skillId = record.longField("skill_id")
			)
	}
}
