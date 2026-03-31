package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * 清除地形动作。
 */
class ClearTerrainActionNode : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.CLEAR_TERRAIN
}
