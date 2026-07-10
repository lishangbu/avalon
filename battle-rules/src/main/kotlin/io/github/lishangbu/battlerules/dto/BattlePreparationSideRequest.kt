package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗准备阶段校验的一方队伍。
 */
@Schema(description = "战斗准备阶段校验的一方队伍。")
data class BattlePreparationSideRequest(
	@field:Schema(description = "队伍侧 ID。", example = "side-a")
	var sideId: String = "",
	@field:Schema(description = "当前选择上场的成员 actorId。")
	var activeActorIds: List<String> = emptyList(),
	@field:Schema(description = "本场参战成员。队伍预览赛制只传已选择参战的成员，不传完整登记名单。")
	var participants: List<BattlePreparationParticipantRequest> = emptyList(),
)
