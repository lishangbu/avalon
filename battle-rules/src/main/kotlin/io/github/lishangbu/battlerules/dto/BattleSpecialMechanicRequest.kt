package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗特殊机制维护请求。
 */
@Schema(description = "战斗特殊机制维护请求。")
data class BattleSpecialMechanicRequest(
	@field:Schema(description = "特殊机制稳定 code。", example = "temporary-type-boost")
	var code: String = "",
	@field:Schema(description = "特殊机制简体中文名称。", example = "临时属性强化")
	var name: String = "",
	@field:Schema(description = "特殊机制说明。", nullable = true)
	var description: String? = null,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
