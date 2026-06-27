package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 道具分类口袋写入请求。
 */
@Schema(name = "GameItemCategoryPocketsRequest", description = "道具分类口袋写入请求。")
data class GameItemCategoryPocketsRequest(
	@param:JsonProperty("category_id")
	@get:JsonProperty("category_id")
	@field:Schema(description = "分类 ID")
	val categoryId: Long? = null,
	@param:JsonProperty("pocket_id")
	@get:JsonProperty("pocket_id")
	@field:Schema(description = "口袋 ID")
	val pocketId: Long? = null
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"category_id" to categoryId,
		"pocket_id" to pocketId,
		)
}
