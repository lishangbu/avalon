package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能场地威力倍率维护请求。
 *
 * 倍率会在普通伤害公式的威力阶段生效，并且只在使用者接地且当前场地匹配时应用。
 */
@Schema(description = "技能场地威力倍率维护请求。")
data class BattleSkillTerrainPowerModifierRequest(
	@field:Schema(description = "技能规则 ID。", example = "100000")
	var skillRuleId: Long = 0,
	@field:Schema(description = "场地规则 ID。", example = "4")
	var terrainRuleId: Long = 0,
	@field:Schema(description = "威力倍率，必须大于 0；2 表示威力翻倍。", example = "2.0")
	var powerMultiplier: Double = 1.0,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
