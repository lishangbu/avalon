package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.AddRelayActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `add_relay` 动作执行器。
 */
class AddRelayActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.ADD_RELAY

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is AddRelayActionNode) { "Action must be AddRelayActionNode." }
        val relayValue =
            context.relay as? Number
                ?: error("add_relay requires numeric relay value.")
        return ActionResult(
            relay = relayValue.toDouble() + action.value,
        )
    }
}
