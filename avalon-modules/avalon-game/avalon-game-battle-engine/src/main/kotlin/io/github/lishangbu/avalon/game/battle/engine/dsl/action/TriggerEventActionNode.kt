package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.HookName
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * 主动触发另一个 Hook 的动作。
 *
 * @property hookName 被触发的 Hook 名称。
 */
data class TriggerEventActionNode(
    val hookName: HookName,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.TRIGGER_EVENT
}
