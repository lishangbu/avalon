package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 地点索引响应。
 */
@Schema(name = "GameLocationGameIndicesResponse", description = "地点索引响应。")
@Immutable
interface GameLocationGameIndicesResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("location_id")
	@get:Schema(description = "地点 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val locationId: Long?
	@get:JsonProperty("game_index")
	@get:Schema(description = "索引")
	val gameIndex: Int?
}
