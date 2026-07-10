package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 道具分类口袋响应。
 */
@Schema(name = "GameItemCategoryPocketsResponse", description = "道具分类口袋响应。")
@Immutable
interface GameItemCategoryPocketsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("category_id")
	@get:Schema(description = "分类 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val categoryId: Long?
	@get:JsonProperty("pocket_id")
	@get:Schema(description = "口袋 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val pocketId: Long?
}
