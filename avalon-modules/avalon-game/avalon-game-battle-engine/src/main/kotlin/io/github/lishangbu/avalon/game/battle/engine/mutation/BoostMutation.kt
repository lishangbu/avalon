package io.github.lishangbu.avalon.game.battle.engine.mutation

import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * Boost 变更。
 *
 * @property target 目标选择器。
 * @property boosts 属性到 stage 变化值的映射。
 */
data class BoostMutation(
    val target: TargetSelectorId,
    val boosts: Map<String, Int>,
) : BattleMutation
