package io.github.lishangbu.battlerules.dto

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能能力阶级操作维护响应。
 */
@Schema(description = "技能能力阶级操作维护响应。")
@Immutable
interface BattleSkillStatStageOperationResponse {
	@get:Schema(type = "string", description = "技能能力阶级操作主键 ID。", example = "1")
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	@get:Schema(type = "string", description = "技能规则 ID。", example = "89")
	@JsonConverter(LongToStringConverter::class)
	val skillRuleId: Long
	@get:Schema(type = "string", description = "能力项 ID。", example = "2")
	@JsonConverter(LongToStringConverter::class)
	val statId: Long
	@get:Schema(description = "操作类型。", example = "CLEAR")
	val operationKind: String
	@get:Schema(description = "操作目标范围。", example = "TARGET")
	val targetScope: String
	@get:Schema(description = "操作来源范围。", example = "TARGET")
	val sourceScope: String?
	@get:Schema(description = "效果结算时机。", example = "AFTER_HIT")
	val effectTiming: String
	@get:Schema(description = "触发概率百分比。", example = "100")
	val chancePercent: Int
	@get:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean
	@get:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int
}

