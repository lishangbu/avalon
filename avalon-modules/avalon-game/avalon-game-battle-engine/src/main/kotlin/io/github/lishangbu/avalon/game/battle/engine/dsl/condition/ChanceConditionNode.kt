package io.github.lishangbu.avalon.game.battle.engine.dsl.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * 概率条件。
 *
 * @property value 概率值，通常使用百分比整数表达。
 */
data class ChanceConditionNode(
    val value: Int,
) : ConditionNode {
    override val type: ConditionTypeId = StandardConditionTypeIds.CHANCE
}
