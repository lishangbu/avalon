package io.github.lishangbu.avalon.game.battle.engine.mutation

import io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId

/**
 * 回复 PP 变更。
 *
 * @property target 目标选择器。
 * @property moveId 目标招式标识。
 * @property value 回复量。
 */
data class RestorePpMutation(
    val target: TargetSelectorId,
    val moveId: String?,
    val value: Int,
) : BattleMutation
