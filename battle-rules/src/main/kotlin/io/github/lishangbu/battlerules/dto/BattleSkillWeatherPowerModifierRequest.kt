package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能天气威力倍率维护请求。
 */
@Schema(description = "技能天气威力倍率维护请求。")
data class BattleSkillWeatherPowerModifierRequest(
	@field:Schema(description = "技能规则 ID。", example = "10")
	var skillRuleId: Long = 0,
	@field:Schema(description = "天气规则 ID，不能引用无天气。", example = "3")
	var weatherRuleId: Long = 0,
	@field:Schema(description = "威力倍率，必须大于 0。", example = "0.5")
	var powerMultiplier: Double = 1.0,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
