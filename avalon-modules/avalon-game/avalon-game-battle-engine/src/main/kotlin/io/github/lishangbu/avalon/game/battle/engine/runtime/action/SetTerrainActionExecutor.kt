package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.SetTerrainActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.SetTerrainMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `set_terrain` 动作执行器。
 */
class SetTerrainActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.SET_TERRAIN

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is SetTerrainActionNode) { "Action must be SetTerrainActionNode." }
        return ActionResult(
            mutations = listOf(SetTerrainMutation(action.value)),
        )
    }
}
