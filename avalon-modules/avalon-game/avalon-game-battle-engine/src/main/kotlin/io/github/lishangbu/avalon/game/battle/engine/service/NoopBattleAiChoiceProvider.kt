package io.github.lishangbu.avalon.game.battle.engine.service

import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionChoice
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery
import org.springframework.stereotype.Component

@Component
class NoopBattleAiChoiceProvider : BattleAiChoiceProvider {
    override fun supports(battleKind: BattleType): Boolean = battleKind == BattleType.TRAINER || battleKind == BattleType.PVP

    override fun provide(session: BattleSessionQuery): List<BattleSessionChoice> = emptyList()
}
