package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能能力阶级效果维护请求。
 */
@Schema(description = "技能能力阶级效果维护请求。")
data class BattleSkillStatStageEffectRequest(
	@field:Schema(description = "技能规则 ID。", example = "3")
	var skillRuleId: Long = 0,
	@field:Schema(description = "能力项 ID。", example = "2")
	var statId: Long = 0,
	@field:Schema(description = "作用目标范围。", example = "ALL_OPPONENTS")
	var targetScope: String = "",
	@field:Schema(description = "效果结算时机。", example = "AFTER_HIT")
	var effectTiming: String = "",
	@field:Schema(description = "能力阶级变化值。", example = "-1")
	var stageDelta: Int = 0,
	@field:Schema(description = "触发概率百分比。", example = "100")
	var chancePercent: Int = 100,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
