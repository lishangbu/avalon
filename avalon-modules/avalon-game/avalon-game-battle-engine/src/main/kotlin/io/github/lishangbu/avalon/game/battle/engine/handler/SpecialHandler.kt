package io.github.lishangbu.avalon.game.battle.engine.handler

import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionResult
import io.github.lishangbu.avalon.game.battle.engine.type.SpecialHandlerId

/**
 * 特例处理器接口。
 *
 * 设计意图：
 * - 为无法由通用 DSL 表达的机制提供显式实现入口。
 * - 让特例逻辑仍然通过统一协议接入引擎。
 */
interface SpecialHandler {
    /**
     * 当前 handler 的注册标识。
     */
    val id: SpecialHandlerId

    /**
     * 在给定上下文中执行特例逻辑。
     */
    fun execute(context: EventContext): ActionResult
}
