package io.github.lishangbu.avalon.game.battle.engine.dsl.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * 运行时扩展属性值比较条件。
 *
 * @property key EventContext.attributes 中的键。
 * @property value 期望值。
 */
data class AttributeEqualsConditionNode(
    val key: String,
    val value: String,
) : ConditionNode {
    override val type: ConditionTypeId = StandardConditionTypeIds.ATTRIBUTE_EQUALS
}
