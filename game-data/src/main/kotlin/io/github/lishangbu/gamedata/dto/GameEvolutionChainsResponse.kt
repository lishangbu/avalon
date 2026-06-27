package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 进化链响应。
 */
@Schema(name = "GameEvolutionChainsResponse", description = "进化链响应。")
data class GameEvolutionChainsResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("baby_trigger_item_id")
	@field:Schema(description = "幼体触发道具 ID")
	val babyTriggerItemId: Long?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameEvolutionChainsResponse =
			GameEvolutionChainsResponse(
				id = record.id,
				babyTriggerItemId = record.longField("baby_trigger_item_id")
			)
	}
}
