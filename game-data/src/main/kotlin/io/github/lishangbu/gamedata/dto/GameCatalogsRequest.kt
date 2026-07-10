package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 图鉴目录写入请求。
 */
@Schema(name = "GameCatalogsRequest", description = "图鉴目录写入请求。")
data class GameCatalogsRequest(
	@param:JsonProperty("code")
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String? = null,
	@param:JsonProperty("name")
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String? = null,
	@param:JsonProperty("region_id")
	@get:JsonProperty("region_id")
	@field:Schema(description = "地区 ID")
	val regionId: Long? = null,
	@param:JsonProperty("main_series")
	@get:JsonProperty("main_series")
	@field:Schema(description = "主体资料")
	val mainSeries: Boolean? = null,
	@param:JsonProperty("description")
	@get:JsonProperty("description")
	@field:Schema(description = "说明")
	val description: String? = null,
	@param:JsonProperty("enabled")
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean? = null
)
