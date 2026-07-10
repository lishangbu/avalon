package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 树果口味写入请求。
 */
@Schema(name = "GameBerryFlavorsRequest", description = "树果口味写入请求。")
data class GameBerryFlavorsRequest(
	@param:JsonProperty("code")
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String? = null,
	@param:JsonProperty("name")
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String? = null,
	@param:JsonProperty("contest_type_id")
	@get:JsonProperty("contest_type_id")
	@field:Schema(description = "评分类别 ID")
	val contestTypeId: Long? = null,
	@param:JsonProperty("enabled")
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean? = null
)
