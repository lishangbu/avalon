package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 区域遭遇方式概率写入请求。
 */
@Schema(name = "GameLocationAreaMethodRatesRequest", description = "区域遭遇方式概率写入请求。")
data class GameLocationAreaMethodRatesRequest(
	@param:JsonProperty("area_id")
	@get:JsonProperty("area_id")
	@field:Schema(description = "区域 ID")
	val areaId: Long? = null,
	@param:JsonProperty("method_id")
	@get:JsonProperty("method_id")
	@field:Schema(description = "遭遇方式 ID")
	val methodId: Long? = null,
	@param:JsonProperty("rate")
	@get:JsonProperty("rate")
	@field:Schema(description = "概率")
	val rate: Int? = null
)
