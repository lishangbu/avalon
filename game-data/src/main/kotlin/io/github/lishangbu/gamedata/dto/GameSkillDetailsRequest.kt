package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能详情写入请求。
 */
@Schema(name = "GameSkillDetailsRequest", description = "技能详情写入请求。")
data class GameSkillDetailsRequest(
	@param:JsonProperty("skill_id")
	@get:JsonProperty("skill_id")
	@field:Schema(description = "技能 ID")
	val skillId: Long? = null,
	@param:JsonProperty("ailment_id")
	@get:JsonProperty("ailment_id")
	@field:Schema(description = "异常 ID")
	val ailmentId: Long? = null,
	@param:JsonProperty("category_id")
	@get:JsonProperty("category_id")
	@field:Schema(description = "分类 ID")
	val categoryId: Long? = null,
	@param:JsonProperty("target_id")
	@get:JsonProperty("target_id")
	@field:Schema(description = "目标 ID")
	val targetId: Long? = null,
	@param:JsonProperty("min_hits")
	@get:JsonProperty("min_hits")
	@field:Schema(description = "最少命中")
	val minHits: Int? = null,
	@param:JsonProperty("max_hits")
	@get:JsonProperty("max_hits")
	@field:Schema(description = "最多命中")
	val maxHits: Int? = null,
	@param:JsonProperty("min_turns")
	@get:JsonProperty("min_turns")
	@field:Schema(description = "最少回合")
	val minTurns: Int? = null,
	@param:JsonProperty("max_turns")
	@get:JsonProperty("max_turns")
	@field:Schema(description = "最多回合")
	val maxTurns: Int? = null,
	@param:JsonProperty("drain")
	@get:JsonProperty("drain")
	@field:Schema(description = "吸取值")
	val drain: Int? = null,
	@param:JsonProperty("healing")
	@get:JsonProperty("healing")
	@field:Schema(description = "回复值")
	val healing: Int? = null,
	@param:JsonProperty("crit_rate")
	@get:JsonProperty("crit_rate")
	@field:Schema(description = "暴击修正")
	val critRate: Int? = null,
	@param:JsonProperty("ailment_chance")
	@get:JsonProperty("ailment_chance")
	@field:Schema(description = "异常概率")
	val ailmentChance: Int? = null,
	@param:JsonProperty("flinch_chance")
	@get:JsonProperty("flinch_chance")
	@field:Schema(description = "畏缩概率")
	val flinchChance: Int? = null,
	@param:JsonProperty("stat_chance")
	@get:JsonProperty("stat_chance")
	@field:Schema(description = "数值变化概率")
	val statChance: Int? = null,
	@param:JsonProperty("effect")
	@get:JsonProperty("effect")
	@field:Schema(description = "效果")
	val effect: String? = null,
	@param:JsonProperty("short_effect")
	@get:JsonProperty("short_effect")
	@field:Schema(description = "短效果")
	val shortEffect: String? = null,
	@param:JsonProperty("flavor_text")
	@get:JsonProperty("flavor_text")
	@field:Schema(description = "风味说明")
	val flavorText: String? = null
)
