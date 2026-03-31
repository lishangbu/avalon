package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 会话层 action 执行协调器。
 *
 * 设计意图：
 * - 负责遍历当前回合动作队列，并把具体执行委托给 `BattleSessionActionHandler`。
 * - 让真正的 action 行为扩展点转移到 registry + handler 体系中。
 *
 * @property session 当前 battle session。
 * @property actionHandlerRegistry action 执行处理器注册中心。
 */
internal class BattleSessionActionExecutor(
    private val session: BattleSession,
    private val actionHandlerRegistry: BattleSessionActionHandlerRegistry,
) {
    /**
     * 执行当前回合动作队列中的所有 action。
     *
     * 队列顺序已经由 `BattleSessionActionQueue` 保证，这里只负责逐个分发与汇总。
     *
     * @return 当前回合所有已执行 action 的结果列表。
     */
    fun executeQueuedActions(): List<BattleSessionActionExecutionResult> {
        session.ensureStarted()
        val actions = session.actionQueue.drain()
        val results = mutableListOf<BattleSessionActionExecutionResult>()
        actions.forEach { action ->
            val handler = actionHandlerRegistry.get(action)
            results += handler.execute(action, session)
            if (session.currentSnapshot.battle.ended) {
                return results
            }
        }
        return results
    }
}
