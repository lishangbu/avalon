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

/**
 * 战斗行动请求。
 */
@Schema(description = "战斗行动请求。")
data class BattleActionRequest(
	@field:Schema(description = "行动类型：USE_SKILL 或 SWITCH_PARTICIPANT。", example = "USE_SKILL")
	var type: String = "",
	@field:Schema(description = "行动成员 actorId。", example = "side-a-1")
	var actorId: String = "",
	@field:Schema(description = "技能资料 ID；使用技能时必填。", nullable = true, example = "1")
	var skillId: Long? = null,
	@field:Schema(description = "目标成员 actorId 或替换目标 actorId。", example = "side-b-1")
	var targetActorId: String = "",
)
