package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 结构化事件投影器。
 */
class BattleSessionStructuredEventProjector : BattleSessionProjector {
    /**
     * 当前 projector 在发布链中的执行顺序。
     */
    override val order: Int = 100

    /**
     * 把发布事件中的结构化事件投影到 session event log。
     */
    override fun project(
        session: BattleSession,
        publication: BattleSessionPublication,
    ) {
        val eventPayload = publication.eventPayload ?: return
        session.appendEventLog(
            BattleSessionEvent(
                turn = publication.turn,
                payload = eventPayload,
            ),
        )
    }
}
