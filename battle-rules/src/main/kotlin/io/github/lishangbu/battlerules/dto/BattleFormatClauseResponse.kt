package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗赛制条款维护响应。
 */
@Schema(description = "战斗赛制条款维护响应。")
data class BattleFormatClauseResponse(
	@field:Schema(description = "条款主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "条款稳定 code。", example = "species-unique")
	val code: String,
	@field:Schema(description = "条款简体中文名称。", example = "种类唯一")
	val name: String,
	@field:Schema(description = "条款类型。", example = "TEAM")
	val clauseType: String,
	@field:Schema(description = "条款说明。", nullable = true)
	val description: String?,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
