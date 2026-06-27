package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 树果资料写入请求。
 */
@Schema(name = "GameBerriesRequest", description = "树果资料写入请求。")
data class GameBerriesRequest(
	@param:JsonProperty("code")
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String? = null,
	@param:JsonProperty("name")
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String? = null,
	@param:JsonProperty("item_id")
	@get:JsonProperty("item_id")
	@field:Schema(description = "道具 ID")
	val itemId: Long? = null,
	@param:JsonProperty("firmness_id")
	@get:JsonProperty("firmness_id")
	@field:Schema(description = "硬度 ID")
	val firmnessId: Long? = null,
	@param:JsonProperty("natural_gift_element_id")
	@get:JsonProperty("natural_gift_element_id")
	@field:Schema(description = "自然效果属性 ID")
	val naturalGiftElementId: Long? = null,
	@param:JsonProperty("growth_time")
	@get:JsonProperty("growth_time")
	@field:Schema(description = "成长时间")
	val growthTime: Int? = null,
	@param:JsonProperty("max_harvest")
	@get:JsonProperty("max_harvest")
	@field:Schema(description = "最大收获")
	val maxHarvest: Int? = null,
	@param:JsonProperty("natural_gift_power")
	@get:JsonProperty("natural_gift_power")
	@field:Schema(description = "自然效果威力")
	val naturalGiftPower: Int? = null,
	@param:JsonProperty("size")
	@get:JsonProperty("size")
	@field:Schema(description = "尺寸")
	val size: Int? = null,
	@param:JsonProperty("smoothness")
	@get:JsonProperty("smoothness")
	@field:Schema(description = "顺滑度")
	val smoothness: Int? = null,
	@param:JsonProperty("soil_dryness")
	@get:JsonProperty("soil_dryness")
	@field:Schema(description = "土壤干燥度")
	val soilDryness: Int? = null,
	@param:JsonProperty("enabled")
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"code" to code,
		"name" to name,
		"item_id" to itemId,
		"firmness_id" to firmnessId,
		"natural_gift_element_id" to naturalGiftElementId,
		"growth_time" to growthTime,
		"max_harvest" to maxHarvest,
		"natural_gift_power" to naturalGiftPower,
		"size" to size,
		"smoothness" to smoothness,
		"soil_dryness" to soilDryness,
		"enabled" to enabled,
		)
}
