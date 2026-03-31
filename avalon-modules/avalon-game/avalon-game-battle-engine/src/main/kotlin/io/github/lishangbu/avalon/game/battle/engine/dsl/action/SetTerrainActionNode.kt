package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * 设置地形动作。
 *
 * @property value 目标地形标识。
 */
data class SetTerrainActionNode(
    val value: String,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.SET_TERRAIN
}
