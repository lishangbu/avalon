package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 树果资料响应。
 */
@Schema(name = "GameBerriesResponse", description = "树果资料响应。")
@Immutable
interface GameBerriesResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("code")
	@get:Schema(description = "编码")
	val code: String?
	@get:JsonProperty("name")
	@get:Schema(description = "名称")
	val name: String?
	@get:JsonProperty("item_id")
	@get:Schema(description = "道具 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val itemId: Long?
	@get:JsonProperty("firmness_id")
	@get:Schema(description = "硬度 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val firmnessId: Long?
	@get:JsonProperty("natural_gift_element_id")
	@get:Schema(description = "自然效果属性 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val naturalGiftElementId: Long?
	@get:JsonProperty("growth_time")
	@get:Schema(description = "成长时间")
	val growthTime: Int?
	@get:JsonProperty("max_harvest")
	@get:Schema(description = "最大收获")
	val maxHarvest: Int?
	@get:JsonProperty("natural_gift_power")
	@get:Schema(description = "自然效果威力")
	val naturalGiftPower: Int?
	@get:JsonProperty("size")
	@get:Schema(description = "尺寸")
	val size: Int?
	@get:JsonProperty("smoothness")
	@get:Schema(description = "顺滑度")
	val smoothness: Int?
	@get:JsonProperty("soil_dryness")
	@get:Schema(description = "土壤干燥度")
	val soilDryness: Int?
	@get:JsonProperty("enabled")
	@get:Schema(description = "启用")
	val enabled: Boolean?
}
