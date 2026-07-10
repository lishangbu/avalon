package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗行动校验响应。
 */
@Schema(description = "战斗行动校验响应。")
data class BattleActionValidationResponse(
	@field:Schema(description = "是否通过行动提交校验。", example = "false")
	val valid: Boolean,
	@field:Schema(description = "违规项列表。")
	val violations: List<BattleActionViolationResponse>,
)
