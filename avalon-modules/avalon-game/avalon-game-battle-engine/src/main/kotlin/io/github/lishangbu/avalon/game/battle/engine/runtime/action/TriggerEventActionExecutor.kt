package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.TriggerEventActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.TriggerEventMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `trigger_event` 动作执行器。
 */
class TriggerEventActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.TRIGGER_EVENT

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is TriggerEventActionNode) { "Action must be TriggerEventActionNode." }
        return ActionResult(
            mutations = listOf(TriggerEventMutation(action.hookName)),
        )
    }
}
