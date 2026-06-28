package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能天气命中覆盖维护请求。
 */
@Schema(description = "技能天气命中覆盖维护请求。")
data class BattleSkillWeatherAccuracyOverrideRequest(
	@field:Schema(description = "技能规则 ID。", example = "11")
	var skillRuleId: Long = 0,
	@field:Schema(description = "天气规则 ID，不能引用无天气。", example = "3")
	var weatherRuleId: Long = 0,
	@field:Schema(description = "命中覆盖百分比，1 到 100；为空表示必中。", example = "50", nullable = true)
	var accuracyPercent: Int? = null,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
