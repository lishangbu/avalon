package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 已入队 action 执行步骤。
 */
class BattleSessionQueuedActionExecutionStep : BattleSessionTurnStep {
    /**
     * 当前步骤的执行顺序。
     */
    override val order: Int = 100

    /**
     * 执行当前回合动作队列中的全部 action。
     */
    override fun execute(context: BattleSessionTurnContext) {
        context.actionResults = context.session.executeQueuedActions()
        context.snapshot = context.session.currentSnapshot
    }
}
