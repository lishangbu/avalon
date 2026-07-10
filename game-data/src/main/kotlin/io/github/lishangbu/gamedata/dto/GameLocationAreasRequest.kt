package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 地点区域写入请求。
 */
@Schema(name = "GameLocationAreasRequest", description = "地点区域写入请求。")
data class GameLocationAreasRequest(
	@param:JsonProperty("code")
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String? = null,
	@param:JsonProperty("name")
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String? = null,
	@param:JsonProperty("location_id")
	@get:JsonProperty("location_id")
	@field:Schema(description = "地点 ID")
	val locationId: Long? = null,
	@param:JsonProperty("game_index")
	@get:JsonProperty("game_index")
	@field:Schema(description = "索引")
	val gameIndex: Int? = null,
	@param:JsonProperty("enabled")
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean? = null
)
