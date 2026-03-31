package io.github.lishangbu.avalon.game.battle.engine.mutation

import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 修改属性列表变更。
 *
 * @property target 目标选择器。
 * @property values 新的属性集合。
 */
data class ChangeTypeMutation(
    val target: TargetSelectorId,
    val values: List<String>,
) : BattleMutation
