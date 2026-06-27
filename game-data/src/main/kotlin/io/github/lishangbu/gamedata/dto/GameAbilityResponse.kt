package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 特性资料响应。
 */
@Schema(name = "GameAbilityResponse", description = "特性资料响应。")
data class GameAbilityResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("code")
	@field:Schema(description = "编码")
	val code: String?,
	@get:JsonProperty("name")
	@field:Schema(description = "名称")
	val name: String?,
	@get:JsonProperty("main_series")
	@field:Schema(description = "主体资料")
	val mainSeries: Boolean?,
	@get:JsonProperty("enabled")
	@field:Schema(description = "启用")
	val enabled: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameAbilityResponse =
			GameAbilityResponse(
				id = record.id,
				code = record.stringField("code"),
				name = record.stringField("name"),
				mainSeries = record.booleanField("main_series"),
				enabled = record.booleanField("enabled")
			)
	}
}
