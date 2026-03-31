package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * BattleSession 结构化事件。
 *
 * @property turn 事件发生时的回合数。
 * @property payload 当前事件的强类型 payload。
 */
data class BattleSessionEvent(
    val turn: Int,
    val payload: BattleSessionEventPayload,
) {
    /**
     * 当前事件对应的结构化事件类型。
     */
    val type: BattleSessionEventType
        get() = payload.type

    /**
     * 与旧读模型兼容的事件属性映射。
     */
    val attributes: Map<String, Any?>
        get() = payload.toAttributes()
}
