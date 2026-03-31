package io.github.lishangbu.avalon.game.battle.engine.repository.memory

import io.github.lishangbu.avalon.game.battle.engine.dsl.EffectDefinition
import io.github.lishangbu.avalon.game.battle.engine.repository.EffectDefinitionRepository

/**
 * 基于内存 Map 的 EffectDefinition 仓库。
 *
 * @property entries effect id 到定义的映射。
 */
class InMemoryEffectDefinitionRepository(
    private val entries: Map<String, EffectDefinition>,
) : EffectDefinitionRepository {
    override fun get(id: String): EffectDefinition =
        entries[id]
            ?: error("No effect definition found for '$id'.")

    override fun contains(id: String): Boolean = entries.containsKey(id)
}
