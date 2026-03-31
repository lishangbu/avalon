package io.github.lishangbu.avalon.game.battle.engine.dsl.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActorId
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * 血量比例比较条件。
 *
 * @property actor 被比较的对象。
 * @property operator 比较操作符，例如 `<=`。
 * @property value 目标血量比例值。
 */
data class HpRatioConditionNode(
    val actor: ActorId,
    val operator: String,
    val value: Double,
) : ConditionNode {
    override val type: ConditionTypeId = StandardConditionTypeIds.HP_RATIO
}
