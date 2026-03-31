package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.FailMoveActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `fail_move` 动作执行器。
 */
class FailMoveActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.FAIL_MOVE

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is FailMoveActionNode) { "Action must be FailMoveActionNode." }
        return ActionResult(cancelled = true)
    }
}
