package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 技能详情响应。
 */
@Schema(name = "GameSkillDetailsResponse", description = "技能详情响应。")
@Immutable
interface GameSkillDetailsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("skill_id")
	@get:Schema(description = "技能 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val skillId: Long?
	@get:JsonProperty("ailment_id")
	@get:Schema(description = "异常 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val ailmentId: Long?
	@get:JsonProperty("category_id")
	@get:Schema(description = "分类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val categoryId: Long?
	@get:JsonProperty("target_id")
	@get:Schema(description = "目标 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val targetId: Long?
	@get:JsonProperty("contest_type_id")
	@get:Schema(description = "评分类别 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val contestTypeId: Long?
	@get:JsonProperty("contest_effect_id")
	@get:Schema(description = "评价效果 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val contestEffectId: Long?
	@get:JsonProperty("advanced_contest_effect_id")
	@get:Schema(description = "高级评价效果 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val advancedContestEffectId: Long?
	@get:JsonProperty("min_hits")
	@get:Schema(description = "最少命中")
	val minHits: Int?
	@get:JsonProperty("max_hits")
	@get:Schema(description = "最多命中")
	val maxHits: Int?
	@get:JsonProperty("min_turns")
	@get:Schema(description = "最少回合")
	val minTurns: Int?
	@get:JsonProperty("max_turns")
	@get:Schema(description = "最多回合")
	val maxTurns: Int?
	@get:JsonProperty("drain")
	@get:Schema(description = "吸取值")
	val drain: Int?
	@get:JsonProperty("healing")
	@get:Schema(description = "回复值")
	val healing: Int?
	@get:JsonProperty("crit_rate")
	@get:Schema(description = "暴击修正")
	val critRate: Int?
	@get:JsonProperty("ailment_chance")
	@get:Schema(description = "异常概率")
	val ailmentChance: Int?
	@get:JsonProperty("flinch_chance")
	@get:Schema(description = "畏缩概率")
	val flinchChance: Int?
	@get:JsonProperty("stat_chance")
	@get:Schema(description = "数值变化概率")
	val statChance: Int?
	@get:JsonProperty("effect")
	@get:Schema(description = "效果")
	val effect: String?
	@get:JsonProperty("short_effect")
	@get:Schema(description = "短效果")
	val shortEffect: String?
	@get:JsonProperty("flavor_text")
	@get:Schema(description = "风味说明")
	val flavorText: String?
}
