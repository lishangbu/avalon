package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗道具规则维护响应。
 */
@Schema(description = "战斗道具规则维护响应。")
data class BattleItemRuleResponse(
	@field:Schema(description = "道具规则主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "道具 ID，引用基础游戏资料。", example = "211")
	val itemId: Long,
	@field:Schema(description = "触发时机。", example = "HELD_END_TURN")
	val triggerTiming: String,
	@field:Schema(description = "效果策略编码。", example = "leftovers-heal")
	val effectPolicy: String,
	@field:Schema(description = "触发后是否消耗该道具。", example = "false")
	val consumable: Boolean,
	@field:Schema(description = "同一触发时机内的结算顺序。", example = "100")
	val triggerOrder: Int,
	@field:Schema(description = "道具规则说明。", nullable = true)
	val description: String?,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
