package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.HealActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.HealMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `heal` 动作执行器。
 */
class HealActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.HEAL

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is HealActionNode) { "Action must be HealActionNode." }
        return ActionResult(
            mutations = listOf(HealMutation(action.target, action.mode, action.value)),
        )
    }
}
