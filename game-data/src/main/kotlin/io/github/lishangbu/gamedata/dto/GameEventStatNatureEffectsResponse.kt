package io.github.lishangbu.gamedata.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/**
 * 活动能力性格影响响应。
 */
@Schema(name = "GameEventStatNatureEffectsResponse", description = "活动能力性格影响响应。")
@Immutable
interface GameEventStatNatureEffectsResponse {
	@get:Schema(description = "记录主键。", example = "1", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:JsonProperty("event_stat_id")
	@get:Schema(description = "活动能力项 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val eventStatId: Long?
	@get:JsonProperty("nature_id")
	@get:Schema(description = "性格 ID", type = "string")
	@JsonConverter(LongToStringConverter::class)
	val natureId: Long?
	@get:JsonProperty("effect_type")
	@get:Schema(description = "影响类型")
	val effectType: String?
}
