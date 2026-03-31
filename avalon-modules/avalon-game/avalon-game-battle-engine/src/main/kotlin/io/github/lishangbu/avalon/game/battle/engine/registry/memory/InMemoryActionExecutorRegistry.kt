package io.github.lishangbu.avalon.game.battle.engine.registry.memory

import io.github.lishangbu.avalon.game.battle.engine.registry.ActionExecutorRegistry
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId

/**
 * 基于内存 Map 的 ActionExecutorRegistry 实现。
 *
 * @property entries 动作类型到执行器的映射。
 */
class InMemoryActionExecutorRegistry(
    private val entries: Map<ActionTypeId, ActionExecutor>,
) : ActionExecutorRegistry {
    override fun get(type: ActionTypeId): ActionExecutor =
        entries[type]
            ?: error("No action executor registered for '${type.value}'.")

    override fun contains(type: ActionTypeId): Boolean = entries.containsKey(type)
}
