package io.github.lishangbu.avalon.game.battle.engine.dsl.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * 地形判断条件。
 *
 * @property value 期望的地形标识。
 */
data class TerrainIsConditionNode(
    val value: String,
) : ConditionNode {
    override val type: ConditionTypeId = StandardConditionTypeIds.TERRAIN_IS
}
