package io.github.lishangbu.avalon.game.battle.engine.dsl.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActorId
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * 招式标签判断条件。
 *
 * @property actor 被检查的对象，通常是 move。
 * @property value 目标标签。
 */
data class MoveHasTagConditionNode(
    val actor: ActorId,
    val value: String,
) : ConditionNode {
    override val type: ConditionTypeId = StandardConditionTypeIds.MOVE_HAS_TAG
}
