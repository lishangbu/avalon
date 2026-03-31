package io.github.lishangbu.avalon.game.battle.engine.repository

import io.github.lishangbu.avalon.game.battle.engine.dsl.EffectDefinition

/**
 * EffectDefinition 仓库接口。
 *
 * 设计意图：
 * - 为 battle 主流程提供 effect 定义查询入口。
 * - 让 runtime 不直接依赖 loader 或原始资源路径。
 */
interface EffectDefinitionRepository {
    fun get(id: String): EffectDefinition

    fun contains(id: String): Boolean
}
