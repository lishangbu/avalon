package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能跳过蓄力天气维护响应。
 */
@Schema(description = "技能跳过蓄力天气维护响应。")
@Immutable
interface BattleSkillChargeSkipWeatherResponse {
	@get:Schema(type = "string", description = "技能跳过蓄力天气主键 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(type = "string", description = "技能规则 ID。", example = "10")
	@JsonConverter(LongToStringConverter::class)
	val skillRuleId: Long
	@get:Schema(type = "string", description = "天气规则 ID。", example = "2")
	@JsonConverter(LongToStringConverter::class)
	val weatherRuleId: Long
	@get:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean
	@get:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int
}
