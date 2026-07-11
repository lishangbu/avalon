package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/** 为临时会话列表提供状态与淘汰时间，不携带完整运行态和 Turn Record。 */
@Schema(description = "Battle Session 列表中的轻量摘要。")
data class BattleSessionSummaryResponse(
	@field:Schema(description = "Session Identifier。")
	val sessionId: String,
	@field:Schema(description = "赛制稳定 code。")
	val formatCode: String,
	@field:Schema(description = "会话生命周期状态。", allowableValues = ["ACTIVE", "COMPLETED", "TERMINATED"])
	val status: String,
	@field:Schema(description = "当前 revision。")
	val revision: Long,
	@field:Schema(description = "当前已结算回合序号。")
	val turnNumber: Int,
	@field:Schema(description = "创建时间。")
	val createdAt: Instant,
	@field:Schema(description = "最后更新时间。")
	val updatedAt: Instant,
	@field:Schema(description = "进入终态时间。", nullable = true)
	val endedAt: Instant?,
	@field:Schema(description = "Recent Session 预计淘汰时间。", nullable = true)
	val expiresAt: Instant?,
	@field:Schema(description = "引擎确认的战斗结果；未完成时为空。", nullable = true)
	val result: BattleSessionResponse.Result?,
	@field:Schema(description = "外部终止原因；非 TERMINATED 状态时为空。", nullable = true)
	val terminationReason: String?,
)
