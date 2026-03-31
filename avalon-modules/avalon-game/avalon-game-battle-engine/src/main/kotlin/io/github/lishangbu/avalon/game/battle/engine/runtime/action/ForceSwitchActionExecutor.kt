package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ForceSwitchActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.ForceSwitchMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `force_switch` 动作执行器。
 */
class ForceSwitchActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.FORCE_SWITCH

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is ForceSwitchActionNode) { "Action must be ForceSwitchActionNode." }
        return ActionResult(
            mutations = listOf(ForceSwitchMutation(action.target)),
        )
    }
}
