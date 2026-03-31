package io.github.lishangbu.avalon.game.battle.engine.registry

import io.github.lishangbu.avalon.game.battle.engine.runtime.ConditionInterpreter
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId

/**
 * 条件解释器注册中心。
 *
 * 设计意图：
 * - 根据 condition type 查找对应解释器。
 * - 保持 DSL 条件系统可扩展。
 */
interface ConditionInterpreterRegistry {
    /**
     * 获取指定条件类型的解释器。
     */
    fun get(type: ConditionTypeId): ConditionInterpreter

    /**
     * 判断指定条件类型是否已注册。
     */
    fun contains(type: ConditionTypeId): Boolean
}
