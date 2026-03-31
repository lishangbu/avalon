package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.SetFlagActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.SetFlagMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `set_flag` 动作执行器。
 */
class SetFlagActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.SET_FLAG

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is SetFlagActionNode) { "Action must be SetFlagActionNode." }
        return ActionResult(
            mutations = listOf(SetFlagMutation(action.target, action.key, action.value)),
        )
    }
}
