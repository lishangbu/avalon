package io.github.lishangbu.avalon.game.battle.engine.session

import kotlin.reflect.KClass

/**
 * `BattleSessionCaptureAction` 执行处理器。
 */
class BattleSessionCaptureActionHandler : BattleSessionActionHandler {
    /**
     * 当前处理器负责的 action 类型。
     */
    override val actionType: KClass<out BattleSessionAction> = BattleSessionCaptureAction::class

    /**
     * 执行 capture action。
     */
    override fun execute(
        action: BattleSessionAction,
        session: BattleSession,
    ): BattleSessionActionExecutionResult {
        val captureAction =
            action as? BattleSessionCaptureAction
                ?: error("Expected BattleSessionCaptureAction but got '${action::class.qualifiedName}'.")
        val captureResult =
            session.captureActionResolver.resolve(
                session = session,
                action = captureAction,
            )
        session.recordResourceUsage(
            BattleSessionCaptureResourceUsage(
                playerId = captureAction.playerId,
                sourceUnitId = captureAction.sourceUnitId,
                ballItemId = captureAction.ballItemId,
                targetUnitId = captureAction.targetId,
                quantity = 1,
                success = captureResult.success,
                shakes = captureResult.shakes,
                reason = captureResult.reason,
                finalRate = captureResult.finalRate,
            ),
        )
        session.currentSnapshot =
            if (captureResult.success) {
                session.finishByCapture(captureAction.targetId)
            } else {
                session.recordCaptureFailure(
                    ballItemId = captureAction.ballItemId,
                    targetUnitId = captureAction.targetId,
                    shakes = captureResult.shakes,
                    reason = captureResult.reason,
                    finalRate = captureResult.finalRate,
                )
            }
        return BattleSessionActionExecutionResult(
            action = captureAction,
            snapshot = session.currentSnapshot,
            captureResult = captureResult,
        )
    }
}
