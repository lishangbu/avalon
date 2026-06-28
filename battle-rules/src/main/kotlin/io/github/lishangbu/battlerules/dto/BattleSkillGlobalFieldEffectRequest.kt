package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能全场效果维护请求。
 */
@Schema(description = "技能全场效果维护请求。")
data class BattleSkillGlobalFieldEffectRequest(
	@field:Schema(description = "技能规则 ID。", example = "18")
	var skillRuleId: Long = 0,
	@field:Schema(description = "全场效果规则 ID。", example = "5")
	var fieldRuleId: Long = 0,
	@field:Schema(description = "效果结算时机。", example = "AFTER_HIT")
	var effectTiming: String = "",
	@field:Schema(description = "要求存在的天气规则 ID；为空表示无天气前置条件。", example = "5")
	var requiredWeatherRuleId: Long? = null,
	@field:Schema(description = "触发概率百分比。", example = "100")
	var chancePercent: Int = 100,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
