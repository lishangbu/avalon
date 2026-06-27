package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 个体特征取值响应。
 */
@Schema(name = "GameCharacteristicValuesResponse", description = "个体特征取值响应。")
data class GameCharacteristicValuesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("characteristic_id")
	@field:Schema(description = "特征 ID")
	val characteristicId: Long?,
	@get:JsonProperty("possible_value")
	@field:Schema(description = "可能取值")
	val possibleValue: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameCharacteristicValuesResponse =
			GameCharacteristicValuesResponse(
				id = record.id,
				characteristicId = record.longField("characteristic_id"),
				possibleValue = record.intField("possible_value")
			)
	}
}
