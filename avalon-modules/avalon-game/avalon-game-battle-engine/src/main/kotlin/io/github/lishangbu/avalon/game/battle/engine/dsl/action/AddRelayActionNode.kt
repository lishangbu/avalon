package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * 给当前 relay 增加固定数值的动作。
 *
 * @property value 需要叠加到 relay 上的值。
 */
data class AddRelayActionNode(
    val value: Double,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.ADD_RELAY
}
