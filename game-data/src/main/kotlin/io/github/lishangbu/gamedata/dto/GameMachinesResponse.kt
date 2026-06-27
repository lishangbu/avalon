package io.github.lishangbu.gamedata.dto

import io.github.lishangbu.gamedata.model.GameDataRecordResponse
import io.github.lishangbu.gamedata.support.*
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 机器资料响应。
 */
@Schema(name = "GameMachinesResponse", description = "机器资料响应。")
data class GameMachinesResponse(
	@field:Schema(description = "记录主键。", example = "1")
	val id: Long,
	@get:JsonProperty("item_id")
	@field:Schema(description = "道具 ID")
	val itemId: Long?,
	@get:JsonProperty("skill_id")
	@field:Schema(description = "技能 ID")
	val skillId: Long?,
) {
	companion object {
		fun from(record: GameDataRecordResponse): GameMachinesResponse =
			GameMachinesResponse(
				id = record.id,
				itemId = record.longField("item_id"),
				skillId = record.longField("skill_id")
			)
	}
}
