package io.github.lishangbu.avalon.game.battle.engine.dsl.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActorId
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * 运行时属性比较条件。
 *
 * @property actor 被比较的对象。
 * @property stat 被比较的属性项。
 * @property operator 比较操作符。
 * @property value 比较目标值。
 */
data class StatCompareConditionNode(
    val actor: ActorId,
    val stat: String,
    val operator: String,
    val value: Int,
) : ConditionNode {
    override val type: ConditionTypeId = StandardConditionTypeIds.STAT_COMPARE
}
