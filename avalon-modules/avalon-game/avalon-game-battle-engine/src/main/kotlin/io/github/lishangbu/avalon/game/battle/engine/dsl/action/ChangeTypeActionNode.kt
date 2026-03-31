package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds
import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 修改属性列表动作。
 *
 * @property target 目标选择器。
 * @property values 变更后的属性集合。
 */
data class ChangeTypeActionNode(
    val target: TargetSelectorId,
    val values: List<String>,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.CHANGE_TYPE
}
