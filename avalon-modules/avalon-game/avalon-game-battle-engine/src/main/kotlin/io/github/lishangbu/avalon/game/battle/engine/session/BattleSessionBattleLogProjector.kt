package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * battle log 投影器。
 */
class BattleSessionBattleLogProjector : BattleSessionProjector {
    /**
     * 当前 projector 在发布链中的执行顺序。
     */
    override val order: Int = 0

    /**
     * 把发布事件中的 battle log 文本投影到 session battle log。
     */
    override fun project(
        session: BattleSession,
        publication: BattleSessionPublication,
    ) {
        val message = publication.battleLogMessage ?: return
        session.appendBattleLog(message)
    }
}
