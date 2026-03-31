package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.AddVolatileActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.AddVolatileMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `add_volatile` 动作执行器。
 */
class AddVolatileActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.ADD_VOLATILE

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is AddVolatileActionNode) { "Action must be AddVolatileActionNode." }
        return ActionResult(
            mutations = listOf(AddVolatileMutation(action.target, action.value)),
        )
    }
}
