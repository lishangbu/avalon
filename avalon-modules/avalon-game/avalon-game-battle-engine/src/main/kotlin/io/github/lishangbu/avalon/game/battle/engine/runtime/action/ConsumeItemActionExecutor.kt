package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ConsumeItemActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.ConsumeItemMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `consume_item` 动作执行器。
 */
class ConsumeItemActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.CONSUME_ITEM

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is ConsumeItemActionNode) { "Action must be ConsumeItemActionNode." }
        return ActionResult(
            mutations = listOf(ConsumeItemMutation(action.target)),
        )
    }
}
