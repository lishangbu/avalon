package io.github.lishangbu.avalon.game.battle.engine.runtime

import io.github.lishangbu.avalon.game.battle.engine.dsl.ActionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId

/**
 * 动作执行器接口。
 *
 * 设计意图：
 * - 负责执行某类 [ActionNode]。
 * - 通过 [type] 与 registry 建立分发关系。
 *
 * 该接口只负责单个动作执行，不负责整条 Hook 规则调度。
 */
fun interface ActionExecutor {
    /**
     * 在指定上下文中执行动作并返回执行结果。
     */
    fun execute(
        action: ActionNode,
        context: EventContext,
    ): ActionResult

    /**
     * 当前执行器负责的动作类型标识。
     */
    val type: ActionTypeId
        get() = error("ActionExecutor.type must be overridden by concrete implementations.")
}
