package io.github.lishangbu.avalon.game.battle.engine.dsl.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * 目标关系判断条件。
 *
 * @property value 关系值，例如 foe、ally。
 */
data class TargetRelationConditionNode(
    val value: String,
) : ConditionNode {
    override val type: ConditionTypeId = StandardConditionTypeIds.TARGET_RELATION
}
