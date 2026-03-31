package io.github.lishangbu.avalon.game.battle.engine.registry.memory

import io.github.lishangbu.avalon.game.battle.engine.registry.ConditionInterpreterRegistry
import io.github.lishangbu.avalon.game.battle.engine.runtime.ConditionInterpreter
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId

/**
 * 基于内存 Map 的 ConditionInterpreterRegistry 实现。
 *
 * @property entries 条件类型到解释器的映射。
 */
class InMemoryConditionInterpreterRegistry(
    private val entries: Map<ConditionTypeId, ConditionInterpreter>,
) : ConditionInterpreterRegistry {
    override fun get(type: ConditionTypeId): ConditionInterpreter =
        entries[type]
            ?: error("No condition interpreter registered for '${type.value}'.")

    override fun contains(type: ConditionTypeId): Boolean = entries.containsKey(type)
}
