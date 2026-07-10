package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 性格资料写入请求。
 */
@Schema(name = "GameNaturesRequest", description = "性格资料写入请求。")
data class GameNaturesRequest(
	@param:JsonProperty("code")
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String? = null,
	@param:JsonProperty("name")
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String? = null,
	@param:JsonProperty("increased_stat_id")
	@get:JsonProperty("increased_stat_id")
	@field:Schema(description = "提升数值项 ID")
	val increasedStatId: Long? = null,
	@param:JsonProperty("decreased_stat_id")
	@get:JsonProperty("decreased_stat_id")
	@field:Schema(description = "降低数值项 ID")
	val decreasedStatId: Long? = null,
	@param:JsonProperty("enabled")
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean? = null
)
