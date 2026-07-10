package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 个体特征取值写入请求。
 */
@Schema(name = "GameCharacteristicValuesRequest", description = "个体特征取值写入请求。")
data class GameCharacteristicValuesRequest(
	@param:JsonProperty("characteristic_id")
	@get:JsonProperty("characteristic_id")
	@field:Schema(description = "特征 ID")
	val characteristicId: Long? = null,
	@param:JsonProperty("possible_value")
	@get:JsonProperty("possible_value")
	@field:Schema(description = "可能取值")
	val possibleValue: Int? = null
)
