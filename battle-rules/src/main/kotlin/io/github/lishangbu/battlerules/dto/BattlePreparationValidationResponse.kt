package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗准备阶段校验响应。
 */
@Schema(description = "战斗准备阶段校验响应。")
data class BattlePreparationValidationResponse(
	@field:Schema(description = "是否通过准备阶段规则校验。", example = "false")
	val valid: Boolean,
	@field:Schema(description = "违规项列表。")
	val violations: List<BattlePreparationViolationResponse>,
)
