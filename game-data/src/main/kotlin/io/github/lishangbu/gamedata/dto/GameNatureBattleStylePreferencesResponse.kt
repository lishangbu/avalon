package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 性格战斗风格偏好响应。
 */
@Schema(name = "GameNatureBattleStylePreferencesResponse", description = "性格战斗风格偏好响应。")
data class GameNatureBattleStylePreferencesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("nature_id")
	@field:Schema(description = "性格 ID")
	val natureId: Long?,
	@get:JsonProperty("battle_style_id")
	@field:Schema(description = "战斗风格 ID")
	val battleStyleId: Long?,
	@get:JsonProperty("low_hp_preference")
	@field:Schema(description = "低体力偏好")
	val lowHpPreference: Int?,
	@get:JsonProperty("high_hp_preference")
	@field:Schema(description = "高体力偏好")
	val highHpPreference: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameNatureBattleStylePreferencesResponse =
			GameNatureBattleStylePreferencesResponse(
				id = record.id,
				natureId = record.longField("nature_id"),
				battleStyleId = record.longField("battle_style_id"),
				lowHpPreference = record.intField("low_hp_preference"),
				highHpPreference = record.intField("high_hp_preference")
			)
	}
}
