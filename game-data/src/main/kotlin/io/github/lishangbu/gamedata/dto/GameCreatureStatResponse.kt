package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 精灵数值绑定响应。
 */
@Schema(name = "GameCreatureStatResponse", description = "精灵数值绑定响应。")
@Immutable
interface GameCreatureStatResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("creature_id")
	@get:Schema(description = "精灵 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val creatureId: Long?
	@get:JsonProperty("stat_id")
	@get:Schema(description = "数值项 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val statId: Long?
	@get:JsonProperty("base_value")
	@get:Schema(description = "基础值")
	val baseValue: Int?
	@get:JsonProperty("effort")
	@get:Schema(description = "努力收益")
	val effort: Int?
}
