package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds
import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 回复 PP 动作。
 *
 * @property target 目标选择器。
 * @property moveId 目标招式标识，空值表示由执行器决定作用范围。
 * @property value 回复量。
 */
data class RestorePpActionNode(
    val target: TargetSelectorId,
    val moveId: String? = null,
    val value: Int,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.RESTORE_PP
}
