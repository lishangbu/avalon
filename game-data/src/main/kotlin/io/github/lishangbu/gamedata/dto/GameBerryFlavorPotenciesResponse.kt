package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 树果口味强度响应。
 */
@Schema(name = "GameBerryFlavorPotenciesResponse", description = "树果口味强度响应。")
data class GameBerryFlavorPotenciesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("berry_id")
	@field:Schema(description = "树果 ID")
	val berryId: Long?,
	@get:JsonProperty("flavor_id")
	@field:Schema(description = "口味 ID")
	val flavorId: Long?,
	@get:JsonProperty("potency")
	@field:Schema(description = "强度")
	val potency: Int?
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameBerryFlavorPotenciesResponse =
			GameBerryFlavorPotenciesResponse(
				id = record.id,
				berryId = record.longField("berry_id"),
				flavorId = record.longField("flavor_id"),
				potency = record.intField("potency")
			)
	}
}
