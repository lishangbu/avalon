package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ModifyMultiplierActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `modify_multiplier` 动作执行器。
 */
class ModifyMultiplierActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.MODIFY_MULTIPLIER

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is ModifyMultiplierActionNode) { "Action must be ModifyMultiplierActionNode." }
        val relayValue =
            context.relay as? Number
                ?: error("modify_multiplier requires numeric relay value.")
        return ActionResult(
            relay = relayValue.toDouble() * action.value,
        )
    }
}
