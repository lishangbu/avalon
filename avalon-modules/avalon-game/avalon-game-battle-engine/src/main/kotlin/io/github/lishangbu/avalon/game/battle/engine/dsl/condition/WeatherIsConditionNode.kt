package io.github.lishangbu.avalon.game.battle.engine.dsl.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * 天气判断条件。
 *
 * @property value 期望的天气标识。
 */
data class WeatherIsConditionNode(
    val value: String,
) : ConditionNode {
    override val type: ConditionTypeId = StandardConditionTypeIds.WEATHER_IS
}
