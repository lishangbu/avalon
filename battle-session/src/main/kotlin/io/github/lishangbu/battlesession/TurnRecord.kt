package io.github.lishangbu.battlesession

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleRandomTraceEntry
import java.time.Instant

data class TurnRecord(
	val commandId: String,
	val revisionBefore: Long,
	val revisionAfter: Long,
	val turnNumber: Int,
	val submittedActions: List<BattleAction>,
	val randomTrace: List<BattleRandomTraceEntry>,
	val events: List<BattleEvent>,
	val resolvedAt: Instant,
)
