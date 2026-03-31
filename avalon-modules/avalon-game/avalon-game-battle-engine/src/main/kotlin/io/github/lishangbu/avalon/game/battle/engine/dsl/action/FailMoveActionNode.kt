package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * 使当前招式失败的动作。
 */
class FailMoveActionNode : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.FAIL_MOVE
}
