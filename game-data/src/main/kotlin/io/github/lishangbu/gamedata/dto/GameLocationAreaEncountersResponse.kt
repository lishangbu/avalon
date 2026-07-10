package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 区域精灵遭遇响应。
 */
@Schema(name = "GameLocationAreaEncountersResponse", description = "区域精灵遭遇响应。")
@Immutable
interface GameLocationAreaEncountersResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("area_id")
	@get:Schema(description = "区域 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val areaId: Long?
	@get:JsonProperty("creature_id")
	@get:Schema(description = "精灵 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val creatureId: Long?
	@get:JsonProperty("method_id")
	@get:Schema(description = "遭遇方式 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val methodId: Long?
	@get:JsonProperty("min_level")
	@get:Schema(description = "最低等级")
	val minLevel: Int?
	@get:JsonProperty("max_level")
	@get:Schema(description = "最高等级")
	val maxLevel: Int?
	@get:JsonProperty("chance")
	@get:Schema(description = "概率")
	val chance: Int?
	@get:JsonProperty("max_chance")
	@get:Schema(description = "最大概率")
	val maxChance: Int?
}
