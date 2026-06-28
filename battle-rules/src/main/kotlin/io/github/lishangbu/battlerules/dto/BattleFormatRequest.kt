package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗赛制维护请求。
 */
@Schema(description = "战斗赛制维护请求。")
data class BattleFormatRequest(
	@field:Schema(description = "赛制稳定 code。", example = "standard-single")
	var code: String = "",
	@field:Schema(description = "赛制简体中文名称。", example = "标准单打")
	var name: String = "",
	@field:Schema(description = "赛制说明。", nullable = true)
	var description: String? = null,
	@field:Schema(description = "站位模式。", example = "SINGLE")
	var battleMode: String = "",
	@field:Schema(description = "参与玩家数量。", example = "2")
	var playerCount: Int = 2,
	@field:Schema(description = "队伍登记成员数量。", example = "6")
	var teamSize: Int = 6,
	@field:Schema(description = "单方同时上场成员数量。", example = "1")
	var activeParticipantCount: Int = 1,
	@field:Schema(description = "默认拉平等级，空值表示不拉平。", example = "50", nullable = true)
	var defaultLevel: Int? = null,
	@field:Schema(description = "是否允许叠加自定义规则。", example = "true")
	var allowCustomRules: Boolean = false,
	@field:Schema(description = "是否启用。", example = "true")
	var enabled: Boolean = true,
	@field:Schema(description = "展示排序。", example = "10")
	var sortOrder: Int = 0,
)
