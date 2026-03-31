package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ChangeTypeActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.ChangeTypeMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `change_type` 动作执行器。
 */
class ChangeTypeActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.CHANGE_TYPE

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is ChangeTypeActionNode) { "Action must be ChangeTypeActionNode." }
        return ActionResult(
            mutations = listOf(ChangeTypeMutation(action.target, action.values)),
        )
    }
}
