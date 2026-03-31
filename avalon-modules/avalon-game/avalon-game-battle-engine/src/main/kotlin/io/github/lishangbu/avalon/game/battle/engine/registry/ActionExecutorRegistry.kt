package io.github.lishangbu.avalon.game.battle.engine.registry

import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor
import io.github.lishangbu.avalon.game.battle.engine.type.ActionTypeId

/**
 * 动作执行器注册中心。
 *
 * 设计意图：
 * - 根据动作类型查找对应执行器。
 * - 保持动作系统可扩展且职责单一。
 */
interface ActionExecutorRegistry {
    /**
     * 获取指定动作类型的执行器。
     */
    fun get(type: ActionTypeId): ActionExecutor

    /**
     * 判断指定动作类型是否已注册。
     */
    fun contains(type: ActionTypeId): Boolean
}
