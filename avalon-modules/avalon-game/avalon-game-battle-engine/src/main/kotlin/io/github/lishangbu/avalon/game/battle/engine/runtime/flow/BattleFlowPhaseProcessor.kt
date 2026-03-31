package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.dsl.EffectDefinition

/**
 * battle hook phase 处理器。
 *
 * 设计意图：
 * - 为 move resolution pipeline 和 mutation interceptor 提供统一的 hook 执行入口。
 * - 把 effect 自身 hook、挂载 effect hook、mutation 写回等细节集中管理。
 */
interface BattleFlowPhaseProcessor {
    /**
     * 处理一次完整的 hook phase。
     *
     * @param snapshot 当前 battle 快照。
     * @param hookName 当前要执行的 hook 名称。
     * @param moveEffect 当前正在结算的 effect 定义。
     * @param selfId 当前 hook 的 self 单位标识。
     * @param targetId 当前 hook 的目标单位标识。
     * @param sourceId 当前 hook 的来源单位标识。
     * @param relay 当前 hook 的输入 relay。
     * @param attributes 当前 hook 的附加属性。
     * @return 本次 hook phase 的处理结果。
     */
    fun processPhase(
        snapshot: BattleRuntimeSnapshot,
        hookName: String,
        moveEffect: EffectDefinition,
        selfId: String,
        targetId: String,
        sourceId: String,
        relay: Any?,
        attributes: Map<String, Any?>,
    ): HookPhaseResult

    /**
     * 处理某个单位上挂载 effect 的指定 hook。
     *
     * @param snapshot 当前 battle 快照。
     * @param unitId 当前要处理挂载 effect 的单位标识。
     * @param hookName 当前要执行的 hook 名称。
     * @param targetId 当前 hook 的目标单位标识。
     * @param sourceId 当前 hook 的来源单位标识。
     * @param relay 当前 hook 的输入 relay。
     * @param attributes 当前 hook 的附加属性。
     * @return 本次 attached effect hook 的处理结果。
     */
    fun processAttachedEffects(
        snapshot: BattleRuntimeSnapshot,
        unitId: String,
        hookName: String,
        targetId: String?,
        sourceId: String?,
        relay: Any?,
        attributes: Map<String, Any?>,
    ): HookPhaseResult
}
