package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能天气命中覆盖维护响应。
 */
@Schema(description = "技能天气命中覆盖维护响应。")
data class BattleSkillWeatherAccuracyOverrideResponse(
	@field:Schema(description = "技能天气命中覆盖主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "技能规则 ID。", example = "11")
	val skillRuleId: Long,
	@field:Schema(description = "天气规则 ID。", example = "3")
	val weatherRuleId: Long,
	@field:Schema(description = "命中覆盖百分比，1 到 100；为空表示必中。", example = "50", nullable = true)
	val accuracyPercent: Int?,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
