package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 精灵索引响应。
 */
@Schema(name = "GameCreatureGameIndicesResponse", description = "精灵索引响应。")
@Immutable
interface GameCreatureGameIndicesResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("creature_id")
	@get:Schema(description = "精灵 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val creatureId: Long?
	@get:JsonProperty("game_index")
	@get:Schema(description = "索引")
	val gameIndex: Int?
}
