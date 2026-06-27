package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 数值项性格影响响应。
 */
@Schema(name = "GameStatNatureEffectsResponse", description = "数值项性格影响响应。")
data class GameStatNatureEffectsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("stat_id")
	@field:Schema(description = "数值项 ID")
	val statId: Long?,
	@get:JsonProperty("nature_id")
	@field:Schema(description = "性格 ID")
	val natureId: Long?,
	@get:JsonProperty("effect_type")
	@field:Schema(description = "影响类型")
	val effectType: String?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameStatNatureEffectsResponse =
			GameStatNatureEffectsResponse(
				id = record.id,
				statId = record.longField("stat_id"),
				natureId = record.longField("nature_id"),
				effectType = record.stringField("effect_type")
			)
	}
}
