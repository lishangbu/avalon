package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 精灵数值绑定写入请求。
 */
@Schema(name = "GameCreatureStatRequest", description = "精灵数值绑定写入请求。")
data class GameCreatureStatRequest(
	@param:JsonProperty("creature_id")
	@get:JsonProperty("creature_id")
	@field:Schema(description = "精灵 ID")
	val creatureId: Long? = null,
	@param:JsonProperty("stat_id")
	@get:JsonProperty("stat_id")
	@field:Schema(description = "数值项 ID")
	val statId: Long? = null,
	@param:JsonProperty("base_value")
	@get:JsonProperty("base_value")
	@field:Schema(description = "基础值")
	val baseValue: Int? = null,
	@param:JsonProperty("effort")
	@get:JsonProperty("effort")
	@field:Schema(description = "努力收益")
	val effort: Int? = null
)
