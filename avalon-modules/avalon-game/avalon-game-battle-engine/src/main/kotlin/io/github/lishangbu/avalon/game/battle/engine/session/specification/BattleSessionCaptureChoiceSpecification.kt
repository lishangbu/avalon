package io.github.lishangbu.avalon.game.battle.engine.session.specification

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession

/**
 * 捕捉动作是否合法的规格。
 */
interface BattleSessionCaptureChoiceSpecification {
    /**
     * 校验当前 battle 下是否允许执行捕捉动作。
     *
     * @param session 当前 battle session。
     * @param playerId 发起捕捉的玩家标识。
     * @param sourceUnitId 扔球单位标识。
     * @param targetUnitId 目标野生单位标识。
     * @return 本次规格校验结果。
     */
    fun validate(
        session: BattleSession,
        playerId: String,
        sourceUnitId: String,
        targetUnitId: String,
    ): BattleSessionValidationResult
}
