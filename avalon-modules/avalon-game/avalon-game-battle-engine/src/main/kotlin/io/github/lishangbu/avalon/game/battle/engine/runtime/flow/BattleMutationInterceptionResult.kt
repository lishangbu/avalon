package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

/**
 * 单个 mutation 在拦截链中的处理结果。
 *
 * @property snapshot 拦截过程中产生的最新 battle 快照。
 * @property allowed 当前 mutation 是否允许继续提交。
 */
data class BattleMutationInterceptionResult(
    val snapshot: BattleRuntimeSnapshot,
    val allowed: Boolean,
)
