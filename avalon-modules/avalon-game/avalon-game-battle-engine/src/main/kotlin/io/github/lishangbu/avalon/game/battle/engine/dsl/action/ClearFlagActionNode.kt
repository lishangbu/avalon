package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds
import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 清除轻量标记动作。
 *
 * @property target 目标选择器。
 * @property key 标记键。
 */
data class ClearFlagActionNode(
    val target: TargetSelectorId,
    val key: String,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.CLEAR_FLAG
}
