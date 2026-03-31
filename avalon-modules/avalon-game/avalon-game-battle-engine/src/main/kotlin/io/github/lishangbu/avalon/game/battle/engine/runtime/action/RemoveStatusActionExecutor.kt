package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.RemoveStatusActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.RemoveStatusMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `remove_status` 动作执行器。
 */
class RemoveStatusActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.REMOVE_STATUS

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is RemoveStatusActionNode) { "Action must be RemoveStatusActionNode." }
        return ActionResult(
            mutations = listOf(RemoveStatusMutation(action.target)),
        )
    }
}
