package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 技能场地属性覆盖维护请求。
 *
 * 属性覆盖只改变本次技能结算属性，不隐含威力倍率；同一技能若还需要场地威力变化，应维护独立的场地威力倍率记录。
 */
@Schema(description = "技能场地属性覆盖维护请求。")
data class BattleSkillTerrainElementOverrideRequest(
	@field:Schema(description = "技能规则 ID。", example = "100000")
	var skillRuleId: Long = 0,
	@field:Schema(description = "场地规则 ID。", example = "4")
	var terrainRuleId: Long = 0,
	@field:Schema(description = "目标属性 ID，引用游戏属性资料。", example = "14")
	var targetElementId: Long = 0,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
