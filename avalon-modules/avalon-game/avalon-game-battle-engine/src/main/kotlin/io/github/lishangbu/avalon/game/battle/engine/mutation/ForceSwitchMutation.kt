package io.github.lishangbu.avalon.game.battle.engine.mutation

import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 强制替换变更。
 *
 * @property target 目标选择器。
 */
data class ForceSwitchMutation(
    val target: TargetSelectorId,
) : BattleMutation
