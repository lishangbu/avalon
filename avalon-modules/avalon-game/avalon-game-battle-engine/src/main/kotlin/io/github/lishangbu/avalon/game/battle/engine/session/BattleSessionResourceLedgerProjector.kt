package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * resource ledger 投影器。
 */
class BattleSessionResourceLedgerProjector : BattleSessionProjector {
    /**
     * 当前 projector 在发布链中的执行顺序。
     */
    override val order: Int = 200

    /**
     * 把发布事件中的资源账本条目投影到 session resource ledger。
     */
    override fun project(
        session: BattleSession,
        publication: BattleSessionPublication,
    ) {
        val usage = publication.resourceUsage ?: return
        session.appendResourceUsage(usage)
    }
}
