package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.RemoveConditionActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.RemoveConditionMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `remove_condition` 动作执行器。
 */
class RemoveConditionActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.REMOVE_CONDITION

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is RemoveConditionActionNode) { "Action must be RemoveConditionActionNode." }
        return ActionResult(
            mutations = listOf(RemoveConditionMutation(action.target, action.value)),
        )
    }
}
