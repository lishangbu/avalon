package io.github.lishangbu.avalon.game.battle.engine.runtime

import io.github.lishangbu.avalon.game.battle.engine.mutation.BattleMutation

/**
 * 单条 HookRule 处理结果。
 *
 * @property matched 条件是否命中 then 分支。
 * @property cancelled 是否中断后续流程。
 * @property relay 处理完成后的 relay 值。
 * @property mutations 处理过程中产生的结构化变更。
 */
data class HookRuleResult(
    val matched: Boolean,
    val cancelled: Boolean,
    val relay: Any? = null,
    val mutations: List<BattleMutation> = emptyList(),
)
