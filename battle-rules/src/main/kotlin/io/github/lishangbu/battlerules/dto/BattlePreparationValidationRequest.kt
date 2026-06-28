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

/**
 * 战斗准备阶段校验的一方队伍。
 */
@Schema(description = "战斗准备阶段校验的一方队伍。")
data class BattlePreparationSideRequest(
	@field:Schema(description = "队伍侧 ID。", example = "side-a")
	var sideId: String = "",
	@field:Schema(description = "当前选择上场的成员 actorId。")
	var activeActorIds: List<String> = emptyList(),
	@field:Schema(description = "登记成员。")
	var participants: List<BattlePreparationParticipantRequest> = emptyList(),
)

/**
 * 战斗准备阶段校验的成员快照。
 */
@Schema(description = "战斗准备阶段校验的成员快照。")
data class BattlePreparationParticipantRequest(
	@field:Schema(description = "战斗内成员 ID。", example = "side-a-1")
	var actorId: String = "",
	@field:Schema(description = "成员种类资料 ID。", example = "1")
	var creatureId: Long = 0,
	@field:Schema(description = "成员等级。", example = "50")
	var level: Int = 50,
	@field:Schema(description = "技能资料 ID 列表。")
	var skillIds: List<Long> = emptyList(),
	@field:Schema(description = "特性资料 ID。", nullable = true)
	var abilityId: Long? = null,
	@field:Schema(description = "道具资料 ID。", nullable = true)
	var itemId: Long? = null,
)
