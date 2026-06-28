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

/**
 * 战斗行动校验违规项。
 */
@Schema(description = "战斗行动校验违规项。")
data class BattleActionViolationResponse(
	@field:Schema(description = "稳定违规 code。", example = "skill-not-found")
	val code: String,
	@field:Schema(description = "行动成员 actorId。", example = "side-a-1")
	val actorId: String,
	@field:Schema(description = "目标成员 actorId。", nullable = true, example = "side-b-1")
	val targetActorId: String?,
	@field:Schema(description = "触发规则的资料 ID。", nullable = true, example = "1")
	val resourceId: Long?,
	@field:Schema(description = "简体中文说明。")
	val message: String,
)
