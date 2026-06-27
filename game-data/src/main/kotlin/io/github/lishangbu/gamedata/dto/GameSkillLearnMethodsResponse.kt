package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能学习方式响应。
 */
@Schema(name = "GameSkillLearnMethodsResponse", description = "技能学习方式响应。")
data class GameSkillLearnMethodsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("description")
	@field:Schema(description = "说明")
	val description: String?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameSkillLearnMethodsResponse =
			GameSkillLearnMethodsResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				description = record.stringField("description"),
				enabled = record.booleanField("enabled")
			)
	}
}
