package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 区域遭遇条件绑定响应。
 */
@Schema(name = "GameLocationAreaEncounterConditionValuesResponse", description = "区域遭遇条件绑定响应。")
@Immutable
interface GameLocationAreaEncounterConditionValuesResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("encounter_id")
	@get:Schema(description = "遭遇 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val encounterId: Long?
	@get:JsonProperty("condition_value_id")
	@get:Schema(description = "遭遇条件值 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val conditionValueId: Long?
}
