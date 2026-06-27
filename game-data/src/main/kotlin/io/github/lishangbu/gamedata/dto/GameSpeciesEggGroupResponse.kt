package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 种类分组绑定响应。
 */
@Schema(name = "GameSpeciesEggGroupResponse", description = "种类分组绑定响应。")
data class GameSpeciesEggGroupResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("species_id")
	@field:Schema(description = "种类 ID")
	val speciesId: Long?,
	@get:JsonProperty("egg_group_id")
	@field:Schema(description = "分组 ID")
	val eggGroupId: Long?,
	@get:JsonProperty("slot_order")
	@field:Schema(description = "槽位")
	val slotOrder: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameSpeciesEggGroupResponse =
			GameSpeciesEggGroupResponse(
				id = record.id,
				speciesId = record.longField("species_id"),
				eggGroupId = record.longField("egg_group_id"),
				slotOrder = record.intField("slot_order")
			)
	}
}
