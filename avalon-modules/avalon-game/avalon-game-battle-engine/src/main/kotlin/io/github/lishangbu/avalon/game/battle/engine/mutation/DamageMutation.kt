package io.github.lishangbu.avalon.game.battle.engine.mutation

import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 伤害变更。
 *
 * @property target 目标选择器。
 * @property mode 伤害模式。
 * @property value 伤害值。
 */
data class DamageMutation(
    val target: TargetSelectorId,
    val mode: String?,
    val value: Double,
) : BattleMutation
