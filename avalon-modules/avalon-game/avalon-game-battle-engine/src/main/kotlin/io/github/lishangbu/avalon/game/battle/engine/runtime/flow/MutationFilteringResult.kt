package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.mutation.BattleMutation

/**
 * mutation 过滤阶段结果。
 *
 * @property snapshot 过滤过程中可能被前置 hook 修改后的快照。
 * @property mutations 通过拦截检查后允许提交的 mutation。
 */
data class MutationFilteringResult(
    val snapshot: BattleRuntimeSnapshot,
    val mutations: List<BattleMutation>,
)
