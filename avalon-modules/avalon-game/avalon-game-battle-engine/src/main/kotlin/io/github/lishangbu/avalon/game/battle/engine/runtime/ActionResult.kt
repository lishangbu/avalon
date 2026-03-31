package io.github.lishangbu.avalon.game.battle.engine.runtime

import io.github.lishangbu.avalon.game.battle.engine.mutation.BattleMutation

/**
 * 单个动作执行后的结果。
 *
 * 设计意图：
 * - 统一表达动作是否中断流程以及是否修改 relay。
 * - 让动作执行器与上层 Hook 处理器之间的返回值协议保持稳定。
 *
 * @property cancelled 当前动作是否请求中断后续流程。
 * @property relay 动作返回的新 relay 值，空值表示不修改。
 * @property mutations 当前动作产生的结构化变更列表。
 */
data class ActionResult(
    val cancelled: Boolean = false,
    val relay: Any? = null,
    val mutations: List<BattleMutation> = emptyList(),
)
