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

/**
 * 战斗准备阶段校验违规项。
 */
@Schema(description = "战斗准备阶段校验违规项。")
data class BattlePreparationViolationResponse(
	@field:Schema(description = "稳定违规 code。", example = "level-too-high")
	val code: String,
	@field:Schema(description = "队伍侧 ID。", example = "side-a")
	val sideId: String,
	@field:Schema(description = "成员 actorId。", example = "side-a-1")
	val actorId: String,
	@field:Schema(description = "触发规则的资料 ID。", example = "1")
	val resourceId: Long,
	@field:Schema(description = "简体中文说明。")
	val message: String,
)
