package io.github.lishangbu.avalon.game.battle.engine.service

import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionChoice
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery

/**
 * battle 内自动补动作提供器。
 */
interface BattleAiChoiceProvider {
    fun supports(battleKind: BattleType): Boolean

    fun provide(session: BattleSessionQuery): List<BattleSessionChoice>
}
