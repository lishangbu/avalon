package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds
import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 强制替换动作。
 *
 * @property target 目标选择器。
 */
data class ForceSwitchActionNode(
    val target: TargetSelectorId,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.FORCE_SWITCH
}
