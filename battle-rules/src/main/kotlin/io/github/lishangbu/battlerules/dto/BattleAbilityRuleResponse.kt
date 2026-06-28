package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗特性规则维护响应。
 */
@Schema(description = "战斗特性规则维护响应。")
data class BattleAbilityRuleResponse(
	@field:Schema(description = "特性规则主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "特性 ID，引用基础游戏资料。", example = "65")
	val abilityId: Long,
	@field:Schema(description = "触发时机。", example = "BEFORE_DAMAGE")
	val triggerTiming: String,
	@field:Schema(description = "效果策略编码。", example = "low-hp-grass-boost")
	val effectPolicy: String,
	@field:Schema(description = "同一触发时机内的结算顺序。", example = "100")
	val triggerOrder: Int,
	@field:Schema(description = "特性规则说明。", nullable = true)
	val description: String?,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
