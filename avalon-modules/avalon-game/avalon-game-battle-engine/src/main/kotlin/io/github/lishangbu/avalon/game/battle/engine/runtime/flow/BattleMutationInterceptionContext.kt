package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.mutation.BattleMutation

/**
 * 单个 mutation 在拦截链中的共享上下文。
 *
 * @property snapshot 当前拦截阶段持有的 battle 快照。
 * @property selfId 当前 hook 的 self 单位标识。
 * @property targetId 当前 hook 的目标单位标识。
 * @property sourceId 当前 hook 的来源单位标识。
 * @property mutation 当前正在判断的 mutation。
 */
data class BattleMutationInterceptionContext(
    val snapshot: BattleRuntimeSnapshot,
    val selfId: String?,
    val targetId: String?,
    val sourceId: String?,
    val mutation: BattleMutation,
)
