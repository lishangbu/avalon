package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 进化链响应。
 */
@Schema(name = "GameEvolutionChainsResponse", description = "进化链响应。")
@Immutable
interface GameEvolutionChainsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("baby_trigger_item_id")
	@get:Schema(description = "幼体触发道具 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val babyTriggerItemId: Long?
}
