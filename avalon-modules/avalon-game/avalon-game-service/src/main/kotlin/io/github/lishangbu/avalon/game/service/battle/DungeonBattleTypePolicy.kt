package io.github.lishangbu.avalon.game.service.battle

import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
import org.springframework.stereotype.Component

@Component
internal class DungeonBattleTypePolicy(
    private val simpleBattleSettlementHandler: SimpleBattleSettlementHandler,
) : BattleTypePolicy {
    override fun supports(battleKind: BattleType): Boolean = battleKind == BattleType.DUNGEON

    override fun settlementHandler(): BattleSettlementHandler = simpleBattleSettlementHandler
}
