package io.github.lishangbu.avalon.game.battle.engine.mutation

import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 添加挥发状态变更。
 *
 * @property target 目标选择器。
 * @property volatileId 挥发状态标识。
 */
data class AddVolatileMutation(
    val target: TargetSelectorId,
    val volatileId: String,
) : BattleMutation
