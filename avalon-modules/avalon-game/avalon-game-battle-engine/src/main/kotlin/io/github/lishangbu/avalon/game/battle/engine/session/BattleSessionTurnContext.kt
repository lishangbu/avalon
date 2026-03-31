package io.github.lishangbu.avalon.game.battle.engine.session

import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot

/**
 * 回合推进 pipeline 在单次执行中的共享上下文。
 *
 * @property session 当前正在推进的 battle session。
 * @property actionResults 本回合已经执行完成的 action 结果列表。
 * @property snapshot pipeline 当前阶段持有的最新快照。
 */
class BattleSessionTurnContext(
    val session: BattleSession,
) {
    /**
     * 本回合已经执行完成的 action 结果列表。
     */
    var actionResults: List<BattleSessionActionExecutionResult> = emptyList()

    /**
     * pipeline 当前阶段持有的最新 battle snapshot。
     */
    var snapshot: BattleRuntimeSnapshot = session.currentSnapshot
}
