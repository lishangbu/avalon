package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗特殊机制维护响应。
 */
@Schema(description = "战斗特殊机制维护响应。")
data class BattleSpecialMechanicResponse(
	@field:Schema(description = "特殊机制主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "特殊机制稳定 code。", example = "temporary-type-boost")
	val code: String,
	@field:Schema(description = "特殊机制简体中文名称。", example = "临时属性强化")
	val name: String,
	@field:Schema(description = "特殊机制说明。", nullable = true)
	val description: String?,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
