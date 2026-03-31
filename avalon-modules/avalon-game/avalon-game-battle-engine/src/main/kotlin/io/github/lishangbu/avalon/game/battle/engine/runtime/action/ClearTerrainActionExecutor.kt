package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.ClearTerrainActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.ClearTerrainMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `clear_terrain` 动作执行器。
 */
class ClearTerrainActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.CLEAR_TERRAIN

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is ClearTerrainActionNode) { "Action must be ClearTerrainActionNode." }
        return ActionResult(
            mutations = listOf(ClearTerrainMutation()),
        )
    }
}
