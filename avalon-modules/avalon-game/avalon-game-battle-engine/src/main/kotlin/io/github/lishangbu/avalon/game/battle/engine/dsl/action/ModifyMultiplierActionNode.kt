package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * 修改 relay 数值倍率的动作。
 *
 * @property value 倍率值。
 * @property rounding 可选舍入策略标识。
 */
data class ModifyMultiplierActionNode(
    val value: Double,
    val rounding: String? = null,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.MODIFY_MULTIPLIER
}
