package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能场上效果维护响应。
 */
@Schema(description = "技能场上效果维护响应。")
@Immutable
interface BattleSkillFieldEffectResponse {
	@get:Schema(type = "string", description = "技能场上效果主键 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(type = "string", description = "技能规则 ID。", example = "9")
	@JsonConverter(LongToStringConverter::class)
	val skillRuleId: Long
	@get:Schema(type = "string", description = "场上效果规则 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val fieldRuleId: Long
	@get:Schema(description = "作用侧，USER_SIDE 表示使用者一侧，TARGET_SIDE 表示目标一侧。", example = "USER_SIDE")
	val targetSide: String
	@get:Schema(description = "效果结算时机。", example = "AFTER_HIT")
	val effectTiming: String
	@get:Schema(type = "string", description = "要求存在的天气规则 ID；为空表示无天气前置条件。", example = "5")
	@JsonConverter(LongToStringConverter::class)
	val requiredWeatherRuleId: Long?
	@get:Schema(description = "触发概率百分比。", example = "100")
	val chancePercent: Int
	@get:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean
	@get:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int
}
