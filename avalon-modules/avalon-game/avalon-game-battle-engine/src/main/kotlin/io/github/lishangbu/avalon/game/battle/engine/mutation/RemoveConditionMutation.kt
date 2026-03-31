package io.github.lishangbu.avalon.game.battle.engine.mutation

import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 移除 condition / effect 的变更。
 *
 * @property target 目标选择器。
 * @property conditionId 目标 condition / effect 标识。
 */
data class RemoveConditionMutation(
    val target: TargetSelectorId,
    val conditionId: String,
) : BattleMutation
