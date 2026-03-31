package io.github.lishangbu.avalon.game.battle.engine.mutation

import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 设置轻量标记变更。
 *
 * @property target 目标选择器。
 * @property key 标记键。
 * @property value 标记值。
 */
data class SetFlagMutation(
    val target: TargetSelectorId,
    val key: String,
    val value: String,
) : BattleMutation
