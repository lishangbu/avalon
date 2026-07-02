package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能场地威力倍率维护响应。
 */
@Schema(description = "技能场地威力倍率维护响应。")
data class BattleSkillTerrainPowerModifierResponse(
	@field:Schema(description = "技能场地威力倍率主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "技能规则 ID。", example = "100000")
	val skillRuleId: Long,
	@field:Schema(description = "场地规则 ID。", example = "4")
	val terrainRuleId: Long,
	@field:Schema(description = "威力倍率，必须大于 0；2 表示威力翻倍。", example = "2.0")
	val powerMultiplier: Double,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
