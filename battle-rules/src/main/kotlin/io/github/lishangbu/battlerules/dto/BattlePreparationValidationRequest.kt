package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗准备阶段校验请求。
 */
@Schema(description = "战斗准备阶段校验请求。")
data class BattlePreparationValidationRequest(
	@field:Schema(description = "赛制稳定 code。", example = "official-double")
	var formatCode: String = "",
	@field:Schema(description = "双方队伍快照。")
	var sides: List<BattlePreparationSideRequest> = emptyList(),
)
