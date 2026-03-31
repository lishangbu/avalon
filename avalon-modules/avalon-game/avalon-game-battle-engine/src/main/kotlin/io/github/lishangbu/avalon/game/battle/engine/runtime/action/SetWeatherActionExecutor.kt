package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.SetWeatherActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.SetWeatherMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `set_weather` 动作执行器。
 */
class SetWeatherActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.SET_WEATHER

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is SetWeatherActionNode) { "Action must be SetWeatherActionNode." }
        return ActionResult(
            mutations = listOf(SetWeatherMutation(action.value)),
        )
    }
}
