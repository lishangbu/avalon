package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ApplyConditionActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.ApplyConditionMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `apply_condition` 动作执行器。
 */
class ApplyConditionActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.APPLY_CONDITION

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is ApplyConditionActionNode) { "Action must be ApplyConditionActionNode." }
        return ActionResult(
            mutations = listOf(ApplyConditionMutation(action.target, action.value)),
        )
    }
}
