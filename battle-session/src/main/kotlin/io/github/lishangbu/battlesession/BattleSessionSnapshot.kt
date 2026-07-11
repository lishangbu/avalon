package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.model.BattleState
import java.time.Instant

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
