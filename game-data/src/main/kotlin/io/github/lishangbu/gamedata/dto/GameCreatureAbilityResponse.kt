package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 精灵特性绑定响应。
 */
@Schema(name = "GameCreatureAbilityResponse", description = "精灵特性绑定响应。")
data class GameCreatureAbilityResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("creature_id")
	@field:Schema(description = "精灵 ID")
	val creatureId: Long?,
	@get:JsonProperty("ability_id")
	@field:Schema(description = "特性 ID")
	val abilityId: Long?,
	@get:JsonProperty("slot_order")
	@field:Schema(description = "槽位")
	val slotOrder: Int?,
	@get:JsonProperty("hidden")
	@field:Schema(description = "隐藏")
	val hidden: Boolean?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameCreatureAbilityResponse =
			GameCreatureAbilityResponse(
				id = record.id,
				creatureId = record.longField("creature_id"),
				abilityId = record.longField("ability_id"),
				slotOrder = record.intField("slot_order"),
				hidden = record.booleanField("hidden")
			)
	}
}
