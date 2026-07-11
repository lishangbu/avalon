package io.github.lishangbu.battlesession.model

import io.github.lishangbu.battleengine.model.BattleState
import java.time.Instant

/**
 * 表达一个 Battle Session 当前可观察的权威状态。
 *
 * Snapshot 不持有玩家归属或持久化身份；revision 只随成功命令单调递增。
 */
data class BattleSessionSnapshot(
	val sessionId: String,
	val formatCode: String,
	val status: BattleSessionStatus,
	val revision: Long,
	val state: BattleState,
	val requirements: TurnRequirements,
	val turnRecords: List<TurnRecord> = emptyList(),
	val createdAt: Instant,
	val updatedAt: Instant,
	val endedAt: Instant? = null,
	val expiresAt: Instant? = null,
	val battleRecord: BattleRecord? = null,
)
