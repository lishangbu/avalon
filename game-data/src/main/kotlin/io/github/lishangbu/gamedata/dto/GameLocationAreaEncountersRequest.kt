package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 区域生物遭遇写入请求。
 */
@Schema(name = "GameLocationAreaEncountersRequest", description = "区域生物遭遇写入请求。")
data class GameLocationAreaEncountersRequest(
	@param:JsonProperty("area_id")
	@get:JsonProperty("area_id")
	@field:Schema(description = "区域 ID")
	val areaId: Long? = null,
	@param:JsonProperty("creature_id")
	@get:JsonProperty("creature_id")
	@field:Schema(description = "生物 ID")
	val creatureId: Long? = null,
	@param:JsonProperty("method_id")
	@get:JsonProperty("method_id")
	@field:Schema(description = "遭遇方式 ID")
	val methodId: Long? = null,
	@param:JsonProperty("min_level")
	@get:JsonProperty("min_level")
	@field:Schema(description = "最低等级")
	val minLevel: Int? = null,
	@param:JsonProperty("max_level")
	@get:JsonProperty("max_level")
	@field:Schema(description = "最高等级")
	val maxLevel: Int? = null,
	@param:JsonProperty("chance")
	@get:JsonProperty("chance")
	@field:Schema(description = "概率")
	val chance: Int? = null,
	@param:JsonProperty("max_chance")
	@get:JsonProperty("max_chance")
	@field:Schema(description = "最大概率")
	val maxChance: Int? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"area_id" to areaId,
		"creature_id" to creatureId,
		"method_id" to methodId,
		"min_level" to minLevel,
		"max_level" to maxLevel,
		"chance" to chance,
		"max_chance" to maxChance,
		)
}
