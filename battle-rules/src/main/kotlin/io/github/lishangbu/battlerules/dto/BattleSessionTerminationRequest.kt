package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/** 幂等终止 ACTIVE Battle Session 的命令。 */
@Schema(description = "幂等终止 ACTIVE Battle Session 的命令。")
data class BattleSessionTerminationRequest(
	@field:Schema(description = "调用方生成的 UUID v4 幂等标识。", requiredMode = Schema.RequiredMode.REQUIRED)
	var commandId: String = "",
	@field:Schema(description = "调用方最后观察到的 Session revision。", requiredMode = Schema.RequiredMode.REQUIRED)
	var expectedRevision: Long = -1,
	@field:Schema(description = "可审计的外部终止原因。", requiredMode = Schema.RequiredMode.REQUIRED)
	var reason: String = "",
)
