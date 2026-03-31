package io.github.lishangbu.avalon.game.battle.engine.dsl.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActorId
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * 主状态判断条件。
 *
 * @property actor 被检查的对象。
 * @property value 期望的状态标识，空值可用于表示“是否存在任意状态”。
 */
data class HasStatusConditionNode(
    val actor: ActorId,
    val value: String?,
) : ConditionNode {
    override val type: ConditionTypeId = StandardConditionTypeIds.HAS_STATUS
}
