package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能数值变化响应。
 */
@Schema(name = "GameSkillStatChangesResponse", description = "技能数值变化响应。")
data class GameSkillStatChangesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("skill_id")
	@field:Schema(description = "技能 ID")
	val skillId: Long?,
	@get:JsonProperty("stat_id")
	@field:Schema(description = "数值项 ID")
	val statId: Long?,
	@get:JsonProperty("change_value")
	@field:Schema(description = "变化值")
	val changeValue: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameSkillStatChangesResponse =
			GameSkillStatChangesResponse(
				id = record.id,
				skillId = record.longField("skill_id"),
				statId = record.longField("stat_id"),
				changeValue = record.intField("change_value")
			)
	}
}
