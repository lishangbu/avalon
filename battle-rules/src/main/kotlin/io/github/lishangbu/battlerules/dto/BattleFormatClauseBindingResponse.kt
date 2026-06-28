package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗赛制条款绑定维护响应。
 */
@Schema(description = "战斗赛制条款绑定维护响应。")
data class BattleFormatClauseBindingResponse(
	@field:Schema(description = "绑定主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "赛制 ID。", example = "3")
	val formatId: Long,
	@field:Schema(description = "条款 ID。", example = "1")
	val clauseId: Long,
	@field:Schema(description = "是否为强制条款。", example = "true")
	val required: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
