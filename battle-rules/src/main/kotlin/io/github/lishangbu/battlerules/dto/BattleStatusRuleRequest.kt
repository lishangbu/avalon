package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗状态规则维护请求。
 */
@Schema(description = "战斗状态规则维护请求。")
data class BattleStatusRuleRequest(
	@field:Schema(description = "状态规则稳定 code。", example = "burn")
	var code: String = "",
	@field:Schema(description = "状态规则简体中文名称。", example = "灼伤")
	var name: String = "",
	@field:Schema(description = "状态类型。", example = "MAJOR")
	var statusKind: String = "",
	@field:Schema(description = "引擎效果策略编码。", example = "major-burn")
	var effectPolicy: String = "",
	@field:Schema(description = "最少持续回合。", example = "1", nullable = true)
	var minTurns: Int? = null,
	@field:Schema(description = "最多持续回合。", example = "3", nullable = true)
	var maxTurns: Int? = null,
	@field:Schema(description = "状态规则说明。", nullable = true)
	var description: String? = null,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
