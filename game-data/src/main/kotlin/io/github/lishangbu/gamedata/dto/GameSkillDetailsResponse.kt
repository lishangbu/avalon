package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能详情响应。
 */
@Schema(name = "GameSkillDetailsResponse", description = "技能详情响应。")
data class GameSkillDetailsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("skill_id")
	@field:Schema(description = "技能 ID")
	val skillId: Long?,
	@get:JsonProperty("ailment_id")
	@field:Schema(description = "异常 ID")
	val ailmentId: Long?,
	@get:JsonProperty("category_id")
	@field:Schema(description = "分类 ID")
	val categoryId: Long?,
	@get:JsonProperty("target_id")
	@field:Schema(description = "目标 ID")
	val targetId: Long?,
	@get:JsonProperty("contest_type_id")
	@field:Schema(description = "评分类别 ID")
	val contestTypeId: Long?,
	@get:JsonProperty("contest_effect_id")
	@field:Schema(description = "评价效果 ID")
	val contestEffectId: Long?,
	@get:JsonProperty("advanced_contest_effect_id")
	@field:Schema(description = "高级评价效果 ID")
	val advancedContestEffectId: Long?,
	@get:JsonProperty("min_hits")
	@field:Schema(description = "最少命中")
	val minHits: Int?,
	@get:JsonProperty("max_hits")
	@field:Schema(description = "最多命中")
	val maxHits: Int?,
	@get:JsonProperty("min_turns")
	@field:Schema(description = "最少回合")
	val minTurns: Int?,
	@get:JsonProperty("max_turns")
	@field:Schema(description = "最多回合")
	val maxTurns: Int?,
	@get:JsonProperty("drain")
	@field:Schema(description = "吸取值")
	val drain: Int?,
	@get:JsonProperty("healing")
	@field:Schema(description = "回复值")
	val healing: Int?,
	@get:JsonProperty("crit_rate")
	@field:Schema(description = "暴击修正")
	val critRate: Int?,
	@get:JsonProperty("ailment_chance")
	@field:Schema(description = "异常概率")
	val ailmentChance: Int?,
	@get:JsonProperty("flinch_chance")
	@field:Schema(description = "畏缩概率")
	val flinchChance: Int?,
	@get:JsonProperty("stat_chance")
	@field:Schema(description = "数值变化概率")
	val statChance: Int?,
	@get:JsonProperty("effect")
	@field:Schema(description = "效果")
	val effect: String?,
	@get:JsonProperty("short_effect")
	@field:Schema(description = "短效果")
	val shortEffect: String?,
	@get:JsonProperty("flavor_text")
	@field:Schema(description = "风味说明")
	val flavorText: String?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameSkillDetailsResponse =
			GameSkillDetailsResponse(
				id = record.id,
				skillId = record.longField("skill_id"),
				ailmentId = record.longField("ailment_id"),
				categoryId = record.longField("category_id"),
				targetId = record.longField("target_id"),
				contestTypeId = record.longField("contest_type_id"),
				contestEffectId = record.longField("contest_effect_id"),
				advancedContestEffectId = record.longField("advanced_contest_effect_id"),
				minHits = record.intField("min_hits"),
				maxHits = record.intField("max_hits"),
				minTurns = record.intField("min_turns"),
				maxTurns = record.intField("max_turns"),
				drain = record.intField("drain"),
				healing = record.intField("healing"),
				critRate = record.intField("crit_rate"),
				ailmentChance = record.intField("ailment_chance"),
				flinchChance = record.intField("flinch_chance"),
				statChance = record.intField("stat_chance"),
				effect = record.stringField("effect"),
				shortEffect = record.stringField("short_effect"),
				flavorText = record.stringField("flavor_text")
			)
	}
}
