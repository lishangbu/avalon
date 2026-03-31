package io.github.lishangbu.avalon.game.battle.engine.dsl.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * 逻辑非条件。
 *
 * @property condition 被取反的子条件。
 */
data class NotConditionNode(
    val condition: ConditionNode,
) : ConditionNode {
    override val type: ConditionTypeId = StandardConditionTypeIds.NOT
}
