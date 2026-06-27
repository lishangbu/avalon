package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 道具属性绑定写入请求。
 */
@Schema(name = "GameItemAttributeBindingsRequest", description = "道具属性绑定写入请求。")
data class GameItemAttributeBindingsRequest(
	@param:JsonProperty("item_id")
	@get:JsonProperty("item_id")
	@field:Schema(description = "道具 ID")
	val itemId: Long? = null,
	@param:JsonProperty("attribute_id")
	@get:JsonProperty("attribute_id")
	@field:Schema(description = "属性 ID")
	val attributeId: Long? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"item_id" to itemId,
		"attribute_id" to attributeId,
		)
}
