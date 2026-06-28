package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗赛制限制维护请求。
 */
@Schema(description = "战斗赛制限制维护请求。")
data class BattleFormatRestrictionRequest(
	@field:Schema(description = "赛制 ID。", example = "3")
	var formatId: Long = 0,
	@field:Schema(description = "限制稳定 code。", example = "level-cap-50")
	var code: String = "",
	@field:Schema(description = "限制简体中文名称。", example = "等级上限")
	var name: String = "",
	@field:Schema(description = "限制类型。", example = "LEVEL")
	var restrictionType: String = "",
	@field:Schema(description = "限制判定方式。", example = "MAX")
	var restrictionOperator: String = "",
	@field:Schema(description = "文本操作数。", nullable = true)
	var operandText: String? = null,
	@field:Schema(description = "数值操作数。", example = "50", nullable = true)
	var operandNumber: Int? = null,
	@field:Schema(description = "限制说明。", nullable = true)
	var description: String? = null,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
