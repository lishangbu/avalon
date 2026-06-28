package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗赛制限制维护响应。
 */
@Schema(description = "战斗赛制限制维护响应。")
data class BattleFormatRestrictionResponse(
	@field:Schema(description = "限制主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "赛制 ID。", example = "3")
	val formatId: Long,
	@field:Schema(description = "限制稳定 code。", example = "level-cap-50")
	val code: String,
	@field:Schema(description = "限制简体中文名称。", example = "等级上限")
	val name: String,
	@field:Schema(description = "限制类型。", example = "LEVEL")
	val restrictionType: String,
	@field:Schema(description = "限制判定方式。", example = "MAX")
	val restrictionOperator: String,
	@field:Schema(description = "文本操作数。", nullable = true)
	val operandText: String?,
	@field:Schema(description = "数值操作数。", example = "50", nullable = true)
	val operandNumber: Int?,
	@field:Schema(description = "限制说明。", nullable = true)
	val description: String?,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
