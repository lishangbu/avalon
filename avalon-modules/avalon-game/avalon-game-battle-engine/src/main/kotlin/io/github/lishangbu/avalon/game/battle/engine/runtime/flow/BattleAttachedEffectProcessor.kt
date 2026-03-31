package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

/**
 * 已挂载 effect 的 hook 处理回调。
 *
 * 设计意图：
 * - 为 mutation interceptor 提供一个窄接口，用于触发目标单位上挂载 effect 的响应式 hook。
 * - 避免 interceptor 与 `DefaultBattleFlowPhaseProcessor` 之间形成循环依赖。
 */
fun interface BattleAttachedEffectProcessor {
    /**
     * 处理某个单位上挂载 effect 的指定 hook。
     *
     * @param snapshot 当前 battle 快照。
     * @param unitId 当前要处理挂载 effect 的单位标识。
     * @param hookName 当前需要执行的 hook 名称。
     * @param targetId 当前 hook 的目标单位标识。
     * @param sourceId 当前 hook 的来源单位标识。
     * @param relay 当前 hook 接收到的 relay 值。
     * @param attributes 当前 hook 的附加属性。
     * @return 本次 attached effect 处理结果。
     */
    fun process(
        snapshot: BattleRuntimeSnapshot,
        unitId: String,
        hookName: String,
        targetId: String?,
        sourceId: String?,
        relay: Any?,
        attributes: Map<String, Any?>,
    ): HookPhaseResult
}
