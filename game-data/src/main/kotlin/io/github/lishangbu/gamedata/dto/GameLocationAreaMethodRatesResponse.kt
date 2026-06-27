package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 区域遭遇方式概率响应。
 */
@Schema(name = "GameLocationAreaMethodRatesResponse", description = "区域遭遇方式概率响应。")
data class GameLocationAreaMethodRatesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("area_id")
	@field:Schema(description = "区域 ID")
	val areaId: Long?,
	@get:JsonProperty("method_id")
	@field:Schema(description = "遭遇方式 ID")
	val methodId: Long?,
	@get:JsonProperty("rate")
	@field:Schema(description = "概率")
	val rate: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameLocationAreaMethodRatesResponse =
			GameLocationAreaMethodRatesResponse(
				id = record.id,
				areaId = record.longField("area_id"),
				methodId = record.longField("method_id"),
				rate = record.intField("rate")
			)
	}
}
