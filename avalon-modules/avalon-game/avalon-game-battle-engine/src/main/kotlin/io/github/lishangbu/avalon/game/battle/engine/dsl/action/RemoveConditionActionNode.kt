package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds
import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 移除 condition / effect 的动作。
 *
 * @property target 目标选择器。
 * @property value condition 或 effect 标识。
 */
data class RemoveConditionActionNode(
    val target: TargetSelectorId,
    val value: String,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.REMOVE_CONDITION
}
