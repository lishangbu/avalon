package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 种类分组绑定写入请求。
 */
@Schema(name = "GameSpeciesEggGroupRequest", description = "种类分组绑定写入请求。")
data class GameSpeciesEggGroupRequest(
	@param:JsonProperty("species_id")
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long? = null,
	@param:JsonProperty("egg_group_id")
	@get:JsonProperty("egg_group_id")
	@field:Schema(description = "分组 ID")
	val eggGroupId: Long? = null,
	@param:JsonProperty("slot_order")
	@get:JsonProperty("slot_order")
	@field:Schema(description = "槽位")
	val slotOrder: Int? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"species_id" to speciesId,
		"egg_group_id" to eggGroupId,
		"slot_order" to slotOrder,
		)
}
