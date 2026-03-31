package io.github.lishangbu.avalon.game.battle.engine.dsl.action

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActionTypeIds

/**
 * 设置天气动作。
 *
 * @property value 目标天气标识。
 */
data class SetWeatherActionNode(
    val value: String,
) : ActionNode {
    override val type: ActionTypeId = StandardActionTypeIds.SET_WEATHER
}
