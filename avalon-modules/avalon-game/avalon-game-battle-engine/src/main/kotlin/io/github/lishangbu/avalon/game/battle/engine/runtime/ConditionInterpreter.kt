package io.github.lishangbu.avalon.game.battle.engine.runtime

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId

/**
 * 条件解释器接口。
 *
 * 设计意图：
 * - 负责把某类 [ConditionNode] 在给定 [EventContext] 中求值为布尔结果。
 * - 通过 [type] 与 registry 建立一一对应关系。
 *
 * 该接口只负责求值，不负责动作执行和状态变更。
 */
fun interface ConditionInterpreter {
    /**
     * 在指定上下文中计算条件是否成立。
     */
    fun evaluate(
        condition: ConditionNode,
        context: EventContext,
    ): Boolean

    /**
     * 当前解释器对应的条件类型标识。
     */
    val type: ConditionTypeId
        get() = error("ConditionInterpreter.type must be overridden by concrete implementations.")
}
