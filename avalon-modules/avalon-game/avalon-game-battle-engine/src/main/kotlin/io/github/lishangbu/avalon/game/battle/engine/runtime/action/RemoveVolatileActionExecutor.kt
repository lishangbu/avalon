package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.RemoveVolatileActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.RemoveVolatileMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `remove_volatile` 动作执行器。
 */
class RemoveVolatileActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.REMOVE_VOLATILE

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is RemoveVolatileActionNode) { "Action must be RemoveVolatileActionNode." }
        return ActionResult(
            mutations = listOf(RemoveVolatileMutation(action.target, action.value)),
        )
    }
}
