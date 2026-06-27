package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能资料响应。
 */
@Schema(name = "GameSkillResponse", description = "技能资料响应。")
data class GameSkillResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("element_id")
	@field:Schema(description = "属性 ID")
	val elementId: Long?,
	@get:JsonProperty("damage_class_id")
	@field:Schema(description = "分类 ID")
	val damageClassId: Long?,
	@get:JsonProperty("accuracy")
	@field:Schema(description = "命中")
	val accuracy: Int?,
	@get:JsonProperty("power")
	@field:Schema(description = "威力")
	val power: Int?,
	@get:JsonProperty("pp")
	@field:Schema(description = "PP")
	val pp: Int?,
	@get:JsonProperty("priority")
	@field:Schema(description = "优先级")
	val priority: Int?,
	@get:JsonProperty("effect_chance")
	@field:Schema(description = "效果概率")
	val effectChance: Int?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameSkillResponse =
			GameSkillResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				elementId = record.longField("element_id"),
				damageClassId = record.longField("damage_class_id"),
				accuracy = record.intField("accuracy"),
				power = record.intField("power"),
				pp = record.intField("pp"),
				priority = record.intField("priority"),
				effectChance = record.intField("effect_chance"),
				enabled = record.booleanField("enabled")
			)
	}
}
