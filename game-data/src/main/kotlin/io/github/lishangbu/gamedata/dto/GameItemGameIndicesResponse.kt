package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 道具索引响应。
 */
@Schema(name = "GameItemGameIndicesResponse", description = "道具索引响应。")
@Immutable
interface GameItemGameIndicesResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("item_id")
	@get:Schema(description = "道具 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val itemId: Long?
	@get:JsonProperty("game_index")
	@get:Schema(description = "索引")
	val gameIndex: Int?
}
