package io.github.lishangbu.avalon.game.service.battle

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery
import io.github.lishangbu.avalon.game.battle.engine.session.ItemChoice
import io.github.lishangbu.avalon.game.battle.engine.session.MoveChoice
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetQuery

/** 基于真实数据与当前会话快照生成行动输入。 */
interface BattleChoiceFactory {
    fun queryTargets(
        session: BattleSessionQuery,
        effectId: String,
        actorUnitId: String,
    ): BattleSessionTargetQuery

    fun createMoveChoice(
        session: BattleSessionQuery,
        request: SmartMoveChoiceRequest,
    ): MoveChoice

    fun createItemChoice(
        session: BattleSessionQuery,
        request: SmartItemChoiceRequest,
    ): ItemChoice
}
