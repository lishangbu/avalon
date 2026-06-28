package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能跳过蓄力天气维护请求。
 */
@Schema(description = "技能跳过蓄力天气维护请求。")
data class BattleSkillChargeSkipWeatherRequest(
	@field:Schema(description = "技能规则 ID，必须是已配置为蓄力后发动的技能规则。", example = "10")
	var skillRuleId: Long = 0,
	@field:Schema(description = "天气规则 ID，不能引用无天气。", example = "2")
	var weatherRuleId: Long = 0,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
