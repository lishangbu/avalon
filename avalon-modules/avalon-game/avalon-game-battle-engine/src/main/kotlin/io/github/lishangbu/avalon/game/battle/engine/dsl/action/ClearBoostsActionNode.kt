package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds
import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 清空 boosts 动作。
 *
 * @property target 目标选择器。
 */
data class ClearBoostsActionNode(
    val target: TargetSelectorId,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.CLEAR_BOOSTS
}
