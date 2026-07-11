package io.github.lishangbu.battlesession.model

import io.github.lishangbu.battleengine.model.BattleReplay
import java.time.Instant

/**
 * 保存终态会话的不可变诊断与复盘材料。
 *
 * 该记录随 Recent Session 保留，不承担 Runtime 恢复或永久 Match 历史职责。
 */
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
