package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗首回合行动校验请求。
 */
@Schema(description = "战斗首回合行动校验请求。")
data class BattleActionValidationRequest(
	@field:Schema(description = "赛制稳定 code。", example = "official-double")
	var formatCode: String = "",
	@field:Schema(description = "双方队伍快照。")
	var sides: List<BattlePreparationSideRequest> = emptyList(),
	@field:Schema(description = "本回合提交行动。")
	var actions: List<BattleActionRequest> = emptyList(),
)
