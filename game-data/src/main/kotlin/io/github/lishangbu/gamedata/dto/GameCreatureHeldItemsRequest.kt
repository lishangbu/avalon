package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 精灵持有道具写入请求。
 */
@Schema(name = "GameCreatureHeldItemsRequest", description = "精灵持有道具写入请求。")
data class GameCreatureHeldItemsRequest(
	@param:JsonProperty("creature_id")
	@get:JsonProperty("creature_id")
	@field:Schema(description = "精灵 ID")
	val creatureId: Long? = null,
	@param:JsonProperty("item_id")
	@get:JsonProperty("item_id")
	@field:Schema(description = "道具 ID")
	val itemId: Long? = null,
	@param:JsonProperty("rarity")
	@get:JsonProperty("rarity")
	@field:Schema(description = "稀有度")
	val rarity: Int? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"creature_id" to creatureId,
		"item_id" to itemId,
		"rarity" to rarity,
		)
}
