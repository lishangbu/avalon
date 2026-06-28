package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗赛制条款绑定维护请求。
 */
@Schema(description = "战斗赛制条款绑定维护请求。")
data class BattleFormatClauseBindingRequest(
	@field:Schema(description = "赛制 ID。", example = "3")
	var formatId: Long = 0,
	@field:Schema(description = "条款 ID。", example = "1")
	var clauseId: Long = 0,
	@field:Schema(description = "是否为强制条款。", example = "true")
	var required: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
