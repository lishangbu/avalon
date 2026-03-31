package io.github.lishangbu.avalon.game.battle.engine.registry.memory

import io.github.lishangbu.avalon.game.battle.engine.handler.SpecialHandler
import io.github.lishangbu.avalon.game.battle.engine.registry.SpecialHandlerRegistry
import io.github.lishangbu.avalon.game.battle.engine.type.SpecialHandlerId

/**
 * 基于内存 Map 的 SpecialHandlerRegistry 实现。
 *
 * @property entries handler 标识到实现的映射。
 */
class InMemorySpecialHandlerRegistry(
    private val entries: Map<SpecialHandlerId, SpecialHandler>,
) : SpecialHandlerRegistry {
    override fun get(id: SpecialHandlerId): SpecialHandler =
        entries[id]
            ?: error("No special handler registered for '${id.value}'.")

    override fun contains(id: SpecialHandlerId): Boolean = entries.containsKey(id)
}
