package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ClearFlagActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.ClearFlagMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `clear_flag` 动作执行器。
 */
class ClearFlagActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.CLEAR_FLAG

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is ClearFlagActionNode) { "Action must be ClearFlagActionNode." }
        return ActionResult(
            mutations = listOf(ClearFlagMutation(action.target, action.key)),
        )
    }
}
