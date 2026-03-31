package io.github.lishangbu.avalon.game.service.battle

import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
import org.springframework.stereotype.Component

@Component
internal class WildBattleTypePolicy(
    private val wildBattleSettlementHandler: WildBattleSettlementHandler,
) : BattleTypePolicy {
    override fun supports(battleKind: BattleType): Boolean = battleKind == BattleType.WILD

    override fun settlementHandler(): BattleSettlementHandler = wildBattleSettlementHandler
}
