package io.github.lishangbu.avalon.game.battle.engine.dsl.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * 逻辑或条件。
 *
 * @property conditions 任一子条件成立即可成立的条件列表。
 */
data class AnyConditionNode(
    val conditions: List<ConditionNode>,
) : ConditionNode {
    override val type: ConditionTypeId = StandardConditionTypeIds.ANY
}
