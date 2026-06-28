package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能全场效果维护响应。
 */
@Schema(description = "技能全场效果维护响应。")
data class BattleSkillGlobalFieldEffectResponse(
	@field:Schema(description = "技能全场效果主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "技能规则 ID。", example = "18")
	val skillRuleId: Long,
	@field:Schema(description = "全场效果规则 ID。", example = "5")
	val fieldRuleId: Long,
	@field:Schema(description = "效果结算时机。", example = "AFTER_HIT")
	val effectTiming: String,
	@field:Schema(description = "要求存在的天气规则 ID；为空表示无天气前置条件。", example = "5")
	val requiredWeatherRuleId: Long?,
	@field:Schema(description = "触发概率百分比。", example = "100")
	val chancePercent: Int,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
