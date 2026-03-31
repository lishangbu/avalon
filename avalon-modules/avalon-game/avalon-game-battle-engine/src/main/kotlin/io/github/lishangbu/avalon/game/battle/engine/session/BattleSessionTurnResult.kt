package io.github.lishangbu.avalon.game.battle.engine.session

import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.MoveResolutionResult

/**
 * BattleSession 一次整回合推进结果。
 *
 * @property actionResults 队列中每个行动的结算结果，顺序与队列排序规则一致。
 * @property snapshot 执行队列并完成回合结束后的最新快照。
 */
data class BattleSessionTurnResult(
    val actionResults: List<BattleSessionActionExecutionResult>,
    val snapshot: BattleRuntimeSnapshot,
)
