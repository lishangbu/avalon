package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds
import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 回复动作。
 *
 * @property target 回复目标选择器。
 * @property mode 回复模式。
 * @property value 回复值。
 */
data class HealActionNode(
    val target: TargetSelectorId,
    val mode: String? = null,
    val value: Double,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.HEAL
}
