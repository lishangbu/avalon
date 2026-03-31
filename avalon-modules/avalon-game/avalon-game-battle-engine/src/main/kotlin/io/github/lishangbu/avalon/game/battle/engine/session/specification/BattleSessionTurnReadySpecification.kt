package io.github.lishangbu.avalon.game.battle.engine.session.specification

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession

/**
 * 回合是否允许进入结算阶段的规格。
 */
interface BattleSessionTurnReadySpecification {
    /**
     * 校验当前回合是否已经满足最小结算条件。
     *
     * @param session 当前 battle session。
     * @return 本次规格校验结果。
     */
    fun validate(session: BattleSession): BattleSessionValidationResult
}
