package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能状态附加效果维护响应。
 */
@Schema(description = "技能状态附加效果维护响应。")
data class BattleSkillStatusEffectResponse(
	@field:Schema(description = "技能状态效果主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "技能规则 ID。", example = "4")
	val skillRuleId: Long,
	@field:Schema(description = "状态规则 ID。", example = "1")
	val statusRuleId: Long,
	@field:Schema(description = "作用目标范围。", example = "TARGET")
	val targetScope: String,
	@field:Schema(description = "效果结算时机。", example = "AFTER_HIT")
	val effectTiming: String,
	@field:Schema(description = "触发概率百分比。", example = "10")
	val chancePercent: Int,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
