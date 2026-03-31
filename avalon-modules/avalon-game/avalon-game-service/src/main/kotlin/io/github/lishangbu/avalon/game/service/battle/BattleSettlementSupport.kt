package io.github.lishangbu.avalon.game.service.battle

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery
import io.github.lishangbu.avalon.game.service.capture.CaptureBattleResult

/**
 * BattleTypePolicy 结算辅助契约。
 */
internal interface BattleSettlementSupport {
    fun settleCaptureLedger(
        sessionId: String,
        session: BattleSessionQuery,
    ): CaptureBattleResult?

    fun markSessionSettled(sessionId: String): BattleSessionQuery
}
