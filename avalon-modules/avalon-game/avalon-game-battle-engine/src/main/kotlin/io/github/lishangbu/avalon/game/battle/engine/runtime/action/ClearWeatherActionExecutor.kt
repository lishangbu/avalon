package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ClearWeatherActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.ClearWeatherMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `clear_weather` 动作执行器。
 */
class ClearWeatherActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.CLEAR_WEATHER

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is ClearWeatherActionNode) { "Action must be ClearWeatherActionNode." }
        return ActionResult(
            mutations = listOf(ClearWeatherMutation()),
        )
    }
}
