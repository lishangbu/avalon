package io.github.lishangbu.avalon.game.battle.engine.registry

import io.github.lishangbu.avalon.game.battle.engine.handler.SpecialHandler
import io.github.lishangbu.avalon.game.battle.engine.type.SpecialHandlerId

/**
 * Special handler 注册中心。
 *
 * 设计意图：
 * - 为特例机制提供显式查找入口。
 * - 避免特殊逻辑分散在 battle 主流程中。
 */
interface SpecialHandlerRegistry {
    /**
     * 获取指定特例处理器。
     */
    fun get(id: SpecialHandlerId): SpecialHandler

    /**
     * 判断指定特例处理器是否已注册。
     */
    fun contains(id: SpecialHandlerId): Boolean
}
