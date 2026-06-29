package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能天气属性覆盖维护响应。
 */
@Schema(description = "技能天气属性覆盖维护响应。")
data class BattleSkillWeatherElementOverrideResponse(
	@field:Schema(description = "技能天气属性覆盖主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "技能规则 ID。", example = "13")
	val skillRuleId: Long,
	@field:Schema(description = "天气规则 ID。", example = "3")
	val weatherRuleId: Long,
	@field:Schema(description = "目标属性 ID。", example = "11")
	val targetElementId: Long,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
