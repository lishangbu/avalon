package io.github.lishangbu.avalon.game.battle.engine.dsl.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * 回合数比较条件。
 *
 * @property operator 比较操作符。
 * @property value 比较目标回合数。
 */
data class TurnCompareConditionNode(
    val operator: String,
    val value: Int,
) : ConditionNode {
    override val type: ConditionTypeId = StandardConditionTypeIds.TURN_COMPARE
}
