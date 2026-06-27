package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 进化条件写入请求。
 */
@Schema(name = "GameEvolutionDetailsRequest", description = "进化条件写入请求。")
data class GameEvolutionDetailsRequest(
	@param:JsonProperty("chain_id")
	@get:JsonProperty("chain_id")
	@field:Schema(description = "进化链 ID")
	val chainId: Long? = null,
	@param:JsonProperty("from_species_id")
	@get:JsonProperty("from_species_id")
	@field:Schema(description = "起始种类 ID")
	val fromSpeciesId: Long? = null,
	@param:JsonProperty("to_species_id")
	@get:JsonProperty("to_species_id")
	@field:Schema(description = "目标种类 ID")
	val toSpeciesId: Long? = null,
	@param:JsonProperty("trigger_id")
	@get:JsonProperty("trigger_id")
	@field:Schema(description = "触发器 ID")
	val triggerId: Long? = null,
	@param:JsonProperty("item_id")
	@get:JsonProperty("item_id")
	@field:Schema(description = "道具 ID")
	val itemId: Long? = null,
	@param:JsonProperty("held_item_id")
	@get:JsonProperty("held_item_id")
	@field:Schema(description = "持有道具 ID")
	val heldItemId: Long? = null,
	@param:JsonProperty("known_skill_id")
	@get:JsonProperty("known_skill_id")
	@field:Schema(description = "已掌握技能 ID")
	val knownSkillId: Long? = null,
	@param:JsonProperty("known_element_id")
	@get:JsonProperty("known_element_id")
	@field:Schema(description = "已掌握属性 ID")
	val knownElementId: Long? = null,
	@param:JsonProperty("location_id")
	@get:JsonProperty("location_id")
	@field:Schema(description = "地点 ID")
	val locationId: Long? = null,
	@param:JsonProperty("party_species_id")
	@get:JsonProperty("party_species_id")
	@field:Schema(description = "队伍种类 ID")
	val partySpeciesId: Long? = null,
	@param:JsonProperty("party_element_id")
	@get:JsonProperty("party_element_id")
	@field:Schema(description = "队伍属性 ID")
	val partyElementId: Long? = null,
	@param:JsonProperty("trade_species_id")
	@get:JsonProperty("trade_species_id")
	@field:Schema(description = "交换种类 ID")
	val tradeSpeciesId: Long? = null,
	@param:JsonProperty("gender_id")
	@get:JsonProperty("gender_id")
	@field:Schema(description = "性别 ID")
	val genderId: Long? = null,
	@param:JsonProperty("region_id")
	@get:JsonProperty("region_id")
	@field:Schema(description = "地区 ID")
	val regionId: Long? = null,
	@param:JsonProperty("min_level")
	@get:JsonProperty("min_level")
	@field:Schema(description = "最低等级")
	val minLevel: Int? = null,
	@param:JsonProperty("min_happiness")
	@get:JsonProperty("min_happiness")
	@field:Schema(description = "最低亲和度")
	val minHappiness: Int? = null,
	@param:JsonProperty("min_beauty")
	@get:JsonProperty("min_beauty")
	@field:Schema(description = "最低美丽度")
	val minBeauty: Int? = null,
	@param:JsonProperty("min_affection")
	@get:JsonProperty("min_affection")
	@field:Schema(description = "最低友好度")
	val minAffection: Int? = null,
	@param:JsonProperty("relative_physical_stats")
	@get:JsonProperty("relative_physical_stats")
	@field:Schema(description = "物攻物防关系")
	val relativePhysicalStats: Int? = null,
	@param:JsonProperty("min_damage_taken")
	@get:JsonProperty("min_damage_taken")
	@field:Schema(description = "最低承伤")
	val minDamageTaken: Int? = null,
	@param:JsonProperty("min_move_count")
	@get:JsonProperty("min_move_count")
	@field:Schema(description = "最低技能数")
	val minMoveCount: Int? = null,
	@param:JsonProperty("min_steps")
	@get:JsonProperty("min_steps")
	@field:Schema(description = "最低步数")
	val minSteps: Int? = null,
	@param:JsonProperty("time_of_day")
	@get:JsonProperty("time_of_day")
	@field:Schema(description = "时间段")
	val timeOfDay: String? = null,
	@param:JsonProperty("needs_overworld_rain")
	@get:JsonProperty("needs_overworld_rain")
	@field:Schema(description = "需要下雨")
	val needsOverworldRain: Boolean? = null,
	@param:JsonProperty("turn_upside_down")
	@get:JsonProperty("turn_upside_down")
	@field:Schema(description = "需要倒置")
	val turnUpsideDown: Boolean? = null,
	@param:JsonProperty("near_special_rock")
	@get:JsonProperty("near_special_rock")
	@field:Schema(description = "靠近特殊岩石")
	val nearSpecialRock: Boolean? = null,
	@param:JsonProperty("needs_multiplayer")
	@get:JsonProperty("needs_multiplayer")
	@field:Schema(description = "需要多人")
	val needsMultiplayer: Boolean? = null,
	@param:JsonProperty("is_default")
	@get:JsonProperty("is_default")
	@field:Schema(description = "默认条件")
	val isDefault: Boolean? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"chain_id" to chainId,
		"from_species_id" to fromSpeciesId,
		"to_species_id" to toSpeciesId,
		"trigger_id" to triggerId,
		"item_id" to itemId,
		"held_item_id" to heldItemId,
		"known_skill_id" to knownSkillId,
		"known_element_id" to knownElementId,
		"location_id" to locationId,
		"party_species_id" to partySpeciesId,
		"party_element_id" to partyElementId,
		"trade_species_id" to tradeSpeciesId,
		"gender_id" to genderId,
		"region_id" to regionId,
		"min_level" to minLevel,
		"min_happiness" to minHappiness,
		"min_beauty" to minBeauty,
		"min_affection" to minAffection,
		"relative_physical_stats" to relativePhysicalStats,
		"min_damage_taken" to minDamageTaken,
		"min_move_count" to minMoveCount,
		"min_steps" to minSteps,
		"time_of_day" to timeOfDay,
		"needs_overworld_rain" to needsOverworldRain,
		"turn_upside_down" to turnUpsideDown,
		"near_special_rock" to nearSpecialRock,
		"needs_multiplayer" to needsMultiplayer,
		"is_default" to isDefault,
		)
}
