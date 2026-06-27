package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 进化条件响应。
 */
@Schema(name = "GameEvolutionDetailsResponse", description = "进化条件响应。")
data class GameEvolutionDetailsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("chain_id")
	@field:Schema(description = "进化链 ID")
	val chainId: Long?,
	@get:JsonProperty("from_species_id")
	@field:Schema(description = "起始种类 ID")
	val fromSpeciesId: Long?,
	@get:JsonProperty("to_species_id")
	@field:Schema(description = "目标种类 ID")
	val toSpeciesId: Long?,
	@get:JsonProperty("trigger_id")
	@field:Schema(description = "触发器 ID")
	val triggerId: Long?,
	@get:JsonProperty("item_id")
	@field:Schema(description = "道具 ID")
	val itemId: Long?,
	@get:JsonProperty("held_item_id")
	@field:Schema(description = "持有道具 ID")
	val heldItemId: Long?,
	@get:JsonProperty("known_skill_id")
	@field:Schema(description = "已掌握技能 ID")
	val knownSkillId: Long?,
	@get:JsonProperty("known_element_id")
	@field:Schema(description = "已掌握属性 ID")
	val knownElementId: Long?,
	@get:JsonProperty("location_id")
	@field:Schema(description = "地点 ID")
	val locationId: Long?,
	@get:JsonProperty("party_species_id")
	@field:Schema(description = "队伍种类 ID")
	val partySpeciesId: Long?,
	@get:JsonProperty("party_element_id")
	@field:Schema(description = "队伍属性 ID")
	val partyElementId: Long?,
	@get:JsonProperty("trade_species_id")
	@field:Schema(description = "交换种类 ID")
	val tradeSpeciesId: Long?,
	@get:JsonProperty("gender_id")
	@field:Schema(description = "性别 ID")
	val genderId: Long?,
	@get:JsonProperty("region_id")
	@field:Schema(description = "地区 ID")
	val regionId: Long?,
	@get:JsonProperty("min_level")
	@field:Schema(description = "最低等级")
	val minLevel: Int?,
	@get:JsonProperty("min_happiness")
	@field:Schema(description = "最低亲和度")
	val minHappiness: Int?,
	@get:JsonProperty("min_beauty")
	@field:Schema(description = "最低美丽度")
	val minBeauty: Int?,
	@get:JsonProperty("min_affection")
	@field:Schema(description = "最低友好度")
	val minAffection: Int?,
	@get:JsonProperty("relative_physical_stats")
	@field:Schema(description = "物攻物防关系")
	val relativePhysicalStats: Int?,
	@get:JsonProperty("min_damage_taken")
	@field:Schema(description = "最低承伤")
	val minDamageTaken: Int?,
	@get:JsonProperty("min_move_count")
	@field:Schema(description = "最低技能数")
	val minMoveCount: Int?,
	@get:JsonProperty("min_steps")
	@field:Schema(description = "最低步数")
	val minSteps: Int?,
	@get:JsonProperty("time_of_day")
	@field:Schema(description = "时间段")
	val timeOfDay: String?,
	@get:JsonProperty("needs_overworld_rain")
	@field:Schema(description = "需要下雨")
	val needsOverworldRain: Boolean?,
	@get:JsonProperty("turn_upside_down")
	@field:Schema(description = "需要倒置")
	val turnUpsideDown: Boolean?,
	@get:JsonProperty("near_special_rock")
	@field:Schema(description = "靠近特殊岩石")
	val nearSpecialRock: Boolean?,
	@get:JsonProperty("needs_multiplayer")
	@field:Schema(description = "需要多人")
	val needsMultiplayer: Boolean?,
	@get:JsonProperty("is_default")
	@field:Schema(description = "默认条件")
	val isDefault: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameEvolutionDetailsResponse =
			GameEvolutionDetailsResponse(
				id = record.id,
				chainId = record.longField("chain_id"),
				fromSpeciesId = record.longField("from_species_id"),
				toSpeciesId = record.longField("to_species_id"),
				triggerId = record.longField("trigger_id"),
				itemId = record.longField("item_id"),
				heldItemId = record.longField("held_item_id"),
				knownSkillId = record.longField("known_skill_id"),
				knownElementId = record.longField("known_element_id"),
				locationId = record.longField("location_id"),
				partySpeciesId = record.longField("party_species_id"),
				partyElementId = record.longField("party_element_id"),
				tradeSpeciesId = record.longField("trade_species_id"),
				genderId = record.longField("gender_id"),
				regionId = record.longField("region_id"),
				minLevel = record.intField("min_level"),
				minHappiness = record.intField("min_happiness"),
				minBeauty = record.intField("min_beauty"),
				minAffection = record.intField("min_affection"),
				relativePhysicalStats = record.intField("relative_physical_stats"),
				minDamageTaken = record.intField("min_damage_taken"),
				minMoveCount = record.intField("min_move_count"),
				minSteps = record.intField("min_steps"),
				timeOfDay = record.stringField("time_of_day"),
				needsOverworldRain = record.booleanField("needs_overworld_rain"),
				turnUpsideDown = record.booleanField("turn_upside_down"),
				nearSpecialRock = record.booleanField("near_special_rock"),
				needsMultiplayer = record.booleanField("needs_multiplayer"),
				isDefault = record.booleanField("is_default")
			)
	}
}
