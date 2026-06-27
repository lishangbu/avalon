package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 树果资料响应。
 */
@Schema(name = "GameBerriesResponse", description = "树果资料响应。")
data class GameBerriesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("item_id")
	@field:Schema(description = "道具 ID")
	val itemId: Long?,
	@get:JsonProperty("firmness_id")
	@field:Schema(description = "硬度 ID")
	val firmnessId: Long?,
	@get:JsonProperty("natural_gift_element_id")
	@field:Schema(description = "自然效果属性 ID")
	val naturalGiftElementId: Long?,
	@get:JsonProperty("growth_time")
	@field:Schema(description = "成长时间")
	val growthTime: Int?,
	@get:JsonProperty("max_harvest")
	@field:Schema(description = "最大收获")
	val maxHarvest: Int?,
	@get:JsonProperty("natural_gift_power")
	@field:Schema(description = "自然效果威力")
	val naturalGiftPower: Int?,
	@get:JsonProperty("size")
	@field:Schema(description = "尺寸")
	val size: Int?,
	@get:JsonProperty("smoothness")
	@field:Schema(description = "顺滑度")
	val smoothness: Int?,
	@get:JsonProperty("soil_dryness")
	@field:Schema(description = "土壤干燥度")
	val soilDryness: Int?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameBerriesResponse =
			GameBerriesResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				itemId = record.longField("item_id"),
				firmnessId = record.longField("firmness_id"),
				naturalGiftElementId = record.longField("natural_gift_element_id"),
				growthTime = record.intField("growth_time"),
				maxHarvest = record.intField("max_harvest"),
				naturalGiftPower = record.intField("natural_gift_power"),
				size = record.intField("size"),
				smoothness = record.intField("smoothness"),
				soilDryness = record.intField("soil_dryness"),
				enabled = record.booleanField("enabled")
			)
	}
}
