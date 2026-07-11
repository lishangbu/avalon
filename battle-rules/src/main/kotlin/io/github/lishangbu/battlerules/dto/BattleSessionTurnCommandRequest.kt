package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/** 携带当前全部人工选择，并用 commandId 与 expectedRevision 保证安全重试。 */
@Schema(description = "恰好推进 Battle Session 一次的完整 Turn Command。")
data class BattleSessionTurnCommandRequest(
	@field:Schema(description = "调用方生成的 UUID v4 幂等标识。", requiredMode = Schema.RequiredMode.REQUIRED)
	var commandId: String = "",
	@field:Schema(description = "调用方最后观察到的 Session revision。", requiredMode = Schema.RequiredMode.REQUIRED)
	var expectedRevision: Long = -1,
	@field:Schema(description = "满足当前全部 Turn Requirements 的行动。", requiredMode = Schema.RequiredMode.REQUIRED)
	var actions: List<BattleActionRequest> = emptyList(),
)
