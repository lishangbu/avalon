package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.action.DamageActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.DamageMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * `damage` 动作执行器。
 */
class DamageActionExecutor : ActionExecutor {
    override val type: ActionTypeId = StandardActionTypeIds.DAMAGE

    override fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult {
        require(action is DamageActionNode) { "Action must be DamageActionNode." }
        return ActionResult(
            mutations = listOf(DamageMutation(action.target, action.mode, action.value)),
        )
    }
}
