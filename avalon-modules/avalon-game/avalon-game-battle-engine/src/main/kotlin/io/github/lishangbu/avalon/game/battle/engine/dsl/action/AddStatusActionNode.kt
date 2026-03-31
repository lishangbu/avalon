package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds
import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 附加主状态动作。
 *
 * @property target 目标选择器。
 * @property value 状态标识。
 */
data class AddStatusActionNode(
    val target: TargetSelectorId,
    val value: String,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.ADD_STATUS
}
