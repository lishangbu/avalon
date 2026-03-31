package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.RestorePpActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.RestorePpMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `restore_pp` 动作执行器。
 */
class RestorePpActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.RESTORE_PP

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is RestorePpActionNode) { "Action must be RestorePpActionNode." }
        return ActionResult(
            mutations = listOf(RestorePpMutation(action.target, action.moveId, action.value)),
        )
    }
}
