package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 精灵特性绑定写入请求。
 */
@Schema(name = "GameCreatureAbilityRequest", description = "精灵特性绑定写入请求。")
data class GameCreatureAbilityRequest(
	@param:JsonProperty("creature_id")
	@get:JsonProperty("creature_id")
	@field:Schema(description = "精灵 ID")
	val creatureId: Long? = null,
	@param:JsonProperty("ability_id")
	@get:JsonProperty("ability_id")
	@field:Schema(description = "特性 ID")
	val abilityId: Long? = null,
	@param:JsonProperty("slot_order")
	@get:JsonProperty("slot_order")
	@field:Schema(description = "槽位")
	val slotOrder: Int? = null,
	@param:JsonProperty("hidden")
	@get:JsonProperty("hidden")
	@field:Schema(description = "隐藏")
	val hidden: Boolean? = null
)
