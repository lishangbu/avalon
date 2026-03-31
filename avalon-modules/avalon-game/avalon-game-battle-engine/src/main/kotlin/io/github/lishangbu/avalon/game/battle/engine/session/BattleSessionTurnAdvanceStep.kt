package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 回合结束推进步骤。
 */
class BattleSessionTurnAdvanceStep : BattleSessionTurnStep {
    /**
     * 当前步骤的执行顺序。
     */
    override val order: Int = 200

    /**
     * 在 battle 尚未结束时推进 residual 与 turn 递增。
     */
    override fun execute(context: BattleSessionTurnContext) {
        context.snapshot =
            if (context.session.currentSnapshot.battle.ended) {
                context.session.currentSnapshot
            } else {
                context.session.endTurn()
            }
    }
}
