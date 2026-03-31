package io.github.lishangbu.avalon.game.battle.engine.dsl.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActorId
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * 属性包含判断条件。
 *
 * @property actor 被检查的对象。
 * @property value 目标属性标识。
 */
data class HasTypeConditionNode(
    val actor: ActorId,
    val value: String,
) : ConditionNode {
    override val type: ConditionTypeId = StandardConditionTypeIds.HAS_TYPE
}
