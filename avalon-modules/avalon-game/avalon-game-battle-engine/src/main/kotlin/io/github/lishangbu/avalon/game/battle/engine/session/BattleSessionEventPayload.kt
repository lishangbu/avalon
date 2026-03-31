package io.github.lishangbu.avalon.game.battle.engine.session

import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * battle session 结构化事件 payload。
 *
 * 设计意图：
 * - 用强类型 payload 取代 `BattleSessionEvent` 内部字符串化的 attributes 写入方式。
 * - 保留 `type` 与 `attributes` 的统一派生能力，兼容现有读模型。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface BattleSessionEventPayload {
    /**
     * 当前 payload 对应的结构化事件类型。
     */
    val type: BattleSessionEventType

    /**
     * 把强类型 payload 映射为兼容现有读模型的 attributes 结构。
     *
     * @return 与旧事件模型兼容的属性映射。
     */
    fun toAttributes(): Map<String, Any?> = emptyMap()
}

/**
 * battle session 动作事件种类。
 *
 * 设计意图：
 * - 为 queued/executed/battle-ended 等事件提供比裸字符串更明确的动作语义。
 *
 * @property wireValue 与既有 `attributes["actionType"]` 兼容的字符串值。
 */
enum class BattleSessionActionEventKind(
    val wireValue: String,
) {
    MOVE("move"),
    SWITCH("switch"),
    ITEM("item"),
    CAPTURE("capture"),
    RUN("run"),
    WAIT("wait"),
}
