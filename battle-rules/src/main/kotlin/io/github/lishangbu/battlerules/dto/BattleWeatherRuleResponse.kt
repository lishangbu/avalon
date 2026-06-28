package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗天气规则维护响应。
 */
@Schema(description = "战斗天气规则维护响应。")
data class BattleWeatherRuleResponse(
	@field:Schema(description = "天气规则主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "天气规则稳定 code。", example = "rain")
	val code: String,
	@field:Schema(description = "天气规则简体中文名称。", example = "下雨")
	val name: String,
	@field:Schema(description = "引擎效果策略编码。", example = "weather-rain")
	val effectPolicy: String,
	@field:Schema(description = "默认持续回合。", example = "5", nullable = true)
	val defaultDurationTurns: Int?,
	@field:Schema(description = "天气规则说明。", nullable = true)
	val description: String?,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
