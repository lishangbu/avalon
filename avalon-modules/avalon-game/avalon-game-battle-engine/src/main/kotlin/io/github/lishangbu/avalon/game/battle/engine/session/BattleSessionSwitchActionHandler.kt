package io.github.lishangbu.avalon.game.battle.engine.session

import kotlin.reflect.KClass

/**
 * `BattleSessionSwitchAction` 执行处理器。
 *
 * @property actionExecutionSupport switch/run 等状态更新辅助组件。
 */
class BattleSessionSwitchActionHandler(
    private val actionExecutionSupport: BattleSessionActionExecutionSupport,
) : BattleSessionActionHandler {
    /**
     * 当前处理器负责的 action 类型。
     */
    override val actionType: KClass<out BattleSessionAction> = BattleSessionSwitchAction::class

    /**
     * 执行 switch action。
     */
    override fun execute(
        action: BattleSessionAction,
        session: BattleSession,
    ): BattleSessionActionExecutionResult {
        val switchAction =
            action as? BattleSessionSwitchAction
                ?: error("Expected BattleSessionSwitchAction but got '${action::class.qualifiedName}'.")
        session.currentSnapshot = actionExecutionSupport.applySwitchAction(session, switchAction)
        return BattleSessionActionExecutionResult(
            action = switchAction,
            snapshot = session.currentSnapshot,
        )
    }
}
