package io.github.lishangbu.avalon.game.battle.engine.mutation

import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 清除轻量标记变更。
 *
 * @property target 目标选择器。
 * @property key 标记键。
 */
data class ClearFlagMutation(
    val target: TargetSelectorId,
    val key: String,
) : BattleMutation
