package io.github.lishangbu.avalon.game.battle.engine.mutation

import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 回复变更。
 *
 * @property target 目标选择器。
 * @property mode 回复模式。
 * @property value 回复值。
 */
data class HealMutation(
    val target: TargetSelectorId,
    val mode: String?,
    val value: Double,
) : BattleMutation
