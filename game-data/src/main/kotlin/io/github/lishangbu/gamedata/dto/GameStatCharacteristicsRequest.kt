package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 数值项特征写入请求。
 */
@Schema(name = "GameStatCharacteristicsRequest", description = "数值项特征写入请求。")
data class GameStatCharacteristicsRequest(
	@param:JsonProperty("stat_id")
	@get:JsonProperty("stat_id")
	@field:Schema(description = "数值项 ID")
	val statId: Long? = null,
	@param:JsonProperty("characteristic_id")
	@get:JsonProperty("characteristic_id")
	@field:Schema(description = "特征 ID")
	val characteristicId: Long? = null
)
