package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Session Roster 中的一方阵容。
 */
@Schema(description = "Battle Session 的一方阵容配置。")
data class BattleSessionRosterSideRequest(
	@field:Schema(
		description = "初始上场成员在 participants 中的零基索引。",
		example = "[0,1]",
		requiredMode = Schema.RequiredMode.REQUIRED,
	)
	var activeParticipantIndexes: List<Int> = emptyList(),
	@field:Schema(description = "按固定顺序排列的参战成员。", requiredMode = Schema.RequiredMode.REQUIRED)
	var participants: List<BattleSessionRosterParticipantRequest> = emptyList(),
)
