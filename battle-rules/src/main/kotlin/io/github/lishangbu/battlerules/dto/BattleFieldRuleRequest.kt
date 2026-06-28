package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗场上效果规则维护请求。
 */
@Schema(description = "战斗场上效果规则维护请求。")
data class BattleFieldRuleRequest(
	@field:Schema(description = "场上效果稳定 code。", example = "reflect")
	var code: String = "",
	@field:Schema(description = "场上效果简体中文名称。", example = "反射壁")
	var name: String = "",
	@field:Schema(description = "效果范围。", example = "SIDE")
	var effectScope: String = "",
	@field:Schema(description = "引擎效果策略编码。", example = "side-reflect")
	var effectPolicy: String = "",
	@field:Schema(description = "最少持续回合。", example = "5", nullable = true)
	var minTurns: Int? = null,
	@field:Schema(description = "最多持续回合。", example = "8", nullable = true)
	var maxTurns: Int? = null,
	@field:Schema(description = "可叠加层数上限。", example = "3", nullable = true)
	var maxLayers: Int? = null,
	@field:Schema(description = "场上效果说明。", nullable = true)
	var description: String? = null,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
