package io.github.lishangbu.avalon.game.battle.engine.service

import io.github.lishangbu.avalon.game.battle.engine.capture.CaptureFormulaService
import io.github.lishangbu.avalon.game.battle.engine.capture.DefaultCaptureFormulaService
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureAction
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureResult

/**
 * 引擎内置的默认 capture action resolver。
 *
 * 业务层只需要提供公式输入，具体公式计算由 battle-engine 负责。
 */
class DefaultCaptureActionResolver(
    private val captureFormulaInputResolver: CaptureFormulaInputResolver,
    private val captureFormulaService: CaptureFormulaService = DefaultCaptureFormulaService(),
) : CaptureActionResolver {
    override fun resolve(
        session: BattleSession,
        action: BattleSessionCaptureAction,
    ): BattleSessionCaptureResult {
        val formulaInput =
            captureFormulaInputResolver.resolve(
                sessionId = session.currentSnapshot.battle.id,
                snapshot = session.currentSnapshot,
                action = action,
            )
        val formulaResult = captureFormulaService.calculate(formulaInput, session::nextCaptureShakeRoll)
        return BattleSessionCaptureResult(
            success = formulaResult.success,
            playerId = action.playerId,
            ballItemId = action.ballItemId,
            sourceUnitId = action.sourceUnitId,
            targetId = action.targetId,
            shakes = formulaResult.shakes,
            reason = formulaResult.reason,
            finalRate = formulaResult.finalRate,
        )
    }
}
