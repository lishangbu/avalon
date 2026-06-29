package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能能力阶级操作维护响应。
 */
@Schema(description = "技能能力阶级操作维护响应。")
data class BattleSkillStatStageOperationResponse(
	@field:Schema(description = "技能能力阶级操作主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "技能规则 ID。", example = "89")
	val skillRuleId: Long,
	@field:Schema(description = "能力项 ID。", example = "2")
	val statId: Long,
	@field:Schema(description = "操作类型。", example = "CLEAR")
	val operationKind: String,
	@field:Schema(description = "操作目标范围。", example = "TARGET")
	val targetScope: String,
	@field:Schema(description = "操作来源范围。", example = "TARGET")
	val sourceScope: String?,
	@field:Schema(description = "效果结算时机。", example = "AFTER_HIT")
	val effectTiming: String,
	@field:Schema(description = "触发概率百分比。", example = "100")
	val chancePercent: Int,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)

