package io.github.lishangbu.avalon.game.battle.engine.session.specification

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession

/**
 * active 单位是否允许在本回合提交行动的规格。
 */
interface BattleSessionUnitChoiceSpecification {
    /**
     * 校验某个单位当前是否允许提交行动。
     *
     * @param session 当前 battle session。
     * @param unitId 待提交行动的单位标识。
     * @return 本次规格校验结果。
     */
    fun validate(
        session: BattleSession,
        unitId: String,
    ): BattleSessionValidationResult
}
