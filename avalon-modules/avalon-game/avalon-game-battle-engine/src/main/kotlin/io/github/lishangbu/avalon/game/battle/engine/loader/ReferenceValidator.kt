package io.github.lishangbu.avalon.game.battle.engine.loader

import io.github.lishangbu.avalon.game.battle.engine.dsl.EffectDefinition

/**
 * 引用合法性校验接口。
 *
 * 设计意图：
 * - 校验 effect 之间的交叉引用是否存在且语义合法。
 * - 把引用校验从 schema 校验与数据加载中拆出来。
 */
interface ReferenceValidator {
    /**
     * 校验 effect 集合内部的引用关系。
     */
    fun validate(effects: List<EffectDefinition>)
}
