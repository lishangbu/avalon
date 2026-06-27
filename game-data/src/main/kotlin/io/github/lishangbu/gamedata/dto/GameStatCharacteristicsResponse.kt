package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 数值项特征响应。
 */
@Schema(name = "GameStatCharacteristicsResponse", description = "数值项特征响应。")
data class GameStatCharacteristicsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("stat_id")
	@field:Schema(description = "数值项 ID")
	val statId: Long?,
	@get:JsonProperty("characteristic_id")
	@field:Schema(description = "特征 ID")
	val characteristicId: Long?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameStatCharacteristicsResponse =
			GameStatCharacteristicsResponse(
				id = record.id,
				statId = record.longField("stat_id"),
				characteristicId = record.longField("characteristic_id")
			)
	}
}
