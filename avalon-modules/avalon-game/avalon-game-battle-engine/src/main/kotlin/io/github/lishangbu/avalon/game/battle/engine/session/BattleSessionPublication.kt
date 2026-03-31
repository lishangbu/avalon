package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * battle session 内部发布事件。
 *
 * 设计意图：
 * - 作为 `BattleSessionEventPublisher` 与各类 projector 之间的统一载体。
 * - 让 battle log、结构化事件和 resource ledger 可以共享同一条发布链路。
 *
 * @property turn 发布发生时的回合数。
 * @property battleLogMessage 需要投影到 battle log 的文本消息。
 * @property eventPayload 需要投影到结构化事件流的强类型 payload。
 * @property resourceUsage 需要投影到 resource ledger 的账本条目。
 */
data class BattleSessionPublication(
    val turn: Int,
    val battleLogMessage: String? = null,
    val eventPayload: BattleSessionEventPayload? = null,
    val resourceUsage: BattleSessionResourceUsage? = null,
)
