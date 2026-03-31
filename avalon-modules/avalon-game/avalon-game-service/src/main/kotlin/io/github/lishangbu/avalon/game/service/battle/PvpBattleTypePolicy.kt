package io.github.lishangbu.avalon.game.service.battle

import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
import org.springframework.stereotype.Component

@Component
internal class PvpBattleTypePolicy(
    private val simpleBattleSettlementHandler: SimpleBattleSettlementHandler,
) : BattleTypePolicy {
    override fun supports(battleKind: BattleType): Boolean = battleKind == BattleType.PVP

    override fun settlementHandler(): BattleSettlementHandler = simpleBattleSettlementHandler
}
