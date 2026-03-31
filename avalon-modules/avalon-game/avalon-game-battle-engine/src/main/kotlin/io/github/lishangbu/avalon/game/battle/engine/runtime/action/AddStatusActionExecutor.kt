package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.AddStatusActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.SetStatusMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `add_status` 动作执行器。
 */
class AddStatusActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.ADD_STATUS

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is AddStatusActionNode) { "Action must be AddStatusActionNode." }
        return ActionResult(
            mutations = listOf(SetStatusMutation(action.target, action.value)),
        )
    }
}
