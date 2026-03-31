package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ClearBoostsActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.ClearBoostsMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `clear_boosts` 动作执行器。
 */
class ClearBoostsActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.CLEAR_BOOSTS

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is ClearBoostsActionNode) { "Action must be ClearBoostsActionNode." }
        return ActionResult(
            mutations = listOf(ClearBoostsMutation(action.target)),
        )
    }
}
