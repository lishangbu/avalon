package io.github.lishangbu.avalon.game.battle.engine.mutation

import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 设置主状态变更。
 *
 * @property target 目标选择器。
 * @property statusId 状态标识。
 */
data class SetStatusMutation(
    val target: TargetSelectorId,
    val statusId: String,
) : BattleMutation
