package io.github.lishangbu.avalon.game.battle.engine.runtime.support

import io.github.lishangbu.avalon.game.battle.engine.event.EventContext

/**
 * 概率判定辅助组件。
 *
 * 当前实现从 EventContext.attributes 中读取 `chanceRoll`。
 */
object ChanceRollReader {
    fun read(context: EventContext): Int {
        val rawValue: Any? = context.attributes["chanceRoll"]
        return (rawValue as? Number)?.toInt()
            ?: error("EventContext.attributes['chanceRoll'] is required for chance evaluation.")
    }
}
