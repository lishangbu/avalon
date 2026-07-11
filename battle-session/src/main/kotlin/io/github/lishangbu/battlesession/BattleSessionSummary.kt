package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.model.BattleResult
import java.time.Instant

data class BattleSessionSummary(
	val sessionId: String,
	val formatCode: String,
	val status: BattleSessionStatus,
	val revision: Long,
	val turnNumber: Int,
	val createdAt: Instant,
	val updatedAt: Instant,
	val endedAt: Instant?,
	val expiresAt: Instant?,
	val result: BattleResult?,
	val terminationReason: String?,
) {
	companion object {
		internal fun from(session: BattleSessionSnapshot): BattleSessionSummary =
			BattleSessionSummary(
				sessionId = session.sessionId,
				formatCode = session.formatCode,
				status = session.status,
				revision = session.revision,
				turnNumber = session.state.turnNumber,
				createdAt = session.createdAt,
				updatedAt = session.updatedAt,
				endedAt = session.endedAt,
				expiresAt = session.expiresAt,
				result = session.state.result,
				terminationReason = session.battleRecord?.termination?.reason,
			)
	}
}
