package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能评价组合响应。
 */
@Schema(name = "GameSkillContestCombosResponse", description = "技能评价组合响应。")
data class GameSkillContestCombosResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("skill_id")
	@field:Schema(description = "技能 ID")
	val skillId: Long?,
	@get:JsonProperty("combo_type")
	@field:Schema(description = "组合类型")
	val comboType: String?,
	@get:JsonProperty("relation_type")
	@field:Schema(description = "关系类型")
	val relationType: String?,
	@get:JsonProperty("related_skill_id")
	@field:Schema(description = "关联技能 ID")
	val relatedSkillId: Long?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameSkillContestCombosResponse =
			GameSkillContestCombosResponse(
				id = record.id,
				skillId = record.longField("skill_id"),
				comboType = record.stringField("combo_type"),
				relationType = record.stringField("relation_type"),
				relatedSkillId = record.longField("related_skill_id")
			)
	}
}
