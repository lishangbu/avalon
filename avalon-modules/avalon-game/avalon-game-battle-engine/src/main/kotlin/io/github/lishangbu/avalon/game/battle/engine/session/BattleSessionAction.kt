package io.github.lishangbu.avalon.game.battle.engine.session

import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * BattleSession 队列中的通用行动模型。
 *
 * 设计意图：
 * - 让 session 队列可以同时容纳 move、switch 等不同类型行动。
 * - 为排序逻辑提供统一的优先级和速度字段。
 *
 * @property kind 当前 action 的命令种类。
 * @property priority 当前 action 的行动优先级。
 * @property speed 当前 action 的排序速度值。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface BattleSessionAction {
    /**
     * 当前 action 的命令种类。
     */
    val kind: BattleSessionActionKind

    /**
     * 当前 action 的行动优先级。
     */
    val priority: Int

    /**
     * 当前 action 的排序速度值。
     */
    val speed: Int
}
