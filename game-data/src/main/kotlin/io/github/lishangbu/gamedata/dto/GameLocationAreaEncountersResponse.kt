package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 区域生物遭遇响应。
 */
@Schema(name = "GameLocationAreaEncountersResponse", description = "区域生物遭遇响应。")
data class GameLocationAreaEncountersResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("area_id")
	@field:Schema(description = "区域 ID")
	val areaId: Long?,
	@get:JsonProperty("creature_id")
	@field:Schema(description = "生物 ID")
	val creatureId: Long?,
	@get:JsonProperty("method_id")
	@field:Schema(description = "遭遇方式 ID")
	val methodId: Long?,
	@get:JsonProperty("min_level")
	@field:Schema(description = "最低等级")
	val minLevel: Int?,
	@get:JsonProperty("max_level")
	@field:Schema(description = "最高等级")
	val maxLevel: Int?,
	@get:JsonProperty("chance")
	@field:Schema(description = "概率")
	val chance: Int?,
	@get:JsonProperty("max_chance")
	@field:Schema(description = "最大概率")
	val maxChance: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameLocationAreaEncountersResponse =
			GameLocationAreaEncountersResponse(
				id = record.id,
				areaId = record.longField("area_id"),
				creatureId = record.longField("creature_id"),
				methodId = record.longField("method_id"),
				minLevel = record.intField("min_level"),
				maxLevel = record.intField("max_level"),
				chance = record.intField("chance"),
				maxChance = record.intField("max_chance")
			)
	}
}
