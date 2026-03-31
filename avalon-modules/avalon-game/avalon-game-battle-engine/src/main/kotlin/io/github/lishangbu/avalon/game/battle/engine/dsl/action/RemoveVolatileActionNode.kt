package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds
import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 移除挥发状态动作。
 *
 * @property target 目标选择器。
 * @property value 挥发状态标识。
 */
data class RemoveVolatileActionNode(
    val target: TargetSelectorId,
    val value: String,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.REMOVE_VOLATILE
}
