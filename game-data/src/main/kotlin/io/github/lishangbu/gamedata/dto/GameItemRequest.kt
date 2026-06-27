package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 道具资料写入请求。
 */
@Schema(name = "GameItemRequest", description = "道具资料写入请求。")
data class GameItemRequest(
	@param:JsonProperty("code")
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String? = null,
	@param:JsonProperty("name")
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String? = null,
	@param:JsonProperty("category_id")
	@get:JsonProperty("category_id")
	@field:Schema(description = "分类 ID")
	val categoryId: Long? = null,
	@param:JsonProperty("cost")
	@get:JsonProperty("cost")
	@field:Schema(description = "价格")
	val cost: Int? = null,
	@param:JsonProperty("fling_power")
	@get:JsonProperty("fling_power")
	@field:Schema(description = "投掷威力")
	val flingPower: Int? = null,
	@param:JsonProperty("enabled")
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"code" to code,
		"name" to name,
		"category_id" to categoryId,
		"cost" to cost,
		"fling_power" to flingPower,
		"enabled" to enabled,
		)
}
