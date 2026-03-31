package io.github.lishangbu.avalon.game.battle.engine.loader

import io.github.lishangbu.avalon.game.battle.engine.dsl.EffectDefinition

/**
 * 战斗数据加载接口。
 *
 * 设计意图：
 * - 定义从资源、文件或其他来源加载 effect 数据的统一入口。
 * - 把“加载”与“校验”“解释执行”彻底解耦。
 */
interface BattleDataLoader {
    /**
     * 加载所有 effect 定义。
     */
    fun loadEffects(): List<EffectDefinition>
}
