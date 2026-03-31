package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.BoostActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.BoostMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `boost` 动作执行器。
 */
class BoostActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.BOOST

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is BoostActionNode) { "Action must be BoostActionNode." }
        return ActionResult(
            mutations = listOf(BoostMutation(action.target, action.stats)),
        )
    }
}
