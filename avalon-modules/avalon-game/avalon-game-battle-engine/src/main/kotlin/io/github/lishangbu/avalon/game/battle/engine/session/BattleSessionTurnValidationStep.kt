package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 回合推进前置校验步骤。
 */
class BattleSessionTurnValidationStep : BattleSessionTurnStep {
    /**
     * 当前步骤的执行顺序。
     */
    override val order: Int = 0

    /**
     * 确认当前回合已经满足最小结算条件。
     */
    override fun execute(context: BattleSessionTurnContext) {
        context.session.ensureTurnReady()
        context.snapshot = context.session.currentSnapshot
    }
}
