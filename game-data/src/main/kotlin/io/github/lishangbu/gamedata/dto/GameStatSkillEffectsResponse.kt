package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 数值项技能影响响应。
 */
@Schema(name = "GameStatSkillEffectsResponse", description = "数值项技能影响响应。")
data class GameStatSkillEffectsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("stat_id")
	@field:Schema(description = "数值项 ID")
	val statId: Long?,
	@get:JsonProperty("skill_id")
	@field:Schema(description = "技能 ID")
	val skillId: Long?,
	@get:JsonProperty("change_value")
	@field:Schema(description = "变化值")
	val changeValue: Int?,
	@get:JsonProperty("effect_type")
	@field:Schema(description = "影响类型")
	val effectType: String?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameStatSkillEffectsResponse =
			GameStatSkillEffectsResponse(
				id = record.id,
				statId = record.longField("stat_id"),
				skillId = record.longField("skill_id"),
				changeValue = record.intField("change_value"),
				effectType = record.stringField("effect_type")
			)
	}
}
