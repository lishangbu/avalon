package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗道具规则维护请求。
 */
@Schema(description = "战斗道具规则维护请求。")
data class BattleItemRuleRequest(
	@field:Schema(description = "道具 ID，引用基础游戏资料。", example = "211")
	var itemId: Long = 0,
	@field:Schema(description = "触发时机。", example = "HELD_END_TURN")
	var triggerTiming: String = "",
	@field:Schema(description = "效果策略编码。", example = "leftovers-heal")
	var effectPolicy: String = "",
	@field:Schema(description = "触发后是否消耗该道具。", example = "false")
	var consumable: Boolean = false,
	@field:Schema(description = "同一触发时机内的结算顺序。", example = "100")
	var triggerOrder: Int = 100,
	@field:Schema(description = "道具规则说明。", nullable = true)
	var description: String? = null,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
