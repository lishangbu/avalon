package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 进化链写入请求。
 */
@Schema(name = "GameEvolutionChainsRequest", description = "进化链写入请求。")
data class GameEvolutionChainsRequest(
	@param:JsonProperty("baby_trigger_item_id")
	@get:JsonProperty("baby_trigger_item_id")
	@field:Schema(description = "幼体触发道具 ID")
	val babyTriggerItemId: Long? = null
)
