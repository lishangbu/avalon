package io.github.lishangbu.avalon.game.battle.engine.session

import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot

/**
 * BattleSession 对外查询结果。
 *
 * 设计意图：
 * - 把 session 当前对外可见的状态收拢成稳定 DTO。
 * - 避免调用方直接分别拉取 snapshot、pending actions、replacement requests、logs。
 *
 * @property snapshot 当前 battle 快照。
 * @property pendingActions 当前待执行行动列表。
 * @property choiceStatuses 当前回合各 side 的输入状态。
 * @property replacementRequests 当前待处理替补请求。
 * @property resourceLedger 当前全部资源账本条目。
 * @property battleLogs 人类可读 battle log。
 * @property eventLogs 结构化事件日志。
 */
data class BattleSessionQuery(
    val snapshot: BattleRuntimeSnapshot,
    val pendingActions: List<BattleSessionAction>,
    val choiceStatuses: List<BattleSessionChoiceStatus>,
    val replacementRequests: List<BattleSessionReplacementRequest>,
    val resourceLedger: List<BattleSessionResourceUsage>,
    val battleLogs: List<String>,
    val eventLogs: List<BattleSessionEvent>,
) {
    /**
     * 结构化事件的强类型 payload 视图。
     *
     * 设计意图：
     * - 让调用方不必再通过 `event.type + event.attributes` 手工恢复事件语义。
     * - 保持 `eventLogs` 作为完整事件壳的同时，提供更贴近业务的 typed 访问入口。
     */
    val eventPayloads: List<BattleSessionEventPayload>
        get() = eventLogs.map(BattleSessionEvent::payload)

    /**
     * 已入队动作事件的强类型视图。
     */
    val queuedActionEvents: List<BattleSessionQueuedActionPayload>
        get() = eventPayloads.filterIsInstance<BattleSessionQueuedActionPayload>()

    /**
     * 已执行动作事件的强类型视图。
     */
    val executedActionEvents: List<BattleSessionExecutedActionPayload>
        get() = eventPayloads.filterIsInstance<BattleSessionExecutedActionPayload>()

    /**
     * 捕捉资源账本的强类型视图。
     *
     * 设计意图：
     * - 让结算与投影逻辑可以直接消费捕捉账本，而不是再从泛型账本列表中手动筛选。
     */
    val captureResourceLedger: List<BattleSessionCaptureResourceUsage>
        get() = resourceLedger.filterIsInstance<BattleSessionCaptureResourceUsage>()
}
