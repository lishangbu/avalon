package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.mutation.BattleMutation

/**
 * battle mutation 拦截链。
 */
interface BattleMutationInterceptorChain {
    /**
     * 过滤一组待提交的 mutation。
     *
     * @param snapshot 当前 battle 快照。
     * @param selfId 当前 hook 的 self 单位标识。
     * @param targetId 当前 hook 的目标单位标识。
     * @param sourceId 当前 hook 的来源单位标识。
     * @param mutations 本次待提交的 mutation 列表。
     * @param attachedEffectProcessor 挂载 effect 的 hook 处理回调。
     * @return mutation 过滤后的结果对象。
     */
    fun filter(
        snapshot: BattleRuntimeSnapshot,
        selfId: String?,
        targetId: String?,
        sourceId: String?,
        mutations: List<BattleMutation>,
        attachedEffectProcessor: BattleAttachedEffectProcessor,
    ): MutationFilteringResult
}
