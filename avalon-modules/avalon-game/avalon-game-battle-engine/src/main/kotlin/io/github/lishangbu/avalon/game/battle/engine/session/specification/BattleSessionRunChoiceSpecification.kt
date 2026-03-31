package io.github.lishangbu.avalon.game.battle.engine.session.specification

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession

/**
 * side 是否允许提交逃跑选择的规格。
 */
interface BattleSessionRunChoiceSpecification {
    /**
     * 校验某个 side 当前是否允许提交逃跑选择。
     *
     * @param session 当前 battle session。
     * @param sideId 待逃跑的 side 标识。
     * @return 本次规格校验结果。
     */
    fun validate(
        session: BattleSession,
        sideId: String,
    ): BattleSessionValidationResult
}
