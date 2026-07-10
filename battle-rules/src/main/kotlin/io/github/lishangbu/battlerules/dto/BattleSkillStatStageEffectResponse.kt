package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能能力阶级效果维护响应。
 */
@Schema(description = "技能能力阶级效果维护响应。")
@Immutable
interface BattleSkillStatStageEffectResponse {
	@get:Schema(type = "string", description = "技能能力阶级效果主键 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(type = "string", description = "技能规则 ID。", example = "3")
	@JsonConverter(LongToStringConverter::class)
	val skillRuleId: Long
	@get:Schema(type = "string", description = "能力项 ID。", example = "2")
	@JsonConverter(LongToStringConverter::class)
	val statId: Long
	@get:Schema(description = "作用目标范围。", example = "ALL_OPPONENTS")
	val targetScope: String
	@get:Schema(description = "效果结算时机。", example = "AFTER_HIT")
	val effectTiming: String
	@get:Schema(description = "能力阶级变化值。", example = "-1")
	val stageDelta: Int
	@get:Schema(description = "触发概率百分比。", example = "100")
	val chancePercent: Int
	@get:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean
	@get:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int
}
