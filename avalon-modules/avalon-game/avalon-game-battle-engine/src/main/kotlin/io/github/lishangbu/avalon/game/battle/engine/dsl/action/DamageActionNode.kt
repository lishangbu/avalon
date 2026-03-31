package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds
import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 伤害动作。
 *
 * @property target 伤害目标选择器。
 * @property mode 伤害模式，例如固定值或按最大生命比例。
 * @property value 伤害值。
 */
data class DamageActionNode(
    val target: TargetSelectorId,
    val mode: String? = null,
    val value: Double,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.DAMAGE
}
