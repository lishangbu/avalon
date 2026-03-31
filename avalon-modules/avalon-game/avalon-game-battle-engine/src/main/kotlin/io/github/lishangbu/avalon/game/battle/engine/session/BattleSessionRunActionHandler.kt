package io.github.lishangbu.avalon.game.battle.engine.session

import kotlin.reflect.KClass

/**
 * `BattleSessionRunAction` 执行处理器。
 *
 * @property actionExecutionSupport switch/run 等状态更新辅助组件。
 */
class BattleSessionRunActionHandler(
    private val actionExecutionSupport: BattleSessionActionExecutionSupport,
) : BattleSessionActionHandler {
    /**
     * 当前处理器负责的 action 类型。
     */
    override val actionType: KClass<out BattleSessionAction> = BattleSessionRunAction::class

    /**
     * 执行 run action。
     */
    override fun execute(
        action: BattleSessionAction,
        session: BattleSession,
    ): BattleSessionActionExecutionResult {
        val runAction =
            action as? BattleSessionRunAction
                ?: error("Expected BattleSessionRunAction but got '${action::class.qualifiedName}'.")
        session.currentSnapshot = actionExecutionSupport.applyRunAction(session, runAction)
        return BattleSessionActionExecutionResult(
            action = runAction,
            snapshot = session.currentSnapshot,
        )
    }
}
