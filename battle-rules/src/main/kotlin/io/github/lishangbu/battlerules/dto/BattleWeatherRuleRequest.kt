package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗天气规则维护请求。
 */
@Schema(description = "战斗天气规则维护请求。")
data class BattleWeatherRuleRequest(
	@field:Schema(description = "天气规则稳定 code。", example = "rain")
	var code: String = "",
	@field:Schema(description = "天气规则简体中文名称。", example = "下雨")
	var name: String = "",
	@field:Schema(description = "引擎效果策略编码。", example = "weather-rain")
	var effectPolicy: String = "",
	@field:Schema(description = "默认持续回合。", example = "5", nullable = true)
	var defaultDurationTurns: Int? = null,
	@field:Schema(description = "天气规则说明。", nullable = true)
	var description: String? = null,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
