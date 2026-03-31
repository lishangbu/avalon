package io.github.lishangbu.avalon.game.battle.engine.runtime

import io.github.lishangbu.avalon.game.battle.engine.event.EventContext

/**
 * 目标解析器接口。
 *
 * 设计意图：
 * - 把动作中的 target selector 解析为具体运行时对象集合。
 * - 让 selector 语义与动作执行逻辑解耦。
 */
interface TargetResolver {
    /**
     * 根据 selector 和上下文返回目标对象集合。
     */
    fun resolve(
        selector: String,
        context: EventContext,
    ): List<Any>
}
