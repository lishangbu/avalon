package io.github.lishangbu.avalon.game.battle.engine.mutation

import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 消耗道具变更。
 *
 * @property target 目标选择器。
 */
data class ConsumeItemMutation(
    val target: TargetSelectorId,
) : BattleMutation
