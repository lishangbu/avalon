package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 生物形态属性写入请求。
 */
@Schema(name = "GameCreatureFormElementsRequest", description = "生物形态属性写入请求。")
data class GameCreatureFormElementsRequest(
	@param:JsonProperty("form_id")
	@get:JsonProperty("form_id")
	@field:Schema(description = "形态 ID")
	val formId: Long? = null,
	@param:JsonProperty("element_id")
	@get:JsonProperty("element_id")
	@field:Schema(description = "属性 ID")
	val elementId: Long? = null,
	@param:JsonProperty("slot_order")
	@get:JsonProperty("slot_order")
	@field:Schema(description = "槽位顺序")
	val slotOrder: Int? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"form_id" to formId,
		"element_id" to elementId,
		"slot_order" to slotOrder,
		)
}
