package io.github.lishangbu.avalon.game.battle.engine.dsl.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActorId
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * Stage / boost 比较条件。
 *
 * @property actor 被比较的对象。
 * @property stat 被比较的能力项。
 * @property operator 比较操作符。
 * @property value 比较目标值。
 */
data class BoostCompareConditionNode(
    val actor: ActorId,
    val stat: String,
    val operator: String,
    val value: Int,
) : ConditionNode {
    override val type: ConditionTypeId = StandardConditionTypeIds.BOOST_COMPARE
}
