package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗场地规则维护请求。
 */
@Schema(description = "战斗场地规则维护请求。")
data class BattleTerrainRuleRequest(
	@field:Schema(description = "场地规则稳定 code。", example = "electric-terrain")
	var code: String = "",
	@field:Schema(description = "场地规则简体中文名称。", example = "电气场地")
	var name: String = "",
	@field:Schema(description = "引擎效果策略编码。", example = "terrain-electric")
	var effectPolicy: String = "",
	@field:Schema(description = "默认持续回合。", example = "5", nullable = true)
	var defaultDurationTurns: Int? = null,
	@field:Schema(description = "场地规则说明。", nullable = true)
	var description: String? = null,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
