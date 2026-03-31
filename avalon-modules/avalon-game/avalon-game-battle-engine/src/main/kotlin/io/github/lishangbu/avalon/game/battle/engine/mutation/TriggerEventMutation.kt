package io.github.lishangbu.avalon.game.battle.engine.mutation

import io.github.lishangbu.avalon.game.battle.engine.type.HookName

/**
 * 主动触发 Hook 的变更请求。
 *
 * @property hookName 被触发的 Hook 名称。
 */
data class TriggerEventMutation(
    val hookName: HookName,
) : BattleMutation
