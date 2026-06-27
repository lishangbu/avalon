package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 特性详情响应。
 */
@Schema(name = "GameAbilityDetailsResponse", description = "特性详情响应。")
data class GameAbilityDetailsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("ability_id")
	@field:Schema(description = "特性 ID")
	val abilityId: Long?,
	@get:JsonProperty("effect")
	@field:Schema(description = "效果")
	val effect: String?,
	@get:JsonProperty("short_effect")
	@field:Schema(description = "短效果")
	val shortEffect: String?,
	@get:JsonProperty("flavor_text")
	@field:Schema(description = "风味说明")
	val flavorText: String?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameAbilityDetailsResponse =
			GameAbilityDetailsResponse(
				id = record.id,
				abilityId = record.longField("ability_id"),
				effect = record.stringField("effect"),
				shortEffect = record.stringField("short_effect"),
				flavorText = record.stringField("flavor_text")
			)
	}
}
