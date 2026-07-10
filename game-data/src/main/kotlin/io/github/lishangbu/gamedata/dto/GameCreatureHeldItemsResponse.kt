package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 精灵持有道具响应。
 */
@Schema(name = "GameCreatureHeldItemsResponse", description = "精灵持有道具响应。")
@Immutable
interface GameCreatureHeldItemsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("creature_id")
	@get:Schema(description = "精灵 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val creatureId: Long?
	@get:JsonProperty("item_id")
	@get:Schema(description = "道具 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val itemId: Long?
	@get:JsonProperty("rarity")
	@get:Schema(description = "稀有度")
	val rarity: Int?
}
