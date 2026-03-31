package io.github.lishangbu.avalon.game.battle.engine.mutation

import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 清空 boosts 变更。
 *
 * @property target 目标选择器。
 */
data class ClearBoostsMutation(
    val target: TargetSelectorId,
) : BattleMutation
