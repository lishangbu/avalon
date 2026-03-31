package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * battle session 事件发布器。
 *
 * 设计意图：
 * - 把 session 内部产生的事件统一发布给多个 projector。
 * - 让 battle log、event log、resource ledger 的写入从 `BattleSession` 主体中抽离。
 */
interface BattleSessionEventPublisher {
    /**
     * 发布一条 session 内部事件。
     *
     * @param session 当前 battle session。
     * @param publication 本次发布的事件载体。
     */
    fun publish(
        session: BattleSession,
        publication: BattleSessionPublication,
    )
}
