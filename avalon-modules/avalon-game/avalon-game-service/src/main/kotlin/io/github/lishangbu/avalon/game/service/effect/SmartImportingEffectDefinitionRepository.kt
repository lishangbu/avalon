package io.github.lishangbu.avalon.game.service.effect

import io.github.lishangbu.avalon.game.battle.engine.dsl.EffectDefinition
import io.github.lishangbu.avalon.game.battle.engine.repository.EffectDefinitionRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * 带缓存的智能导入 effect 仓库。
 */
class SmartImportingEffectDefinitionRepository(
    private val importService: SmartBattleEffectImportService,
) : EffectDefinitionRepository {
    private val cache: MutableMap<String, EffectDefinition> = ConcurrentHashMap()

    override fun get(id: String): EffectDefinition =
        cache[id]
            ?: importService.importEffect(id)?.also { imported -> cache[id] = imported }
            ?: error("Effect '$id' was not found in move/ability/item dataset.")

    override fun contains(id: String): Boolean = cache.containsKey(id) || importService.importEffect(id)?.also { imported -> cache[id] = imported } != null
}
