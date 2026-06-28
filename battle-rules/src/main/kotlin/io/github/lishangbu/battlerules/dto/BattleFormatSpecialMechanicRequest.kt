package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗赛制特殊机制绑定维护请求。
 */
@Schema(description = "战斗赛制特殊机制绑定维护请求。")
data class BattleFormatSpecialMechanicRequest(
	@field:Schema(description = "赛制 ID。", example = "3")
	var formatId: Long = 0,
	@field:Schema(description = "特殊机制 ID。", example = "1")
	var mechanicId: Long = 0,
	@field:Schema(description = "该赛制是否启用该特殊机制。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
