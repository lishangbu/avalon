package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 进化条件响应。
 */
@Schema(name = "GameEvolutionDetailsResponse", description = "进化条件响应。")
@Immutable
interface GameEvolutionDetailsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("chain_id")
	@get:Schema(description = "进化链 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val chainId: Long?
	@get:JsonProperty("from_species_id")
	@get:Schema(description = "起始种类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val fromSpeciesId: Long?
	@get:JsonProperty("to_species_id")
	@get:Schema(description = "目标种类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val toSpeciesId: Long?
	@get:JsonProperty("trigger_id")
	@get:Schema(description = "触发器 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val triggerId: Long?
	@get:JsonProperty("item_id")
	@get:Schema(description = "道具 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val itemId: Long?
	@get:JsonProperty("held_item_id")
	@get:Schema(description = "持有道具 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val heldItemId: Long?
	@get:JsonProperty("known_skill_id")
	@get:Schema(description = "已掌握技能 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val knownSkillId: Long?
	@get:JsonProperty("known_element_id")
	@get:Schema(description = "已掌握属性 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val knownElementId: Long?
	@get:JsonProperty("location_id")
	@get:Schema(description = "地点 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val locationId: Long?
	@get:JsonProperty("party_species_id")
	@get:Schema(description = "队伍种类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val partySpeciesId: Long?
	@get:JsonProperty("party_element_id")
	@get:Schema(description = "队伍属性 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val partyElementId: Long?
	@get:JsonProperty("trade_species_id")
	@get:Schema(description = "交换种类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val tradeSpeciesId: Long?
	@get:JsonProperty("gender_id")
	@get:Schema(description = "性别 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val genderId: Long?
	@get:JsonProperty("region_id")
	@get:Schema(description = "地区 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val regionId: Long?
	@get:JsonProperty("min_level")
	@get:Schema(description = "最低等级")
	val minLevel: Int?
	@get:JsonProperty("min_happiness")
	@get:Schema(description = "最低亲和度")
	val minHappiness: Int?
	@get:JsonProperty("min_beauty")
	@get:Schema(description = "最低美丽度")
	val minBeauty: Int?
	@get:JsonProperty("min_affection")
	@get:Schema(description = "最低友好度")
	val minAffection: Int?
	@get:JsonProperty("relative_physical_stats")
	@get:Schema(description = "物攻物防关系")
	val relativePhysicalStats: Int?
	@get:JsonProperty("min_damage_taken")
	@get:Schema(description = "最低承伤")
	val minDamageTaken: Int?
	@get:JsonProperty("min_move_count")
	@get:Schema(description = "最低技能数")
	val minMoveCount: Int?
	@get:JsonProperty("min_steps")
	@get:Schema(description = "最低步数")
	val minSteps: Int?
	@get:JsonProperty("time_of_day")
	@get:Schema(description = "时间段")
	val timeOfDay: String?
	@get:JsonProperty("needs_overworld_rain")
	@get:Schema(description = "需要下雨")
	val needsOverworldRain: Boolean?
	@get:JsonProperty("turn_upside_down")
	@get:Schema(description = "需要倒置")
	val turnUpsideDown: Boolean?
	@get:JsonProperty("near_special_rock")
	@get:Schema(description = "靠近特殊岩石")
	val nearSpecialRock: Boolean?
	@get:JsonProperty("needs_multiplayer")
	@get:Schema(description = "需要多人")
	val needsMultiplayer: Boolean?
	@get:JsonProperty("is_default")
	@get:Schema(description = "默认条件")
	val isDefault: Boolean?
}
