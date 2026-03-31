package io.github.lishangbu.avalon.game.battle.engine.session

import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot

/**
 * BattleSession 可持久化状态。
 */
data class BattleSessionState(
    val snapshot: BattleRuntimeSnapshot,
    val pendingActions: List<BattleSessionAction> = emptyList(),
    val replacementRequests: List<BattleSessionReplacementRequest> = emptyList(),
    val resourceLedger: List<BattleSessionResourceUsage> = emptyList(),
    val battleLogs: List<String> = emptyList(),
    val eventLogs: List<BattleSessionEvent> = emptyList(),
) {
    /**
     * 结构化事件 payload 的强类型视图。
     */
    val eventPayloads: List<BattleSessionEventPayload>
        get() = eventLogs.map(BattleSessionEvent::payload)

    /**
     * 捕捉资源账本的强类型视图。
     */
    val captureResourceLedger: List<BattleSessionCaptureResourceUsage>
        get() = resourceLedger.filterIsInstance<BattleSessionCaptureResourceUsage>()
}
