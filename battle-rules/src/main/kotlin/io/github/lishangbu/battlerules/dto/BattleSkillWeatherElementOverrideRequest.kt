package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能天气属性覆盖维护请求。
 */
@Schema(description = "技能天气属性覆盖维护请求。")
data class BattleSkillWeatherElementOverrideRequest(
	@field:Schema(description = "技能规则 ID。", example = "13")
	var skillRuleId: Long = 0,
	@field:Schema(description = "天气规则 ID，不能引用无天气。", example = "3")
	var weatherRuleId: Long = 0,
	@field:Schema(description = "目标属性 ID。", example = "11")
	var targetElementId: Long = 0,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
