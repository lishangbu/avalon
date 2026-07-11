package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.model.BattleReplay
import java.time.Instant

data class BattleRecord(
	val sessionId: String,
	val status: BattleSessionStatus,
	val replay: BattleReplay,
	val termination: SessionTermination?,
	val startedAt: Instant,
	val endedAt: Instant,
) {
	init {
		require(status != BattleSessionStatus.ACTIVE) { "Battle Record requires a terminal session" }
		require((status == BattleSessionStatus.TERMINATED) == (termination != null)) {
			"termination must be present exactly for a terminated session"
		}
	}
}
