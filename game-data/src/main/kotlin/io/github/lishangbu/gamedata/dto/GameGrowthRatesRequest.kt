package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 成长速率写入请求。
 */
@Schema(name = "GameGrowthRatesRequest", description = "成长速率写入请求。")
data class GameGrowthRatesRequest(
	@param:JsonProperty("code")
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String? = null,
	@param:JsonProperty("name")
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String? = null,
	@param:JsonProperty("formula")
	@get:JsonProperty("formula")
	@field:Schema(description = "公式")
	val formula: String? = null,
	@param:JsonProperty("description")
	@get:JsonProperty("description")
	@field:Schema(description = "说明")
	val description: String? = null,
	@param:JsonProperty("enabled")
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean? = null
)
