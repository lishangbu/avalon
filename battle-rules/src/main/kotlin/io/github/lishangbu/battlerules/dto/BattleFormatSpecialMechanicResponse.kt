package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗赛制特殊机制绑定维护响应。
 */
@Schema(description = "战斗赛制特殊机制绑定维护响应。")
data class BattleFormatSpecialMechanicResponse(
	@field:Schema(description = "绑定主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "赛制 ID。", example = "3")
	val formatId: Long,
	@field:Schema(description = "特殊机制 ID。", example = "1")
	val mechanicId: Long,
	@field:Schema(description = "该赛制是否启用该特殊机制。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
