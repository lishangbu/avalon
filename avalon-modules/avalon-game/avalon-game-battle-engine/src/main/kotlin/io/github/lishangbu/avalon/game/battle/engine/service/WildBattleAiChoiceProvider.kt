package io.github.lishangbu.avalon.game.battle.engine.service

import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionChoice
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery
import io.github.lishangbu.avalon.game.battle.engine.session.WaitChoice
import org.springframework.stereotype.Component

@Component
class WildBattleAiChoiceProvider : BattleAiChoiceProvider {
    override fun supports(battleKind: BattleType): Boolean = battleKind == BattleType.WILD

    override fun provide(session: BattleSessionQuery): List<BattleSessionChoice> {
        val capturableSideId = session.snapshot.battle.capturableSideId ?: return emptyList()
        return session.choiceStatuses
            .filter { status -> status.sideId == capturableSideId }
            .flatMap { status -> status.missingUnitIds }
            .map { unitId ->
                val speed =
                    session.snapshot.units[unitId]
                        ?.stats
                        ?.get("speed") ?: 0
                WaitChoice(unitId = unitId, speed = speed)
            }
    }
}
