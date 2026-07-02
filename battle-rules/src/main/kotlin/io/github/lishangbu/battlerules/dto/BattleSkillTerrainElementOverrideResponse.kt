package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能场地属性覆盖维护响应。
 */
@Schema(description = "技能场地属性覆盖维护响应。")
data class BattleSkillTerrainElementOverrideResponse(
	@field:Schema(description = "技能场地属性覆盖主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "技能规则 ID。", example = "100000")
	val skillRuleId: Long,
	@field:Schema(description = "场地规则 ID。", example = "4")
	val terrainRuleId: Long,
	@field:Schema(description = "目标属性 ID，引用游戏属性资料。", example = "14")
	val targetElementId: Long,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
