package io.github.lishangbu.avalon.game.battle.engine.session.specification

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession

/**
 * effect 目标是否合法的规格。
 */
interface BattleSessionTargetChoiceSpecification {
    /**
     * 校验当前 effect 对给定目标的指向是否合法。
     *
     * @param session 当前 battle session。
     * @param effectId 当前 effect 标识。
     * @param actorUnitId 行动发起者单位标识。
     * @param targetUnitId 目标单位标识。
     * @return 本次规格校验结果。
     */
    fun validate(
        session: BattleSession,
        effectId: String,
        actorUnitId: String,
        targetUnitId: String,
    ): BattleSessionValidationResult
}
