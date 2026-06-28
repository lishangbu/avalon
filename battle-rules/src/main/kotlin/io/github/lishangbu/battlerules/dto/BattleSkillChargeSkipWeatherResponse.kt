package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能跳过蓄力天气维护响应。
 */
@Schema(description = "技能跳过蓄力天气维护响应。")
data class BattleSkillChargeSkipWeatherResponse(
	@field:Schema(description = "技能跳过蓄力天气主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "技能规则 ID。", example = "10")
	val skillRuleId: Long,
	@field:Schema(description = "天气规则 ID。", example = "2")
	val weatherRuleId: Long,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
