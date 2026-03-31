package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds
import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 设置轻量标记动作。
 *
 * @property target 目标选择器。
 * @property key 标记键。
 * @property value 标记值。
 */
data class SetFlagActionNode(
    val target: TargetSelectorId,
    val key: String,
    val value: String,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.SET_FLAG
}
