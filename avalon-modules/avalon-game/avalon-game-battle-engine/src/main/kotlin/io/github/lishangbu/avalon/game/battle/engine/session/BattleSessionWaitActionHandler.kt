package io.github.lishangbu.avalon.game.battle.engine.session

import kotlin.reflect.KClass

/**
 * `BattleSessionWaitAction` 执行处理器。
 */
class BattleSessionWaitActionHandler : BattleSessionActionHandler {
    /**
     * 当前处理器负责的 action 类型。
     */
    override val actionType: KClass<out BattleSessionAction> = BattleSessionWaitAction::class

    /**
     * 执行 wait action。
     */
    override fun execute(
        action: BattleSessionAction,
        session: BattleSession,
    ): BattleSessionActionExecutionResult {
        val waitAction =
            action as? BattleSessionWaitAction
                ?: error("Expected BattleSessionWaitAction but got '${action::class.qualifiedName}'.")
        session.recordLog("Executed wait action for unit ${waitAction.unitId}.")
        session.recordEvent(
            BattleSessionWaitExecutedPayload(unitId = waitAction.unitId),
        )
        return BattleSessionActionExecutionResult(
            action = waitAction,
            snapshot = session.currentSnapshot,
        )
    }
}
