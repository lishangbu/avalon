package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds
import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * Boost / stage 变化动作。
 *
 * @property target 目标选择器。
 * @property stats 各属性对应的 stage 变化值。
 */
data class BoostActionNode(
    val target: TargetSelectorId,
    val stats: Map<String, Int>,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.BOOST
}
