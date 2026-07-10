package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 精灵属性绑定写入请求。
 */
@Schema(name = "GameCreatureElementRequest", description = "精灵属性绑定写入请求。")
data class GameCreatureElementRequest(
	@param:JsonProperty("creature_id")
	@get:JsonProperty("creature_id")
	@field:Schema(description = "精灵 ID")
	val creatureId: Long? = null,
	@param:JsonProperty("element_id")
	@get:JsonProperty("element_id")
	@field:Schema(description = "属性 ID")
	val elementId: Long? = null,
	@param:JsonProperty("slot_order")
	@get:JsonProperty("slot_order")
	@field:Schema(description = "槽位")
	val slotOrder: Int? = null
)
