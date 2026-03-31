package io.github.lishangbu.avalon.game.service.battle

import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery

/**
 * battle 结算处理器。
 *
 * 设计意图：
 * - 把不同 battle type 的结算逻辑抽成独立组件。
 * - 让 BattleTypePolicy 只负责选择 handler，而不直接承载细节实现。
 */
internal interface BattleSettlementHandler {
    fun supports(battleKind: BattleType): Boolean

    fun settle(
        sessionId: String,
        session: BattleSessionQuery,
        support: BattleSettlementSupport,
    ): BattleSettlementResult
}
