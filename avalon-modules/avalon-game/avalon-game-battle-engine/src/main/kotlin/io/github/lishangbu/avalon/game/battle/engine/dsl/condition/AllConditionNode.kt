package io.github.lishangbu.avalon.game.battle.engine.dsl.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * 逻辑与条件。
 *
 * @property conditions 所有子条件都必须成立的条件列表。
 */
data class AllConditionNode(
    val conditions: List<ConditionNode>,
) : ConditionNode {
    override val type: ConditionTypeId = StandardConditionTypeIds.ALL
}
