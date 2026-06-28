package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗赛制条款维护请求。
 */
@Schema(description = "战斗赛制条款维护请求。")
data class BattleFormatClauseRequest(
	@field:Schema(description = "条款稳定 code。", example = "species-unique")
	var code: String = "",
	@field:Schema(description = "条款简体中文名称。", example = "种类唯一")
	var name: String = "",
	@field:Schema(description = "条款类型。", example = "TEAM")
	var clauseType: String = "",
	@field:Schema(description = "条款说明。", nullable = true)
	var description: String? = null,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
