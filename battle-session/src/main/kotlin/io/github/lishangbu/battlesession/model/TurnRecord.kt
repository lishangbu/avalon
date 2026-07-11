package io.github.lishangbu.battlesession.model

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleRandomTraceEntry
import java.time.Instant

/**
 * 保存一个成功回合的有序行动、随机轨迹、事件增量与结算时间。
 *
 * 历史 Snapshot 由初始状态和记录前缀重放，不在每个记录中重复保存。
 */
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
