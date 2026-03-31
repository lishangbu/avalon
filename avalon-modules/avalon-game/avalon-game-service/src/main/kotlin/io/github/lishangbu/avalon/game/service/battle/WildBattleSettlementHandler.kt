package io.github.lishangbu.avalon.game.service.battle

import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery
import org.springframework.stereotype.Component

@Component
internal class WildBattleSettlementHandler : BattleSettlementHandler {
    override fun supports(battleKind: BattleType): Boolean = battleKind == BattleType.WILD

    override fun settle(
        sessionId: String,
        session: BattleSessionQuery,
        support: BattleSettlementSupport,
    ): BattleSettlementResult {
        val captureResult = support.settleCaptureLedger(sessionId, session)
        val updatedSession = support.markSessionSettled(sessionId)
        return BattleSettlementResult(
            sessionId = sessionId,
            endedReason = updatedSession.snapshot.battle.endedReason,
            settled = updatedSession.snapshot.battle.settled,
            session = updatedSession,
            captureResult = captureResult,
        )
    }
}
