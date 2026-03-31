package io.github.lishangbu.avalon.game.battle.engine.session

import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.MoveResolutionResult

/**
 * session 队列中单个行动的执行结果。
 *
 * @property action 被执行的原始行动。
 * @property snapshot 执行该行动后的最新快照。
 * @property moveResult 如果该行动是 move，则附带其主流程结果。
 */
data class BattleSessionActionExecutionResult(
    val action: BattleSessionAction,
    val snapshot: BattleRuntimeSnapshot,
    val moveResult: MoveResolutionResult? = null,
    val captureResult: BattleSessionCaptureResult? = null,
)
