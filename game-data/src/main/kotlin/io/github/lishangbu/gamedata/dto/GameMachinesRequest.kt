package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 机器资料写入请求。
 */
@Schema(name = "GameMachinesRequest", description = "机器资料写入请求。")
data class GameMachinesRequest(
	@param:JsonProperty("item_id")
	@get:JsonProperty("item_id")
	@field:Schema(description = "道具 ID")
	val itemId: Long? = null,
	@param:JsonProperty("skill_id")
	@get:JsonProperty("skill_id")
	@field:Schema(description = "技能 ID")
	val skillId: Long? = null,
) : GameDataWriteRequest {
	override fun toFields(): Map<String, Any?> =
		mapOf(
		"item_id" to itemId,
		"skill_id" to skillId,
		)
}
