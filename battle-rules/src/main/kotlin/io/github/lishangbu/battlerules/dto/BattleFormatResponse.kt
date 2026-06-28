package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗赛制维护响应。
 */
@Schema(description = "战斗赛制维护响应。")
data class BattleFormatResponse(
	@field:Schema(description = "赛制主键 ID。", example = "1")
	val id: Long,
	@field:Schema(description = "赛制稳定 code。", example = "standard-single")
	val code: String,
	@field:Schema(description = "赛制简体中文名称。", example = "标准单打")
	val name: String,
	@field:Schema(description = "赛制说明。", nullable = true)
	val description: String?,
	@field:Schema(description = "站位模式。", example = "SINGLE")
	val battleMode: String,
	@field:Schema(description = "参与玩家数量。", example = "2")
	val playerCount: Int,
	@field:Schema(description = "队伍登记成员数量。", example = "6")
	val teamSize: Int,
	@field:Schema(description = "单方同时上场成员数量。", example = "1")
	val activeParticipantCount: Int,
	@field:Schema(description = "默认拉平等级。", example = "50", nullable = true)
	val defaultLevel: Int?,
	@field:Schema(description = "是否允许叠加自定义规则。", example = "true")
	val allowCustomRules: Boolean,
	@field:Schema(description = "是否启用。", example = "true")
	val enabled: Boolean,
	@field:Schema(description = "展示排序。", example = "10")
	val sortOrder: Int,
)
