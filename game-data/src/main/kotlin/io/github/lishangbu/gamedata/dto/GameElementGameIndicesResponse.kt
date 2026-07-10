package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 属性索引响应。
 */
@Schema(name = "GameElementGameIndicesResponse", description = "属性索引响应。")
@Immutable
interface GameElementGameIndicesResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("element_id")
	@get:Schema(description = "属性 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val elementId: Long?
	@get:JsonProperty("game_index")
	@get:Schema(description = "索引")
	val gameIndex: Int?
}
